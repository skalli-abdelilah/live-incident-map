package com.livemap.incidents.data.remote

import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.Severity
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Wire model mirroring the exact shape of `incidents.json`. Kept separate from the domain
 * [Incident] so the network/JSON format can change without rippling through the app.
 */
@Serializable
data class IncidentDto(
    val id: String,
    val title: String,
    val category: String,
    val severity: String,
    val lat: Double,
    val lng: Double,
    val city: String,
    val reportedAt: String,
) {
    /** Maps the raw wire model into the typed domain model. */
    fun toDomain(): Incident = Incident(
        id = id,
        title = title,
        category = IncidentCategory.fromApi(category),
        severity = Severity.fromApi(severity),
        lat = lat,
        lng = lng,
        city = city,
        reportedAt = Instant.parse(reportedAt),
    )
}
