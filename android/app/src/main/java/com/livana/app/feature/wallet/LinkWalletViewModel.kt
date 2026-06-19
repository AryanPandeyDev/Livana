package com.livana.app.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.chain.wallet.WalletConnector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LinkWalletViewModel @Inject constructor(
    private val linkWalletUseCase: LinkWalletUseCase,
    private val walletConnector: WalletConnector,
) : ViewModel() {

    private val _state = MutableStateFlow<LinkWalletUiState>(LinkWalletUiState.Connecting)
    val state: StateFlow<LinkWalletUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** Supply the callback that opens AppKit's connect modal (owned by the screen's NavController). */
    fun setModalLauncher(launcher: (() -> Unit)?) {
        walletConnector.setModalLauncher(launcher)
    }

    fun start() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            _state.value = LinkWalletUiState.Connecting
            val result = linkWalletUseCase { phase ->
                _state.value = when (phase) {
                    LinkWalletPhase.Connecting -> LinkWalletUiState.Connecting
                    is LinkWalletPhase.AwaitingSignature -> LinkWalletUiState.AwaitingSignature(phase.message)
                    is LinkWalletPhase.Verifying -> LinkWalletUiState.Verifying(phase.message)
                }
            }
            _state.value = result.toUiState()
        }
    }

    fun retry() {
        job?.cancel()
        start()
    }

    private fun LinkWalletResult.toUiState(): LinkWalletUiState = when (this) {
        is LinkWalletResult.Success -> LinkWalletUiState.Done
        LinkWalletResult.UserRejected ->
            LinkWalletUiState.Error("You declined the signature in your wallet.", canRetry = true)
        LinkWalletResult.NoSession ->
            LinkWalletUiState.Error("No wallet connected. Please try again.", canRetry = true)
        LinkWalletResult.WalletAlreadyLinked ->
            LinkWalletUiState.Error("This wallet is already linked to another account.", canRetry = false)
        LinkWalletResult.SignatureInvalid ->
            LinkWalletUiState.Error("The signature didn't match this wallet. Please try again.", canRetry = true)
        LinkWalletResult.ChallengeMismatch ->
            LinkWalletUiState.Error("The verification message didn't match. Please try again.", canRetry = true)
        LinkWalletResult.Offline ->
            LinkWalletUiState.Error("You're offline. Check your connection and try again.", canRetry = true)
        is LinkWalletResult.Error ->
            LinkWalletUiState.Error(message ?: "Something went wrong. Please try again.", canRetry = true)
    }
}
