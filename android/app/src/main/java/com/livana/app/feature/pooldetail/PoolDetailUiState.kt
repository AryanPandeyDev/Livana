package com.livana.app.feature.pooldetail

import com.livana.app.core.model.PoolDetail

sealed interface PoolDetailUiState {
    data object Loading : PoolDetailUiState

    data class Content(
        val pool: PoolDetail,
    ) : PoolDetailUiState

    data class Error(
        val message: String? = null,
    ) : PoolDetailUiState

    data object Offline : PoolDetailUiState

    data class NotFound(
        val message: String = "This cause is no longer available",
    ) : PoolDetailUiState
}
