package com.livemap.incidents.data.model

import java.time.Instant

/**
 * Domain model for a single incident. This is the type the whole app works with —
 * fully typed, with [category], [severity] and [reportedAt] as real types rather than
 * raw strings, so illegal states are unrepresentable past the parsing boundary.
 */
data class Incident(
    val id: String,
    val title: String,
    val category: IncidentCategory,
    val severity: Severity,
    val lat: Double,
    val lng: Double,
    val city: String,
    val reportedAt: Instant,
)

/**
 * Incident severity. [rank] gives a total order (low < medium < high < critical) used for
 * sorting and threshold filters; [apiValue] is the lowercase string used in the JSON feed.
 */
enum class Severity(val apiValue: String, val rank: Int) {
    LOW("low", 0),
    MEDIUM("medium", 1),
    HIGH("high", 2),
    CRITICAL("critical", 3);

    companion object {
        fun fromApi(value: String): Severity =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown severity: $value")
    }
}

/**
 * Incident category. [apiValue] matches the exact casing used in the JSON feed
 * (e.g. "PowerOutage"); [label] is a human-friendly form for the UI.
 */
enum class IncidentCategory(val apiValue: String, val label: String) {
    ACCIDENT("Accident", "Accident"),
    FIRE("Fire", "Fire"),
    MEDICAL("Medical", "Medical"),
    SECURITY("Security", "Security"),
    INFRASTRUCTURE("Infrastructure", "Infrastructure"),
    FLOOD("Flood", "Flood"),
    POWER_OUTAGE("PowerOutage", "Power outage");

    companion object {
        fun fromApi(value: String): IncidentCategory =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown category: $value")
    }
}
