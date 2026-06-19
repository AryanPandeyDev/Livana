package com.livana.app.feature.pooldetail.state

import com.livana.app.core.model.Proof

sealed interface PoolProofsUiState {
    data object Loading : PoolProofsUiState

    data class Content(
        val proofs: List<Proof>,
        val isLoadingMore: Boolean,
        val endReached: Boolean,
    ) : PoolProofsUiState

    data object Empty : PoolProofsUiState

    data class Error(
        val message: String? = null,
    ) : PoolProofsUiState

    data object Offline : PoolProofsUiState
}