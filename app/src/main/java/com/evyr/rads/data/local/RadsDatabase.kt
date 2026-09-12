package com.evyr.rads.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfile::class, FoodLogEntry::class],
    version = 1,
    exportSchema = true
)
abstract class RadsDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
}

// IMPORTANT (lesson from REBUILD): no destructive fallback migrations.
// Every schema bump from here forward needs a real Migration object added
// to the builder in DatabaseModule, not fallbackToDestructiveMigration().
