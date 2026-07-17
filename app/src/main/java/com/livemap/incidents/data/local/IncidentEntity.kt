package com.livemap.incidents.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.Severity
import java.time.Instant

/**
 * Room row for a cached incident. Enums and the timestamp are stored as primitives
 * (their `apiValue` / epoch-millis) so the table stays simple and query-friendly —
 * e.g. sorting by [reportedAtEpochMs] is a plain column sort.
 */
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val severity: String,
    val lat: Double,
    val lng: Double,
    val city: String,
    val reportedAtEpochMs: Long,
)

fun IncidentEntity.toDomain(): Incident = Incident(
    id = id,
    title = title,
    category = IncidentCategory.fromApi(category),
    severity = Severity.fromApi(severity),
    lat = lat,
    lng = lng,
    city = city,
    reportedAt = Instant.ofEpochMilli(reportedAtEpochMs),
)

fun Incident.toEntity(): IncidentEntity = IncidentEntity(
    id = id,
    title = title,
    category = category.apiValue,
    severity = severity.apiValue,
    lat = lat,
    lng = lng,
    city = city,
    reportedAtEpochMs = reportedAt.toEpochMilli(),
)
