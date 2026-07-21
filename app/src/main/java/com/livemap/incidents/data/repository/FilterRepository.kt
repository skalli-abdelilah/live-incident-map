package com.livemap.incidents.data.repository

import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.IncidentFilters
import com.livemap.incidents.data.model.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the active filters.
 *
 * Scoped to the application rather than to a ViewModel so the map and the list observe the
 * *same* stream. If each screen owned its own copy they would drift apart whenever the user
 * changed a filter on one tab, and the two views would disagree about what is being shown.
 */
@Singleton
class FilterRepository @Inject constructor() {

    private val _filters = MutableStateFlow(IncidentFilters.NONE)
    val filters: StateFlow<IncidentFilters> = _filters.asStateFlow()

    fun toggleCategory(category: IncidentCategory) {
        _filters.update { current ->
            current.copy(categories = current.categories.toggle(category))
        }
    }

    fun toggleSeverity(severity: Severity) {
        _filters.update { current ->
            current.copy(severities = current.severities.toggle(severity))
        }
    }

    fun setDateRange(from: Instant?, to: Instant?) {
        _filters.update { current -> current.copy(from = from, to = to) }
    }

    fun clear() {
        _filters.value = IncidentFilters.NONE
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> =
        if (value in this) this - value else this + value
}
