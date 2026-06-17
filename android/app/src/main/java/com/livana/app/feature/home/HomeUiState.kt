package com.livana.app.feature.home

import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Usdc

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val retryable: Boolean = true, val message: String? = null) : HomeUiState
    data object Offline : HomeUiState

    data class Content(
        val totalDonated: Usdc,
        val totalReleased: Usdc,
        val activePools: Long,
        val verifiedNgos: Long,
        val totalPools: Long,
        val featuredPools: List<PoolSummary>,
    ) : HomeUiState
}
