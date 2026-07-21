package com.livemap.incidents.data.remote

import android.content.Context
import com.livemap.incidents.core.IoDispatcher
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the bundled `incidents.json` from app assets and parses it into domain models.
 *
 * This stands in for a real network call: it runs off the main thread and adds a small
 * artificial delay so the UI's loading state is observable. Swapping this for a Retrofit
 * service later would not touch the repository or anything above it.
 */
@Singleton
class IncidentAssetDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun load(): List<Incident> = withContext(ioDispatcher) {
        delay(SIMULATED_LATENCY_MS) // pretend this is a network round-trip

        // The payload is bundled, so reading it would succeed with no connection at all.
        // Failing explicitly when offline keeps the simulation honest: the repository, the
        // error states and the offline banner all exercise the same path they would against
        // a real endpoint.
        if (!networkMonitor.isCurrentlyOnline()) {
            throw IOException("No network connection")
        }

        val raw = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        json.decodeFromString<List<IncidentDto>>(raw).map(IncidentDto::toDomain)
    }

    private companion object {
        const val ASSET_FILE_NAME = "incidents.json"
        const val SIMULATED_LATENCY_MS = 600L
    }
}
