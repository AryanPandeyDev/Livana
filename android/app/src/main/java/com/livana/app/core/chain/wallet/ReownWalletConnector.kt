package com.livana.app.core.chain.wallet

import com.livana.app.BuildConfig
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.client.models.Session
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class ReownWalletConnector @Inject constructor() : WalletConnector {

    private val configured: Boolean = BuildConfig.WALLETCONNECT_PROJECT_ID.isNotBlank()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow<WalletConnectionState>(WalletConnectionState.Disconnected)
    override val connectionState: StateFlow<WalletConnectionState> = _connectionState.asStateFlow()

    // Replay the latest response so personalSign() doesn't miss one that arrives between sending
    // the request and starting to await it. Only one sign is ever in flight at a time.
    private val requestResponses = MutableSharedFlow<Modal.Model.SessionRequestResponse>(
        replay = 1,
        extraBufferCapacity = 8,
    )

    @Volatile
    private var modalLauncher: (() -> Unit)? = null

    private val delegate = object : AppKit.ModalDelegate {
        override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {
            resolveConnected()?.let { _connectionState.value = it }
        }

        override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) = Unit
        override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) = Unit
        override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) = Unit
        override fun onSessionExtend(session: Modal.Model.Session) = Unit

        override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) {
            _connectionState.value = WalletConnectionState.Disconnected
        }

        override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) {
            requestResponses.tryEmit(response)
        }

        override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) = Unit
        override fun onRequestExpired(request: Modal.Model.ExpiredRequest) = Unit
        override fun onConnectionStateChange(state: Modal.Model.ConnectionState) = Unit
        override fun onError(error: Modal.Model.Error) = Unit
    }

    init {
        if (configured) {
            AppKit.setDelegate(delegate)
            // Seed from an already-restored session (e.g. app relaunch).
            scope.launch { resolveConnected()?.let { _connectionState.value = it } }
        }
    }

    override fun setModalLauncher(launcher: (() -> Unit)?) {
        modalLauncher = launcher
    }

    override suspend fun connect(): WalletConnectionState {
        if (!configured) return WalletConnectionState.Disconnected
        requireConnected()?.let { return it }

        withContext(Dispatchers.Main) { modalLauncher?.invoke() }

        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            connectionState.first { it is WalletConnectionState.Connected }
        } ?: WalletConnectionState.Disconnected
    }

    override suspend fun disconnect() {
        if (!configured) return
        suspendCancellableCoroutine { continuation ->
            runCatching {
                AppKit.disconnect(
                    onSuccess = { if (continuation.isActive) continuation.resume(Unit) {} },
                    onError = { if (continuation.isActive) continuation.resume(Unit) {} },
                )
            }.onFailure { if (continuation.isActive) continuation.resume(Unit) {} }
        }
        _connectionState.value = WalletConnectionState.Disconnected
    }

    override suspend fun personalSign(message: String): PersonalSignResult {
        if (!configured) return PersonalSignResult.NoSession
        val connected = requireConnected() ?: return PersonalSignResult.NoSession

        return when (
            val result = sendRequest(
                method = PERSONAL_SIGN,
                params = personalSignParams(message, connected.address),
                chainId = connected.chainId,
            )
        ) {
            is RpcResult.Success -> PersonalSignResult.Success(result.value)
            RpcResult.Rejected -> PersonalSignResult.Rejected
            is RpcResult.Failed -> PersonalSignResult.Failed(result.message)
        }
    }

    override suspend fun sendTransaction(
        toAddress: String,
        data: String,
        value: String,
    ): TxResult {
        if (!configured) return TxResult.NoSession
        val connected = requireConnected() ?: return TxResult.NoSession

        return when (
            val result = sendRequest(
                method = ETH_SEND_TRANSACTION,
                params = sendTransactionParams(
                    from = connected.address,
                    to = toAddress,
                    data = data,
                    value = value,
                ),
                chainId = connected.chainId,
            )
        ) {
            is RpcResult.Success -> TxResult.Sent(result.value)
            RpcResult.Rejected -> TxResult.Rejected
            is RpcResult.Failed -> TxResult.Failed(result.message)
        }
    }

    /**
     * Sends a JSON-RPC request through AppKit and awaits its response, correlating by the JSON-RPC
     * id from the sent request. Shared by [personalSign] and [sendTransaction].
     */
    private suspend fun sendRequest(method: String, params: String, chainId: String): RpcResult {
        val request = Request(method = method, params = params, chainId = chainId)

        val sentRequestId = CompletableDeferred<Long?>()
        val sendError = CompletableDeferred<String?>()
        withContext(Dispatchers.Main) {
            AppKit.request(
                request = request,
                onSuccess = { sent ->
                    sendError.complete(null)
                    sentRequestId.complete((sent as? SentRequestResult.WalletConnect)?.requestId)
                },
                onError = { throwable ->
                    sendError.complete(throwable.message)
                    sentRequestId.complete(null)
                },
            )
        }

        val requestId = sentRequestId.await()
            ?: return RpcResult.Failed(
                "Could not send the request to your wallet" +
                    (sendError.await()?.let { ": $it" } ?: ""),
            )

        val response = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            requestResponses.first { it.result.id == requestId }
        } ?: return RpcResult.Failed("Timed out waiting for your wallet")

        return when (val result = response.result) {
            is Modal.Model.JsonRpcResponse.JsonRpcResult ->
                (result.result as? String)?.let { RpcResult.Success(it) }
                    ?: RpcResult.Failed("Wallet returned an empty response")

            is Modal.Model.JsonRpcResponse.JsonRpcError ->
                if (result.isUserRejection()) RpcResult.Rejected else RpcResult.Failed(result.message)
        }
    }

    override fun handleDeepLink(url: String) {
        if (!configured) return
        runCatching { AppKit.handleDeepLink(url) { } }
    }

    private fun seedFromExistingAccount() {
        runCatching {
            AppKit.getAccount()?.let { account ->
                _connectionState.value = WalletConnectionState.Connected(
                    address = account.address,
                    chainId = account.chain.id,
                )
            }
        }
    }

    private fun Modal.Model.JsonRpcResponse.JsonRpcError.isUserRejection(): Boolean =
        code == USER_REJECTED_CODE_WC ||
            code == USER_REJECTED_CODE_EIP1193 ||
            message.contains("reject", ignoreCase = true) ||
            message.contains("denied", ignoreCase = true)

    /** personal_sign params: `[hexMessage, address]` with the message hex-encoded (EIP-191). */
    private fun personalSignParams(message: String, address: String): String {
        val hex = "0x" + message.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
            "%02x".format(byte)
        }
        return JsonArray(listOf(JsonPrimitive(hex), JsonPrimitive(address))).toString()
    }

    /** eth_sendTransaction params: a single tx object `[{ from, to, data, value }]`. */
    private fun sendTransactionParams(
        from: String,
        to: String,
        data: String,
        value: String,
    ): String {
        val tx = buildJsonObject {
            put("from", from)
            put("to", to)
            put("data", data)
            put("value", value)
        }
        return JsonArray(listOf(tx)).toString()
    }

    /**
     * Returns the current connected account with a *valid* address, re-resolving from the active
     * WalletConnect session when the cached state is missing/blank (e.g. after an app relaunch where
     * `getAccount()` hasn't repopulated). Updates [_connectionState] when it re-resolves.
     */
    private fun requireConnected(): WalletConnectionState.Connected? {
        (connectionState.value as? WalletConnectionState.Connected)
            ?.takeIf { it.address.isEvmAddress() }
            ?.let { return it }

        val resolved = resolveConnected()
        if (resolved != null) _connectionState.value = resolved
        return resolved
    }

    /**
     * Resolves the connected account for the target chain, preferring the session's approved
     * accounts (the authoritative source) and falling back to [AppKit.getAccount].
     */
    private fun resolveConnected(): WalletConnectionState.Connected? {
        val target = "eip155:${BuildConfig.CHAIN_ID}"

        val sessionAccount = runCatching {
            (AppKit.getSession() as? Session.WalletConnectSession)
                ?.namespaces?.values
                ?.flatMap { it.accounts }
                ?.firstOrNull { it.startsWith("$target:") }
        }.getOrNull()

        sessionAccount?.substringAfterLast(':')?.takeIf { it.isEvmAddress() }?.let { address ->
            return WalletConnectionState.Connected(address = address, chainId = target)
        }

        val account = runCatching { AppKit.getAccount() }.getOrNull()
        if (account != null && account.address.isEvmAddress()) {
            return WalletConnectionState.Connected(address = account.address, chainId = account.chain.id)
        }
        return null
    }

    private sealed interface RpcResult {
        data class Success(val value: String) : RpcResult
        data object Rejected : RpcResult
        data class Failed(val message: String?) : RpcResult
    }

    private companion object {
        const val PERSONAL_SIGN = "personal_sign"
        const val ETH_SEND_TRANSACTION = "eth_sendTransaction"
        const val USER_REJECTED_CODE_WC = 5000
        const val USER_REJECTED_CODE_EIP1193 = 4001
        const val CONNECT_TIMEOUT_MS = 180_000L
        const val REQUEST_TIMEOUT_MS = 180_000L
    }
}

private val EVM_ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")

private fun String.isEvmAddress(): Boolean = EVM_ADDRESS_REGEX.matches(this)
