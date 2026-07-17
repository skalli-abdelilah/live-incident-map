package com.livemap.incidents.data

import com.livemap.incidents.data.local.toDomain
import com.livemap.incidents.data.local.toEntity
import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.Severity
import com.livemap.incidents.data.remote.IncidentDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IncidentMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sample = """
        [
          {
            "id": "INC-00180",
            "title": "Injury requiring evacuation",
            "category": "PowerOutage",
            "severity": "medium",
            "lat": 33.64044,
            "lng": -7.58983,
            "city": "Casablanca",
            "reportedAt": "2026-07-08T11:56:35Z"
          }
        ]
    """.trimIndent()

    @Test
    fun `parses json into typed domain model`() {
        val incident = json.decodeFromString<List<IncidentDto>>(sample).single().toDomain()

        assertEquals("INC-00180", incident.id)
        assertEquals(IncidentCategory.POWER_OUTAGE, incident.category)
        assertEquals(Severity.MEDIUM, incident.severity)
        assertEquals(Instant.parse("2026-07-08T11:56:35Z"), incident.reportedAt)
    }

    @Test
    fun `domain survives a round trip through the room entity`() {
        val original = json.decodeFromString<List<IncidentDto>>(sample).single().toDomain()
        val roundTripped = original.toEntity().toDomain()
        assertEquals(original, roundTripped)
    }

    @Test
    fun `unknown severity is rejected at the parsing boundary`() {
        assertThrows(IllegalArgumentException::class.java) {
            Severity.fromApi("catastrophic")
        }
    }

    @Test
    fun `severity ordering runs low to critical`() {
        val ranks = listOf(Severity.LOW, Severity.MEDIUM, Severity.HIGH, Severity.CRITICAL)
            .map { it.rank }
        assertTrue(ranks == ranks.sorted())
    }
}
