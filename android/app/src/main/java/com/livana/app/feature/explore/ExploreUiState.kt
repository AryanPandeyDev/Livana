package com.livana.app.feature.explore

import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Region

enum class ExploreSortOption(
    val label: String,
    val queryValue: String,
) {
    Newest("Newest", "deployedAt,desc"),
    MostRaised("Most raised", "totalDonated,desc"),
}

sealed interface ExploreUiState {
    val searchQuery: String
    val selectedRegion: Region?
    val sortOption: ExploreSortOption

    data class Loading(
        override val searchQuery: String = "",
        override val selectedRegion: Region? = null,
        override val sortOption: ExploreSortOption = ExploreSortOption.Newest,
    ) : ExploreUiState

    data class Content(
        override val searchQuery: String,
        override val selectedRegion: Region?,
        override val sortOption: ExploreSortOption,
        val pools: List<PoolSummary>,
        val isLoadingMore: Boolean,
        val endReached: Boolean,
    ) : ExploreUiState

    data class Empty(
        override val searchQuery: String,
        override val selectedRegion: Region?,
        override val sortOption: ExploreSortOption,
    ) : ExploreUiState

    data class Error(
        override val searchQuery: String,
        override val selectedRegion: Region?,
        override val sortOption: ExploreSortOption,
        val message: String? = null,
    ) : ExploreUiState

    data class Offline(
        override val searchQuery: String,
        override val selectedRegion: Region?,
        override val sortOption: ExploreSortOption,
    ) : ExploreUiState
}
