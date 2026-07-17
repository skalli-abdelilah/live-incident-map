package com.livemap.incidents.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    /** Observes all cached incidents, newest first. Emits again on every write. */
    @Query("SELECT * FROM incidents ORDER BY reportedAtEpochMs DESC")
    fun observeAll(): Flow<List<IncidentEntity>>

    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(incidents: List<IncidentEntity>)

    @Query("DELETE FROM incidents")
    suspend fun clear()

    /**
     * Atomically replaces the whole cache with a fresh snapshot. Wrapping clear + insert in
     * a single transaction means observers never see an empty, half-written table.
     */
    @Transaction
    suspend fun replaceAll(incidents: List<IncidentEntity>) {
        clear()
        upsertAll(incidents)
    }
}
