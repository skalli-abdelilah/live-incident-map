package com.livemap.incidents.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.model.applyFilters
import com.livemap.incidents.data.network.NetworkMonitor
import com.livemap.incidents.data.repository.FilterRepository
import com.livemap.incidents.data.repository.IncidentRepository
import com.livemap.incidents.data.repository.LiveUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Error(val message: String) : ListUiState
    data class Content(
        /** The page of incidents currently rendered, newest first. */
        val incidents: List<Incident>,
        /** Total available, so the UI can show progress and know when to stop paging. */
        val totalCount: Int,
    ) : ListUiState {
        val canLoadMore: Boolean get() = incidents.size < totalCount
    }
}

private data class RefreshState(val isLoading: Boolean, val isRefreshing: Boolean, val error: String?)

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    private val repository: IncidentRepository,
    private val liveUpdateManager: LiveUpdateManager,
    networkMonitor: NetworkMonitor,
    filterRepository: FilterRepository,
) : ViewModel() {

    /** Drives the "N new incidents" pill above the list. */
    val unseenCount: StateFlow<Int> = liveUpdateManager.unseenCount

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val refreshState = MutableStateFlow(
        RefreshState(isLoading = true, isRefreshing = false, error = null),
    )

    /** How many items are currently revealed. Grows as the user scrolls toward the end. */
    private val pageLimit = MutableStateFlow(PAGE_SIZE)

    /** Drives the pull-to-refresh indicator, kept separate from the main content state. */
    val isRefreshing: StateFlow<Boolean> = refreshState
        .map { it.isRefreshing }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /**
     * Combines the cached incidents with the current page limit. Slicing here (rather than
     * loading everything into the list) keeps composition cheap: the LazyColumn only ever
     * receives the items it needs, and the window grows on demand.
     */
    val uiState: StateFlow<ListUiState> = combine(
        repository.incidents,
        filterRepository.filters,
        refreshState,
        pageLimit,
    ) { incidents, filters, refresh, limit ->
        // Filter first, then page: the window walks the *filtered* set, so totals and
        // "load more" stay correct instead of paging through hidden items.
        val filtered = incidents.applyFilters(filters)
        when {
            refresh.isLoading && incidents.isEmpty() -> ListUiState.Loading
            refresh.error != null && incidents.isEmpty() -> ListUiState.Error(refresh.error)
            else -> ListUiState.Content(
                incidents = filtered.take(limit),
                totalCount = filtered.size,
            )
        }
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListUiState.Loading,
        )

    init {
        refresh()
        liveUpdateManager.start()
    }

    fun acknowledgeNewIncidents() = liveUpdateManager.acknowledge()

    /** Called when the user scrolls near the end of the rendered window. */
    fun loadMore() {
        pageLimit.update { it + PAGE_SIZE }
    }

    fun refresh(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            refreshState.update {
                it.copy(isLoading = !isPullToRefresh, isRefreshing = isPullToRefresh, error = null)
            }
            val result = repository.refresh()
            refreshState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
