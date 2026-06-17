package com.livana.app.feature.pooldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.ProofRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PoolProofsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val proofRepository: ProofRepository,
) : ViewModel() {

    /** The pool's on-chain address, read from the navigation argument. */
    val address: String = checkNotNull(savedStateHandle.get<String>("address"))

    private val _state = MutableStateFlow<PoolProofsUiState>(PoolProofsUiState.Loading)
    val state: StateFlow<PoolProofsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var nextPageJob: Job? = null
    private var nextPage = FirstPage

    init {
        loadFirstPage()
    }

    fun retry() {
        loadFirstPage()
    }

    fun loadNextPage() {
        val currentState = _state.value as? PoolProofsUiState.Content ?: return
        if (currentState.isLoadingMore || currentState.endReached) return

        nextPageJob = viewModelScope.launch {
            _state.value = currentState.copy(isLoadingMore = true)
            when (val result = proofRepository.getPoolProofs(
                address = address,
                page = nextPage,
                size = PageSize,
            )) {
                is LivanaResult.Success -> {
                    val page = result.value
                    nextPage = page.number + 1
                    _state.value = currentState.copy(
                        proofs = currentState.proofs + page.content,
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

    private fun loadFirstPage() {
        loadJob?.cancel()
        nextPageJob?.cancel()
        loadJob = viewModelScope.launch {
            nextPage = FirstPage
            _state.value = PoolProofsUiState.Loading

            when (val result = proofRepository.getPoolProofs(
                address = address,
                page = FirstPage,
                size = PageSize,
            )) {
                is LivanaResult.Success -> {
                    val page = result.value
                    nextPage = page.number + 1
                    _state.value = if (page.content.isEmpty()) {
                        PoolProofsUiState.Empty
                    } else {
                        PoolProofsUiState.Content(
                            proofs = page.content,
                            isLoadingMore = false,
                            endReached = page.last,
                        )
                    }
                }

                is LivanaResult.Failure -> {
                    _state.value = if (result.error is DomainError.Network) {
                        PoolProofsUiState.Offline
                    } else {
                        PoolProofsUiState.Error(message = result.error.message)
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
