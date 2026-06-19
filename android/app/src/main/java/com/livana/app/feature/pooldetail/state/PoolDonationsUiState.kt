package com.livana.app.feature.pooldetail.state

import com.livana.app.core.model.PoolDonation

sealed interface PoolDonationsUiState {
    data object Loading : PoolDonationsUiState

    data class Content(
        val donations: List<PoolDonation>,
        val isLoadingMore: Boolean,
        val endReached: Boolean,
    ) : PoolDonationsUiState

    data object Empty : PoolDonationsUiState

    data class Error(
        val message: String? = null,
    ) : PoolDonationsUiState

    data object Offline : PoolDonationsUiState
}