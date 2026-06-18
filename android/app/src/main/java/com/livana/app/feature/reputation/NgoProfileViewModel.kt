package com.livana.app.feature.reputation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.ReputationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NgoProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reputationRepository: ReputationRepository,
) : ViewModel() {

    /** The NGO's on-chain address, read from the navigation argument. */
    val address: String = checkNotNull(savedStateHandle.get<String>("address"))

    private val _state = MutableStateFlow<NgoProfileUiState>(NgoProfileUiState.Loading)
    val state: StateFlow<NgoProfileUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var nextPageJob: Job? = null
    private var nextPage = FirstPage

    init {
        loadInitial()
    }

    fun retry() {
        loadInitial()
    }

    fun loadNextPage() {
        val currentState = _state.value as? NgoProfileUiState.Content ?: return
        if (currentState.isLoadingMore || currentState.endReached) return

        nextPageJob = viewModelScope.launch {
            _state.value = currentState.copy(isLoadingMore = true)
            when (val result = reputationRepository.getReputationHistory(
                address = address,
                page = nextPage,
                size = PageSize,
            )) {
                is LivanaResult.Success -> {
                    val page = result.value
                    nextPage = page.number + 1
                    _state.value = currentState.copy(
                        history = currentState.history + page.content,
                        isLoadingMore = false,
                        endReached = page.last,
                    )
                }

                is LivanaResult.Failure -> {
                    _state.value = currentState.copy(isLoadingMore = false)
                }
            }
        }
    }

    /**
     * Load reputation summary and page 0 of SBT history concurrently.
     * If reputation fails → Error/Offline.
     * If reputation succeeds but history fails → Content with empty history.
     */
    private fun loadInitial() {
        loadJob?.cancel()
        nextPageJob?.cancel()
        loadJob = viewModelScope.launch {
            nextPage = FirstPage
            _state.value = NgoProfileUiState.Loading

            val reputationDeferred = async {
                reputationRepository.getReputation(address)
            }
            val historyDeferred = async {
                reputationRepository.getReputationHistory(
                    address = address,
                    page = FirstPage,
                    size = PageSize,
                )
            }

            val reputationResult = reputationDeferred.await()
            val historyResult = historyDeferred.await()

            when (reputationResult) {
                is LivanaResult.Success -> {
                    val reputation = reputationResult.value
                    val historyContent = when (historyResult) {
                        is LivanaResult.Success -> {
                            nextPage = historyResult.value.number + 1
                            historyResult.value
                        }
                        is LivanaResult.Failure -> null
                    }
                    _state.value = NgoProfileUiState.Content(
                        reputation = reputation,
                        history = historyContent?.content ?: emptyList(),
                        isLoadingMore = false,
                        endReached = historyContent?.last ?: true,
                    )
                }

                is LivanaResult.Failure -> {
                    _state.value = if (reputationResult.error is DomainError.Network) {
                        NgoProfileUiState.Offline
                    } else {
                        NgoProfileUiState.Error(message = reputationResult.error.message)
                    }
                }
            }
        }
    }

    private companion object {
        const val FirstPage = 0
        const val PageSize = 20
    }
}
