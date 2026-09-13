package com.evyr.rads.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Real migrations only — never fallbackToDestructiveMigration().
 * v1 -> v2: goal weight and rate-of-change on user_profile.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE user_profile ADD COLUMN goalWeightKg REAL NOT NULL DEFAULT 0.0"
        )
        db.execSQL(
            "ALTER TABLE user_profile ADD COLUMN rateLbsPerWeek REAL NOT NULL DEFAULT 1.0"
        )
    }
}
