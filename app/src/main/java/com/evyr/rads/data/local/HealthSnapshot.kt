package com.evyr.rads.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per day, holding whatever Health Connect last reported.
 * dayKey is the epoch-day number so there's exactly one row per calendar day.
 */
@Entity(tableName = "health_snapshot")
data class HealthSnapshot(
    @PrimaryKey val dayKey: Long,
    val steps: Long = 0,
    val weightKg: Double? = null,
    val exerciseMinutes: Long = 0,
    val lastSyncedAt: Long = 0
)
