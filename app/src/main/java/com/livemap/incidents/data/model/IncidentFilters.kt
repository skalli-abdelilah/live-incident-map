package com.livemap.incidents.data.model

import java.time.Instant

/**
 * The active filter selection.
 *
 * An empty [categories] or [severities] set means "no restriction" rather than "match
 * nothing" — that keeps the default state trivially representable and avoids having to
 * seed the model with every enum value.
 */
data class IncidentFilters(
    val categories: Set<IncidentCategory> = emptySet(),
    val severities: Set<Severity> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
) {
    val isActive: Boolean
        get() = categories.isNotEmpty() || severities.isNotEmpty() || from != null || to != null

    /** Number of distinct constraints applied — drives the badge on the filter button. */
    val activeCount: Int
        get() = categories.size + severities.size + if (from != null || to != null) 1 else 0

    fun matches(incident: Incident): Boolean {
        if (categories.isNotEmpty() && incident.category !in categories) return false
        if (severities.isNotEmpty() && incident.severity !in severities) return false
        if (from != null && incident.reportedAt.isBefore(from)) return false
        if (to != null && incident.reportedAt.isAfter(to)) return false
        return true
    }

    companion object {
        val NONE = IncidentFilters()
    }
}

/** Applies the selection to a list. Kept as an extension so it is trivial to unit-test. */
fun List<Incident>.applyFilters(filters: IncidentFilters): List<Incident> =
    if (!filters.isActive) this else filter(filters::matches)
