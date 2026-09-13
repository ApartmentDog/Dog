package com.evyr.rads.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evyr.rads.data.local.DatabaseProvider
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.data.local.HealthSnapshot
import com.evyr.rads.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val db = DatabaseProvider.get(app)
    private val foodDao = db.foodLogDao()
    private val healthDao = db.healthSnapshotDao()
    private val health = HealthConnectManager(app)

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate get() = LocalDate.now(zone)
    private val dayKey: Long get() = today.toEpochDay()
    private val dayStart: Long get() = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val dayEnd: Long get() = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    val entries: StateFlow<List<FoodLogEntry>> =
        foodDao.getEntriesForDay(dayStart, dayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthToday: StateFlow<HealthSnapshot?> =
        healthDao.observeDay(dayKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedEntryId = MutableStateFlow<Long?>(null)
    val selectedEntryId: StateFlow<Long?> = _selectedEntryId.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    enum class SyncStatus { IDLE, SYNCING, OK, UNAVAILABLE, NO_PERMISSION, ERROR }

    fun selectEntry(id: Long?) {
        _selectedEntryId.value = id
    }

    fun addEntry(
        name: String,
        mealSlot: String,
        calories: Int,
        fatGrams: Double,
        proteinGrams: Double,
        carbGrams: Double
    ) {
        viewModelScope.launch {
            foodDao.insert(
                FoodLogEntry(
                    timestamp = System.currentTimeMillis(),
                    mealSlot = mealSlot,
                    name = name,
                    calories = calories,
                    fatGrams = fatGrams,
                    proteinGrams = proteinGrams,
                    carbGrams = carbGrams,
                    source = "manual",
                    flagged = fatGrams >= FAT_WARN_GRAMS_PER_MEAL,
                    flagReason = if (fatGrams >= FAT_WARN_GRAMS_PER_MEAL) "HIGH FAT" else null
                )
            )
        }
    }

    fun deleteEntry(entry: FoodLogEntry) {
        viewModelScope.launch { foodDao.delete(entry) }
    }

    fun sync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                if (!health.isAvailable()) {
                    _syncStatus.value = SyncStatus.UNAVAILABLE
                    return@launch
                }
                if (!health.hasAllPermissions()) {
                    _syncStatus.value = SyncStatus.NO_PERMISSION
                    return@launch
                }
                val daily = health.readToday()
                healthDao.upsert(
                    HealthSnapshot(
                        dayKey = dayKey,
                        steps = daily.steps,
                        weightKg = daily.weightKg,
                        exerciseMinutes = daily.exerciseMinutes,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
                _syncStatus.value = SyncStatus.OK
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    companion object {
        /**
         * Per-meal fat threshold. This is deliberately per-meal, never a
         * daily budget — firm dietary requirement, do not aggregate.
         */
        const val FAT_WARN_GRAMS_PER_MEAL = 15.0
    }
}
