package com.livemap.incidents.data.repository

import com.livemap.incidents.core.ApplicationScope
import com.livemap.incidents.data.local.IncidentDao
import com.livemap.incidents.data.local.toEntity
import com.livemap.incidents.data.remote.IncidentFeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the live incident feed.
 *
 * Arrivals are written into the same Room cache the UI already observes, so a new incident
 * reaches every screen through the existing flow — nothing pushes into the UI, and the cache
 * stays the single source of truth. Because the data path is a plain database write, the map
 * and list re-render from state rather than being told to move, which is what keeps the
 * user's viewport and scroll position untouched.
 *
 * Scoped to the application so a single feed serves every screen; collecting per-ViewModel
 * would restart the stream on each navigation and double-count arrivals.
 */
@Singleton
class LiveUpdateManager @Inject constructor(
    private val feed: IncidentFeed,
    private val dao: IncidentDao,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _unseenCount = MutableStateFlow(0)

    /** Incidents that have arrived since the user last acknowledged them. */
    val unseenCount: StateFlow<Int> = _unseenCount.asStateFlow()

    private var started = false

    /** Idempotent: the first screen to appear starts the feed, later ones are no-ops. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            feed.observe().collect { incident ->
                dao.upsert(incident.toEntity())
                _unseenCount.update { it + 1 }
            }
        }
    }

    fun acknowledge() {
        _unseenCount.value = 0
    }
}
