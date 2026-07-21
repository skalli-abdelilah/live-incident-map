package com.livemap.incidents.data

import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.IncidentFilters
import com.livemap.incidents.data.model.Severity
import com.livemap.incidents.data.model.applyFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IncidentFiltersTest {

    private val now: Instant = Instant.parse("2026-07-20T12:00:00Z")

    private fun incident(
        id: String,
        category: IncidentCategory = IncidentCategory.ACCIDENT,
        severity: Severity = Severity.LOW,
        reportedAt: Instant = now,
    ) = Incident(
        id = id,
        title = "Test incident",
        category = category,
        severity = severity,
        lat = 33.5,
        lng = -7.6,
        city = "Casablanca",
        reportedAt = reportedAt,
    )

    private val sample = listOf(
        incident("a", IncidentCategory.FIRE, Severity.CRITICAL, now),
        incident("b", IncidentCategory.MEDICAL, Severity.LOW, now.minusSeconds(3600)),
        incident("c", IncidentCategory.FIRE, Severity.LOW, now.minusSeconds(86_400 * 10)),
    )

    @Test
    fun `no filters returns everything`() {
        assertEquals(sample, sample.applyFilters(IncidentFilters.NONE))
        assertFalse(IncidentFilters.NONE.isActive)
    }

    @Test
    fun `empty category set means unrestricted, not match-nothing`() {
        val filters = IncidentFilters(severities = setOf(Severity.LOW))
        val result = sample.applyFilters(filters)
        assertEquals(listOf("b", "c"), result.map { it.id })
    }

    @Test
    fun `category and severity combine as AND`() {
        val filters = IncidentFilters(
            categories = setOf(IncidentCategory.FIRE),
            severities = setOf(Severity.LOW),
        )
        assertEquals(listOf("c"), sample.applyFilters(filters).map { it.id })
    }

    @Test
    fun `multi-select within a facet behaves as OR`() {
        val filters = IncidentFilters(
            categories = setOf(IncidentCategory.FIRE, IncidentCategory.MEDICAL),
        )
        assertEquals(listOf("a", "b", "c"), sample.applyFilters(filters).map { it.id })
    }

    @Test
    fun `date range excludes incidents reported before the window`() {
        val filters = IncidentFilters(from = now.minusSeconds(7200))
        assertEquals(listOf("a", "b"), sample.applyFilters(filters).map { it.id })
    }

    @Test
    fun `filters that match nothing return an empty list`() {
        val filters = IncidentFilters(
            categories = setOf(IncidentCategory.FLOOD),
            severities = setOf(Severity.CRITICAL),
        )
        assertTrue(sample.applyFilters(filters).isEmpty())
    }

    @Test
    fun `active count reports each constraint once, with dates counted together`() {
        val filters = IncidentFilters(
            categories = setOf(IncidentCategory.FIRE, IncidentCategory.MEDICAL),
            severities = setOf(Severity.HIGH),
            from = now.minusSeconds(3600),
        )
        assertEquals(4, filters.activeCount)
    }
}
