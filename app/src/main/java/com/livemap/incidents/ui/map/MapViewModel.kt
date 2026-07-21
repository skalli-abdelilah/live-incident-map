package com.livemap.incidents.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.model.applyFilters
import com.livemap.incidents.data.repository.FilterRepository
import com.livemap.incidents.data.repository.IncidentRepository
import com.livemap.incidents.data.repository.LiveUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Error(val message: String) : MapUiState
    data class Content(
        val incidents: List<Incident>,
        val totalCount: Int,
        val isFiltered: Boolean,
    ) : MapUiState
}

private data class RefreshState(val isLoading: Boolean, val error: String?)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: IncidentRepository,
    private val liveUpdateManager: LiveUpdateManager,
    filterRepository: FilterRepository,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState(isLoading = true, error = null))

    /** Drives the "N new incidents" pill. */
    val unseenCount: StateFlow<Int> = liveUpdateManager.unseenCount

    /**
     * Filtering happens here, by combining the cached incidents with the shared filter
     * stream. Any change to either input re-emits, so the map re-renders reactively without
     * the UI ever asking for a refresh.
     */
    val uiState: StateFlow<MapUiState> = combine(
        repository.incidents,
        filterRepository.filters,
        refreshState,
    ) { incidents, filters, refresh ->
        when {
            refresh.isLoading && incidents.isEmpty() -> MapUiState.Loading
            refresh.error != null && incidents.isEmpty() -> MapUiState.Error(refresh.error)
            else -> MapUiState.Content(
                incidents = incidents.applyFilters(filters),
                totalCount = incidents.size,
                isFiltered = filters.isActive,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState.Loading,
    )

    init {
        refresh()
        liveUpdateManager.start()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState.update { it.copy(isLoading = true, error = null) }
            val result = repository.refresh()
            refreshState.update {
                it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun acknowledgeNewIncidents() = liveUpdateManager.acknowledge()
}
