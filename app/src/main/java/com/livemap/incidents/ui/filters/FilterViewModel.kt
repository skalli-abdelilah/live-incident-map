package com.livemap.incidents.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.IncidentFilters
import com.livemap.incidents.data.model.Severity
import com.livemap.incidents.data.repository.FilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Preset windows, which is how an operator actually thinks about "recent". */
enum class DateRangePreset(val label: String, val duration: java.time.Duration?) {
    ANY_TIME("Any time", null),
    LAST_HOUR("Last hour", java.time.Duration.ofHours(1)),
    LAST_24H("Last 24 hours", java.time.Duration.ofDays(1)),
    LAST_7_DAYS("Last 7 days", java.time.Duration.ofDays(7)),
    LAST_30_DAYS("Last 30 days", java.time.Duration.ofDays(30)),
}

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
) : ViewModel() {

    val filters: StateFlow<IncidentFilters> = filterRepository.filters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IncidentFilters.NONE,
        )

    fun toggleCategory(category: IncidentCategory) = filterRepository.toggleCategory(category)

    fun toggleSeverity(severity: Severity) = filterRepository.toggleSeverity(severity)

    fun selectDateRange(preset: DateRangePreset) {
        val duration = preset.duration
        if (duration == null) {
            filterRepository.setDateRange(from = null, to = null)
        } else {
            // Truncated to seconds so the boundary is stable across recompositions.
            val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
            filterRepository.setDateRange(from = now.minus(duration), to = null)
        }
    }

    /** Which preset the current `from` value corresponds to, for highlighting the chip. */
    fun activePreset(filters: IncidentFilters): DateRangePreset {
        val from = filters.from ?: return DateRangePreset.ANY_TIME
        val elapsed = java.time.Duration.between(from, Instant.now())
        return DateRangePreset.entries
            .filter { it.duration != null }
            .minByOrNull { preset ->
                kotlin.math.abs(preset.duration!!.seconds - elapsed.seconds)
            } ?: DateRangePreset.ANY_TIME
    }

    fun clear() = filterRepository.clear()
}
