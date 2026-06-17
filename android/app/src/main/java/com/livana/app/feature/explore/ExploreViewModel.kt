package com.livana.app.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.PoolRepository
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Region
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val poolRepository: PoolRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val _state = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var nextPageJob: Job? = null
    private var nextPage = FirstPage
    private var currentQuery = ""
    private var selectedRegion: Region? = null
    private var selectedSort = ExploreSortOption.Newest

    init {
        loadFirstPage(query = "", region = selectedRegion, sortOption = selectedSort)
        viewModelScope.launch {
            searchQuery
                .drop(1)
                .debounce(SearchDebounceMillis)
                .distinctUntilChanged()
                .collect { query ->
                    loadFirstPage(query = query, region = selectedRegion, sortOption = selectedSort)
                }
        }
    }

    fun onSearchChange(query: String) {
        searchQuery.value = query
        _state.value = _state.value.withSearchQuery(query)
    }

    fun retry() {
        loadFirstPage(
            query = _state.value.searchQuery,
            region = _state.value.selectedRegion,
            sortOption = _state.value.sortOption,
        )
    }

    fun applyFilters(
        region: Region?,
        sortOption: ExploreSortOption,
    ) {
        selectedRegion = region
        selectedSort = sortOption
        loadFirstPage(
            query = _state.value.searchQuery,
            region = region,
            sortOption = sortOption,
        )
    }

    fun loadNextPage() {
        val currentState = _state.value as? ExploreUiState.Content ?: return
        if (currentState.isLoadingMore || currentState.endReached) return

        nextPageJob = viewModelScope.launch {
            _state.value = currentState.copy(isLoadingMore = true)
            when (val result = fetchPage(page = nextPage, query = currentQuery)) {
                is LivanaResult.Success -> {
                    val page = result.value
                    nextPage = page.number + 1
                    _state.value = currentState.copy(
                        pools = currentState.pools + page.content,
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

    private fun loadFirstPage(
        query: String,
        region: Region?,
        sortOption: ExploreSortOption,
    ) {
        loadJob?.cancel()
        nextPageJob?.cancel()
        loadJob = viewModelScope.launch {
            currentQuery = query.trim()
            selectedRegion = region
            selectedSort = sortOption
            nextPage = FirstPage
            _state.value = ExploreUiState.Loading(
                searchQuery = query,
                selectedRegion = region,
                sortOption = sortOption,
            )

            when (val result = fetchPage(page = FirstPage, query = currentQuery)) {
                is LivanaResult.Success -> {
                    val page = result.value
                    nextPage = page.number + 1
                    _state.value = if (page.content.isEmpty()) {
                        ExploreUiState.Empty(
                            searchQuery = query,
                            selectedRegion = region,
                            sortOption = sortOption,
                        )
                    } else {
                        ExploreUiState.Content(
                            searchQuery = query,
                            selectedRegion = region,
                            sortOption = sortOption,
                            pools = page.content,
                            isLoadingMore = false,
                            endReached = page.last,
                        )
                    }
                }

                is LivanaResult.Failure -> {
                    _state.value = if (result.error is DomainError.Network) {
                        ExploreUiState.Offline(
                            searchQuery = query,
                            selectedRegion = region,
                            sortOption = sortOption,
                        )
                    } else {
                        ExploreUiState.Error(
                            searchQuery = query,
                            selectedRegion = region,
                            sortOption = sortOption,
                            message = result.error.message,
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchPage(
        page: Int,
        query: String,
    ): LivanaResult<PagedResult<PoolSummary>> = poolRepository.getPools(
        region = selectedRegion,
        search = query.takeIf(String::isNotBlank),
        page = page,
        size = PageSize,
        sort = selectedSort.queryValue,
    )

    private fun ExploreUiState.withSearchQuery(query: String): ExploreUiState = when (this) {
        is ExploreUiState.Loading -> copy(searchQuery = query)
        is ExploreUiState.Content -> copy(searchQuery = query)
        is ExploreUiState.Empty -> copy(searchQuery = query)
        is ExploreUiState.Error -> copy(searchQuery = query)
        is ExploreUiState.Offline -> copy(searchQuery = query)
    }

    private companion object {
        const val FirstPage = 0
        const val PageSize = 20
        const val SearchDebounceMillis = 300L
    }
}
