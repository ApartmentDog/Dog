package com.evyr.rads.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads health data from Health Connect. Samsung Health (and the Galaxy Watch
 * feeding it) writes into Health Connect; we only ever read from Health Connect.
 * No Samsung SDK dependency anywhere.
 */
class HealthConnectManager(private val context: Context) {

    data class DailyHealth(
        val steps: Long,
        val weightKg: Double?,
        val exerciseMinutes: Long
    )

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable()) return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /** Reads today's steps, latest weight, and total exercise minutes. */
    suspend fun readToday(): DailyHealth {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val now = java.time.Instant.now()

        val steps = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        ).records.sumOf { it.count }

        // Weight is sampled infrequently, so look back 30 days for the latest.
        val weight = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startOfDay.minus(Duration.ofDays(30)),
                    now
                )
            )
        ).records.maxByOrNull { it.time }?.weight?.inKilograms

        val exerciseMinutes = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        ).records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }

        return DailyHealth(
            steps = steps,
            weightKg = weight,
            exerciseMinutes = exerciseMinutes
        )
    }
}
