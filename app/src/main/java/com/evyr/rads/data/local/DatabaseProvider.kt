package com.evyr.rads.data.local

import android.content.Context
import androidx.room.Room

/**
 * Single source for the Room database instance.
 * No destructive migration fallback — every schema bump needs a real Migration.
 */
object DatabaseProvider {
    @Volatile
    private var instance: RadsDatabase? = null

    fun get(context: Context): RadsDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RadsDatabase::class.java,
                "rads.db"
            ).build().also { instance = it }
        }
    }
}
