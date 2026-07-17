package com.livemap.incidents.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [IncidentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class IncidentDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
}
