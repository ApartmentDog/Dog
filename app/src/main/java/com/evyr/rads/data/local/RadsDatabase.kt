package com.evyr.rads.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfile::class, FoodLogEntry::class, HealthSnapshot::class],
    version = 1,
    exportSchema = false
)
abstract class RadsDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun healthSnapshotDao(): HealthSnapshotDao
}

// IMPORTANT: no destructive fallback migrations.
// Every schema bump from here forward needs a real Migration object
// added to the builder in DatabaseProvider, not fallbackToDestructiveMigration().
