package com.livana.app.feature.leaderboard

import com.livana.app.core.model.DonorLeaderboardEntry
import com.livana.app.core.model.NgoLeaderboardEntry

/**
 * State for the Boards screen. Both leaderboards (donors + NGOs) are loaded together,
 * so a single state drives the whole screen; the segment toggle is separate UI state
 * that only chooses which already-loaded list to render.
 */
sealed interface BoardsUiState {
    data object Loading : BoardsUiState

    data class Content(
        val donors: List<DonorLeaderboardEntry>,
        val ngos: List<NgoLeaderboardEntry>,
    ) : BoardsUiState

    data object Empty : BoardsUiState

    data class Error(
        val message: String? = null,
    ) : BoardsUiState

    data object Offline : BoardsUiState
}

/** Which leaderboard the user is viewing. Screen-level UI state — does not refetch. */
enum class BoardsSegment {
    Donors,
    Ngos,
}
