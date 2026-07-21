package com.livemap.incidents.data.remote

import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.Severity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Simulated real-time incident feed.
 *
 * Exposed as a cold [Flow] because that is exactly the shape a real WebSocket client would
 * have: swapping this for an OkHttp `WebSocketListener` wrapped in `callbackFlow` would not
 * change a single line above this class. The flow is cold, so collection starts when a
 * screen subscribes and stops when it goes away — no socket kept alive in the background.
 */
@Singleton
class IncidentFeed @Inject constructor() {

    fun observe(): Flow<Incident> = flow {
        var sequence = 0
        while (true) {
            delay(Random.nextLong(MIN_INTERVAL_MS, MAX_INTERVAL_MS))
            emit(generateIncident(++sequence))
        }
    }

    private fun generateIncident(sequence: Int): Incident {
        val location = MOROCCAN_CITIES.random()
        val category = IncidentCategory.entries.random()
        return Incident(
            // Prefixed so simulated incidents are distinguishable from the seeded dataset.
            id = "LIVE-%05d".format(sequence),
            title = TITLES_BY_CATEGORY.getValue(category).random(),
            category = category,
            severity = randomSeverity(),
            lat = location.lat + Random.nextDouble(-JITTER_DEGREES, JITTER_DEGREES),
            lng = location.lng + Random.nextDouble(-JITTER_DEGREES, JITTER_DEGREES),
            city = location.name,
            reportedAt = Instant.now(),
        )
    }

    /**
     * Weighted so critical incidents stay rare — a feed where every third event is critical
     * would make the severity colouring useless for triage.
     */
    private fun randomSeverity(): Severity = when (Random.nextInt(100)) {
        in 0..44 -> Severity.LOW
        in 45..74 -> Severity.MEDIUM
        in 75..92 -> Severity.HIGH
        else -> Severity.CRITICAL
    }

    private data class City(val name: String, val lat: Double, val lng: Double)

    private companion object {
        const val MIN_INTERVAL_MS = 2_500L
        const val MAX_INTERVAL_MS = 5_000L
        const val JITTER_DEGREES = 0.05

        val MOROCCAN_CITIES = listOf(
            City("Casablanca", 33.5731, -7.5898),
            City("Rabat", 34.0209, -6.8416),
            City("Marrakech", 31.6295, -7.9811),
            City("Fès", 34.0181, -5.0078),
            City("Tanger", 35.7595, -5.8340),
            City("Agadir", 30.4278, -9.5981),
            City("Meknès", 33.8935, -5.5473),
            City("Oujda", 34.6867, -1.9114),
            City("Kénitra", 34.2610, -6.5802),
            City("Tétouan", 35.5889, -5.3626),
            City("Nador", 35.1681, -2.9287),
            City("Laâyoune", 27.1536, -13.2033),
        )

        val TITLES_BY_CATEGORY = mapOf(
            IncidentCategory.ACCIDENT to listOf(
                "Multi-vehicle collision", "Motorcycle accident", "Pedestrian incident",
            ),
            IncidentCategory.FIRE to listOf(
                "Vehicle fire", "Electrical fire", "Building fire reported",
            ),
            IncidentCategory.MEDICAL to listOf(
                "Injury requiring evacuation", "Ambulance dispatch", "Medical emergency",
            ),
            IncidentCategory.SECURITY to listOf(
                "Suspicious package", "Crowd disturbance", "Security alert",
            ),
            IncidentCategory.INFRASTRUCTURE to listOf(
                "Gas leak reported", "Water main break", "Bridge inspection",
            ),
            IncidentCategory.FLOOD to listOf(
                "Wadi overflow", "Street flooding", "Drainage failure",
            ),
            IncidentCategory.POWER_OUTAGE to listOf(
                "Transformer failure", "Grid outage", "Line down",
            ),
        )
    }
}
