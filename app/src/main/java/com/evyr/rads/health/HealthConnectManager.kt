package com.evyr.rads.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads from Health Connect only. Samsung Health and the Galaxy Watch write
 * into Health Connect; R.A.D.S. never talks to Samsung's SDK.
 */
class HealthConnectManager(private val context: Context) {

    data class DailyHealth(
        val steps: Long,
        val weightKg: Double?,
        val exerciseMinutes: Long
    )

    enum class Availability { READY, NOT_INSTALLED, UPDATE_REQUIRED }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    fun availability(): Availability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.READY
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                Availability.UPDATE_REQUIRED
            else -> Availability.NOT_INSTALLED
        }

    fun isAvailable(): Boolean = availability() == Availability.READY

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    /** Opens Health Connect's own settings page for this app. */
    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Play Store page for installing or updating Health Connect. */
    fun installIntent(): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(
                "market://details?id=com.google.android.apps.healthdata"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable()) return false
        return client.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    suspend fun readToday(): DailyHealth {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val now = Instant.now()

        val steps = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        ).records.sumOf { it.count }

        val weight = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startOfDay.minus(Duration.ofDays(30)), now
                )
            )
        ).records.maxByOrNull { it.time }?.weight?.inKilograms

        val exerciseMinutes = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        ).records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }

        return DailyHealth(steps, weight, exerciseMinutes)
    }
}
