package com.livana.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.auth.AuthState
import com.livana.app.core.auth.SessionManager
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.PoolRepository
import com.livana.app.core.data.repository.ReputationRepository
import com.livana.app.core.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val poolRepository: PoolRepository,
    private val reputationRepository: ReputationRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    /** App-wide auth state, surfaced so the top bar can show the Sign-in chip or the user avatar. */
    val authState: StateFlow<AuthState> = sessionManager.authState

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = HomeUiState.Loading

            val statsDeferred = async { statsRepository.getPlatformStats() }
            val poolsDeferred = async {
                poolRepository.getPools(
                    size = FeaturedPoolsSize,
                    sort = FeaturedPoolsSort,
                )
            }
            val topNgosDeferred = async { reputationRepository.getNgoLeaderboard(limit = TopNgosLimit) }

            val statsResult = statsDeferred.await()
            val poolsResult = poolsDeferred.await()
            val topNgosResult = topNgosDeferred.await()

            _state.value = when {
                statsResult is LivanaResult.Success && poolsResult is LivanaResult.Success -> {
                    HomeUiState.Content(
                        totalDonated = statsResult.value.totalDonated,
                        totalReleased = statsResult.value.totalReleased,
                        activePools = statsResult.value.activePoolsCount,
                        verifiedNgos = statsResult.value.verifiedNgosCount,
                        totalPools = statsResult.value.totalPoolsCount,
                        featuredPools = poolsResult.value.content,
                        // Top NGOs is a non-critical section: surface it on success, and
                        // degrade to an empty list on any failure so it simply hides.
                        topNgos = (topNgosResult as? LivanaResult.Success)?.value.orEmpty(),
                    )
                }

                statsResult is LivanaResult.Failure && statsResult.error is DomainError.Network -> HomeUiState.Offline
                poolsResult is LivanaResult.Failure && poolsResult.error is DomainError.Network -> HomeUiState.Offline
                statsResult is LivanaResult.Failure -> HomeUiState.Error(message = statsResult.error.message)
                poolsResult is LivanaResult.Failure -> HomeUiState.Error(message = poolsResult.error.message)
                else -> HomeUiState.Error()
            }
        }
    }

    private companion object {
        const val FeaturedPoolsSize = 6
        const val FeaturedPoolsSort = "deployedAt,desc"
        const val TopNgosLimit = 3
    }
}
