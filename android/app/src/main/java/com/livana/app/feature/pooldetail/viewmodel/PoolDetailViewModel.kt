package com.livana.app.feature.pooldetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.common.BackendErrorCode
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.PoolRepository
import com.livana.app.feature.pooldetail.state.PoolDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoolDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val poolRepository: PoolRepository,
) : ViewModel() {
    private val address = savedStateHandle.get<String>(AddressKey).orEmpty()
    private val _state = MutableStateFlow<PoolDetailUiState>(PoolDetailUiState.Loading)
    val state: StateFlow<PoolDetailUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (address.isBlank()) {
                _state.value = PoolDetailUiState.NotFound()
                return@launch
            }

            _state.value = PoolDetailUiState.Loading
            _state.value = when (val result = poolRepository.getPool(address)) {
                is LivanaResult.Success -> PoolDetailUiState.Content(result.value)
                is LivanaResult.Failure -> result.error.toUiState()
            }
        }
    }

    private fun DomainError.toUiState(): PoolDetailUiState = when (this) {
        is DomainError.Network -> PoolDetailUiState.Offline
        is DomainError.Backend -> when (code) {
            BackendErrorCode.PoolNotFound,
            BackendErrorCode.InvalidAddress,
            -> PoolDetailUiState.NotFound()

            else -> PoolDetailUiState.Error(message)
        }

        else -> PoolDetailUiState.Error(message)
    }

    private companion object {
        const val AddressKey = "address"
    }
}