package com.livemap.incidents.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.repository.IncidentRepository
import com.livemap.incidents.ui.navigation.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Content(val incident: Incident) : DetailUiState
}

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: IncidentRepository,
) : ViewModel() {

    /** Route arguments are read type-safely from the saved state handle. */
    private val incidentId: String = savedStateHandle.toRoute<DetailRoute>().incidentId

    /**
     * Observes this one incident rather than taking a snapshot, so the screen stays correct
     * if the cache is refreshed while the user is looking at it.
     */
    val uiState: StateFlow<DetailUiState> = repository.incidentById(incidentId)
        .map { incident ->
            if (incident == null) DetailUiState.NotFound else DetailUiState.Content(incident)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading,
        )
}
