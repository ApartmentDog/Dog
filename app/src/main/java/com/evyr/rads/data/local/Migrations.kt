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

/** v2 -> v3: extra nutrients and triggers per entry, health conditions on profile. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_log_entry ADD COLUMN saturatedFatGrams REAL")
        db.execSQL("ALTER TABLE food_log_entry ADD COLUMN sugarGrams REAL")
        db.execSQL("ALTER TABLE food_log_entry ADD COLUMN fiberGrams REAL")
        db.execSQL("ALTER TABLE food_log_entry ADD COLUMN sodiumMg REAL")
        db.execSQL("ALTER TABLE food_log_entry ADD COLUMN triggers TEXT")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN conditions TEXT NOT NULL DEFAULT ''")
    }
}
