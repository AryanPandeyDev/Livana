package com.livana.app.feature.reputation

import com.livana.app.core.model.NgoReputation
import com.livana.app.core.model.SbtMint

sealed interface NgoProfileUiState {
    data object Loading : NgoProfileUiState

    data class Content(
        val reputation: NgoReputation,
        val history: List<SbtMint>,
        val isLoadingMore: Boolean,
        val endReached: Boolean,
    ) : NgoProfileUiState

    data class Error(
        val message: String? = null,
    ) : NgoProfileUiState

    data object Offline : NgoProfileUiState
}
