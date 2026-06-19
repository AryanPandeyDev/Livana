package com.livana.app.feature.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.DonationRepository
import com.livana.app.core.data.repository.ReputationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
    private val reputationRepository: ReputationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BoardsUiState>(BoardsUiState.Loading)
    val state: StateFlow<BoardsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = BoardsUiState.Loading

            // Both leaderboards are independent top-N lists — fetch them concurrently.
            val donorsDeferred = async { donationRepository.getDonorLeaderboard(limit = Limit) }
            val ngosDeferred = async { reputationRepository.getNgoLeaderboard(limit = Limit) }
            val donorsResult = donorsDeferred.await()
            val ngosResult = ngosDeferred.await()

            _state.value = when {
                donorsResult is LivanaResult.Success && ngosResult is LivanaResult.Success -> {
                    val donors = donorsResult.value
                    val ngos = ngosResult.value
                    if (donors.isEmpty() && ngos.isEmpty()) {
                        BoardsUiState.Empty
                    } else {
                        BoardsUiState.Content(donors = donors, ngos = ngos)
                    }
                }

                else -> {
                    val error = (donorsResult as? LivanaResult.Failure)?.error
                        ?: (ngosResult as? LivanaResult.Failure)?.error
                    if (error is DomainError.Network) {
                        BoardsUiState.Offline
                    } else {
                        BoardsUiState.Error(message = error?.message)
                    }
                }
            }
        }
    }

    private companion object {
        const val Limit = 25
    }
}
