package com.evyr.rads.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

/**
 * Wraps Health Connect access. This is Kindly's replacement for talking to
 * Samsung Health directly — Samsung Health writes into Health Connect, and
 * we read from Health Connect only. No Samsung SDK dependency anywhere.
 */
class HealthConnectManager(private val context: Context) {

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    suspend fun hasAllPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    fun requestPermissionsContract() =
        PermissionController.createRequestPermissionResultContract()

    suspend fun readLatestWeight(): WeightRecord? {
        val now = Instant.now()
        val thirtyDaysAgo = now.minusSeconds(60L * 60 * 24 * 30)
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(thirtyDaysAgo, now)
            )
        )
        return response.records.maxByOrNull { it.time }
    }

    suspend fun readStepsToday(): Long {
        val now = Instant.now()
        val startOfDay = now.minusSeconds(now.epochSecond % 86400)
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        )
        return response.records.sumOf { it.count }
    }

    suspend fun readExerciseSessionsToday(): List<ExerciseSessionRecord> {
        val now = Instant.now()
        val startOfDay = now.minusSeconds(now.epochSecond % 86400)
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        )
        return response.records
    }
}
