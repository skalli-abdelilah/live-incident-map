package com.livemap.incidents.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations (Navigation Compose 2.8+).
 *
 * Using serializable types instead of string routes means arguments are checked by the
 * compiler — a renamed field breaks the build rather than failing at runtime.
 */
@Serializable
data object MapRoute

@Serializable
data object ListRoute

@Serializable
data class DetailRoute(val incidentId: String)
