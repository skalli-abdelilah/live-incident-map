package com.livemap.incidents.ui.home

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

/** UI state for the home screen — a temporary harness that verifies the data layer. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(val incidents: List<Incident>) : HomeUiState
}

private data class RefreshState(val isLoading: Boolean, val error: String?)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IncidentRepository,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState(isLoading = true, error = null))

    /**
     * Derives the screen state from two inputs: the cached incidents (source of truth) and
     * the in-flight refresh status. Because the incidents flow is offline-first, an error is
     * only surfaced when there is nothing cached to show.
     */
    val uiState: StateFlow<HomeUiState> =
        combine(repository.incidents, refreshState) { incidents, refresh ->
            when {
                refresh.isLoading && incidents.isEmpty() -> HomeUiState.Loading
                refresh.error != null && incidents.isEmpty() -> HomeUiState.Error(refresh.error)
                else -> HomeUiState.Content(incidents)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
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
