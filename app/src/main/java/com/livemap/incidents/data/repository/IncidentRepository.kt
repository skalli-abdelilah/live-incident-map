package com.livemap.incidents.data.repository

import com.livemap.incidents.core.IoDispatcher
import com.livemap.incidents.data.local.IncidentDao
import com.livemap.incidents.data.local.toDomain
import com.livemap.incidents.data.local.toEntity
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.data.remote.IncidentAssetDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for incident data. The rest of the app depends on this interface,
 * never on Room or the asset source directly.
 */
interface IncidentRepository {
    /** Cold stream of the cached incidents, newest first. Re-emits on every cache change. */
    val incidents: Flow<List<Incident>>

    /** Observes a single incident by id; emits null once it is no longer cached. */
    fun incidentById(id: String): Flow<Incident?>

    /** Pulls a fresh snapshot from the source and replaces the cache. Returns the count loaded. */
    suspend fun refresh(): Result<Int>

    /** Whether anything is currently cached (used to decide loading vs. offline behaviour). */
    suspend fun hasCache(): Boolean
}

/**
 * Offline-first implementation: Room is the single source of truth. [incidents] always
 * reads from the database, and [refresh] writes the latest snapshot into it. This means
 * the UI keeps showing the last cached data even when a refresh fails.
 */
@Singleton
class DefaultIncidentRepository @Inject constructor(
    private val assetDataSource: IncidentAssetDataSource,
    private val dao: IncidentDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IncidentRepository {

    override val incidents: Flow<List<Incident>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun incidentById(id: String): Flow<Incident?> =
        dao.observeById(id).map { row -> row?.toDomain() }

    override suspend fun refresh(): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            val loaded = assetDataSource.load()
            dao.replaceAll(loaded.map { it.toEntity() })
            loaded.size
        }
    }

    override suspend fun hasCache(): Boolean = withContext(ioDispatcher) { dao.count() > 0 }
}
