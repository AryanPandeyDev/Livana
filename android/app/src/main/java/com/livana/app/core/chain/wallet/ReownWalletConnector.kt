package com.livana.app.core.chain.wallet

import com.livana.app.BuildConfig
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
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
            approvedSession.toConnected()?.let { _connectionState.value = it }
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
            scope.launch { seedFromExistingAccount() }
        }
    }

    override fun setModalLauncher(launcher: (() -> Unit)?) {
        modalLauncher = launcher
    }

    override suspend fun connect(): WalletConnectionState {
        if (!configured) return WalletConnectionState.Disconnected
        (connectionState.value as? WalletConnectionState.Connected)?.let { return it }

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
        val connected = connectionState.value as? WalletConnectionState.Connected
            ?: return PersonalSignResult.NoSession

        val request = Request(
            method = PERSONAL_SIGN,
            params = personalSignParams(message, connected.address),
            chainId = connected.chainId,
        )

        val sentRequestId = CompletableDeferred<Long?>()
        withContext(Dispatchers.Main) {
            AppKit.request(
                request = request,
                onSuccess = { sent ->
                    sentRequestId.complete((sent as? SentRequestResult.WalletConnect)?.requestId)
                },
                onError = { sentRequestId.complete(null) },
            )
        }

        val requestId = sentRequestId.await()
            ?: return PersonalSignResult.Failed("Could not send the signing request")

        val response = withTimeoutOrNull(SIGN_TIMEOUT_MS) {
            requestResponses.first { it.result.id == requestId }
        } ?: return PersonalSignResult.Failed("Timed out waiting for the wallet signature")

        return when (val result = response.result) {
            is Modal.Model.JsonRpcResponse.JsonRpcResult ->
                (result.result as? String)?.let { PersonalSignResult.Success(it) }
                    ?: PersonalSignResult.Failed("Wallet returned an empty signature")

            is Modal.Model.JsonRpcResponse.JsonRpcError ->
                if (result.isUserRejection()) PersonalSignResult.Rejected
                else PersonalSignResult.Failed(result.message)
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

    private fun Modal.Model.ApprovedSession.toConnected(): WalletConnectionState.Connected? = when (this) {
        is Modal.Model.ApprovedSession.WalletConnectSession ->
            accounts.firstOrNull()?.let { caip10 ->
                WalletConnectionState.Connected(
                    address = caip10.substringAfterLast(':'),
                    chainId = caip10.substringBeforeLast(':'),
                )
            }

        is Modal.Model.ApprovedSession.CoinbaseSession ->
            WalletConnectionState.Connected(address = address, chainId = chain)
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

    private companion object {
        const val PERSONAL_SIGN = "personal_sign"
        const val USER_REJECTED_CODE_WC = 5000
        const val USER_REJECTED_CODE_EIP1193 = 4001
        const val CONNECT_TIMEOUT_MS = 180_000L
        const val SIGN_TIMEOUT_MS = 180_000L
    }
}
