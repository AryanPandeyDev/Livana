package com.livana.app.feature.pooldetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.DonationRepository
import com.livana.app.feature.pooldetail.state.PoolDonationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoolDonationsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val donationRepository: DonationRepository,
) : ViewModel() {

    /** The pool's on-chain address, read from the navigation argument. */
    val address: String = checkNotNull(savedStateHandle.get<String>("address"))

    private val _state = MutableStateFlow<PoolDonationsUiState>(PoolDonationsUiState.Loading)
    val state: StateFlow<PoolDonationsUiState> = _state.asStateFlow()

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
        val currentState = _state.value as? PoolDonationsUiState.Content ?: return
        if (currentState.isLoadingMore || currentState.endReached) return

        nextPageJob = viewModelScope.launch {
            _state.value = currentState.copy(isLoadingMore = true)
            when (val result = donationRepository.getPoolDonations(
                address = address,
                page = nextPage,
                size = PageSize,
            )) {
                is LivanaResult.Success -> {
                    val page = result.value
                    nextPage = page.number + 1
                    _state.value = currentState.copy(
                        donations = currentState.donations + page.content,
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
            _state.value = PoolDonationsUiState.Loading

            when (val result = donationRepository.getPoolDonations(
                address = address,
                page = FirstPage,
                size = PageSize,
            )) {
                is LivanaResult.Success -> {
                    val page = result.value
                    nextPage = page.number + 1
                    _state.value = if (page.content.isEmpty()) {
                        PoolDonationsUiState.Empty
                    } else {
                        PoolDonationsUiState.Content(
                            donations = page.content,
                            isLoadingMore = false,
                            endReached = page.last,
                        )
                    }
                }

                is LivanaResult.Failure -> {
                    _state.value = if (result.error is DomainError.Network) {
                        PoolDonationsUiState.Offline
                    } else {
                        PoolDonationsUiState.Error(message = result.error.message)
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