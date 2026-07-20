package com.livemap.incidents.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.repository.IncidentRepository
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
    data class Content(val incidents: List<Incident>) : MapUiState
}

private data class RefreshState(val isLoading: Boolean, val error: String?)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: IncidentRepository,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState(isLoading = true, error = null))

    val uiState: StateFlow<MapUiState> =
        combine(repository.incidents, refreshState) { incidents, refresh ->
            when {
                refresh.isLoading && incidents.isEmpty() -> MapUiState.Loading
                refresh.error != null && incidents.isEmpty() -> MapUiState.Error(refresh.error)
                else -> MapUiState.Content(incidents)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState.Loading,
        )

    init {
        refresh()
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

}
