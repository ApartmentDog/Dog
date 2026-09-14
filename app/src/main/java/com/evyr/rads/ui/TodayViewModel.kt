package com.evyr.rads.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evyr.rads.data.FoodPortion
import com.evyr.rads.data.ScannedFood
import com.evyr.rads.data.SecureStore
import com.evyr.rads.data.VerdictResult
import com.evyr.rads.data.VerdictRules
import com.evyr.rads.data.local.DatabaseProvider
import com.evyr.rads.data.remote.GeminiVision
import com.evyr.rads.data.remote.OpenFoodFacts
import com.evyr.rads.data.remote.UsdaFoodSearch
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.data.local.HealthSnapshot
import com.evyr.rads.data.local.UserProfile
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
    private val profileDao = db.userProfileDao()
    private val health = HealthConnectManager(app)

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate get() = LocalDate.now(zone)
    private val dayKey: Long get() = today.toEpochDay()
    private val dayStart: Long get() = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val dayEnd: Long get() =
        today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    val entries: StateFlow<List<FoodLogEntry>> =
        foodDao.getEntriesForDay(dayStart, dayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthToday: StateFlow<HealthSnapshot?> =
        healthDao.observeDay(dayKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profile: StateFlow<UserProfile?> =
        profileDao.observe()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    private val _selectedEntryId = MutableStateFlow<Long?>(null)
    val selectedEntryId: StateFlow<Long?> = _selectedEntryId.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    enum class SyncStatus { IDLE, SYNCING, OK, UNAVAILABLE, NO_PERMISSION, ERROR }

    // ---- Scanning ----

    sealed class ScanState {
        object Idle : ScanState()
        data class Working(val message: String) : ScanState()
        data class Message(val text: String) : ScanState()
        data class Choose(val foods: List<ScannedFood>) : ScanState()
        data class Assess(val food: ScannedFood, val verdict: VerdictResult) : ScanState()
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun clearScan() { _scanState.value = ScanState.Idle }

    // ---- Food search ----

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ScannedFood>>(emptyList())
    val searchResults: StateFlow<List<ScannedFood>> = _searchResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _searchMessage = MutableStateFlow<String?>(null)
    val searchMessage: StateFlow<String?> = _searchMessage.asStateFlow()

    private val _pendingPortion = MutableStateFlow<ScannedFood?>(null)
    val pendingPortion: StateFlow<ScannedFood?> = _pendingPortion.asStateFlow()

    /** Published portion options for the pending item, when the source has them. */
    private val _portionOptions = MutableStateFlow<List<FoodPortion>>(emptyList())
    val portionOptions: StateFlow<List<FoodPortion>> = _portionOptions.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun choosePortion(food: ScannedFood?) {
        _pendingPortion.value = food
        _portionOptions.value = emptyList()
        // Only worth a lookup when we don't already have a per-serving figure.
        val id = food?.sourceId
        if (food != null && food.basisGrams != null && id != null) {
            viewModelScope.launch {
                val ctx = getApplication<Application>()
                val found = UsdaFoodSearch.portions(id, SecureStore.usdaKey(ctx))
                if (_pendingPortion.value?.sourceId == id) {
                    _portionOptions.value = found
                }
            }
        }
    }

    fun resetSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchMessage.value = null
        _searching.value = false
        _pendingPortion.value = null
        _portionOptions.value = emptyList()
    }

    /**
     * USDA FoodData Central is the primary source; Open Food Facts fills in
     * packaged items USDA misses. Results are merged, USDA first.
     */
    fun runSearch() {
        val q = _searchQuery.value.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _searching.value = true
            _searchMessage.value = null
            _searchResults.value = emptyList()

            val ctx = getApplication<Application>()
            val usda = UsdaFoodSearch.search(q, SecureStore.usdaKey(ctx))
            val off = runCatching { OpenFoodFacts.search(q) }.getOrDefault(emptyList())

            var usdaError: String? = null
            val merged = when (usda) {
                is UsdaFoodSearch.Result.Found -> usda.foods + off
                is UsdaFoodSearch.Result.Empty -> off
                is UsdaFoodSearch.Result.Failed -> {
                    usdaError = usda.message
                    off
                }
            }

            if (merged.isNotEmpty()) {
                _searchResults.value = merged
                _searching.value = false
                return@launch
            }

            // Tier 3: neither database has it. Fall back to an AI estimate,
            // which is where chain-restaurant items usually land.
            val key = SecureStore.geminiKey(ctx)
            if (key.isBlank()) {
                _searchMessage.value = usdaError
                    ?: "No matches for \"$q\".\n\nAdd a Gemini key in SETUP to estimate items the databases don't carry, or use manual entry."
                _searching.value = false
                return@launch
            }

            _searchMessage.value = "NOT IN DATABASES — ESTIMATING..."
            when (val ai = GeminiVision.estimateFromText(key, SecureStore.geminiModel(ctx), q)) {
                is GeminiVision.Result.Found -> {
                    _searchResults.value = ai.foods
                    _searchMessage.value = null
                }
                is GeminiVision.Result.ModelRetired -> {
                    SecureStore.setGeminiModel(ctx, ai.suggested)
                    when (val retry =
                        GeminiVision.estimateFromText(key, ai.suggested, q)) {
                        is GeminiVision.Result.Found -> {
                            _searchResults.value = retry.foods
                            _searchMessage.value = null
                        }
                        else -> _searchMessage.value =
                            "No matches for \"$q\". Try manual entry."
                    }
                }
                is GeminiVision.Result.Failed ->
                    _searchMessage.value = usdaError
                        ?: "No matches for \"$q\".\n\n${ai.message}"
            }
            _searching.value = false
        }
    }

    fun aiEnabled(): Boolean = SecureStore.hasGeminiKey(getApplication<Application>())

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _scanState.value = ScanState.Working("LOOKING UP $barcode...")
            when (val r = OpenFoodFacts.lookup(barcode)) {
                is OpenFoodFacts.Result.Found -> {
                    _scanState.value = ScanState.Idle
                    choosePortion(r.food)
                }
                is OpenFoodFacts.Result.NotFound ->
                    _scanState.value = ScanState.Message(
                        "Barcode ${r.barcode} isn't in the database.\n\nEnter it manually instead."
                    )
                is OpenFoodFacts.Result.Failed ->
                    _scanState.value = ScanState.Message(r.message)
            }
        }
    }

    fun onPhotoCaptured(bytes: ByteArray) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val key = SecureStore.geminiKey(ctx)
            if (key.isBlank()) {
                _scanState.value = ScanState.Message("No Gemini API key set. Add one in SETUP.")
                return@launch
            }
            _scanState.value = ScanState.Working("ANALYSING IMAGE...")

            var result = GeminiVision.analyze(key, SecureStore.geminiModel(ctx), bytes)

            // Google retired the saved model: adopt the suggested one and retry once.
            if (result is GeminiVision.Result.ModelRetired) {
                val suggested = result.suggested
                SecureStore.setGeminiModel(ctx, suggested)
                _scanState.value = ScanState.Working("MODEL UPDATED. RETRYING...")
                result = GeminiVision.analyze(key, suggested, bytes)
            }

            when (val r = result) {
                is GeminiVision.Result.Found ->
                    if (r.foods.size == 1) {
                        _scanState.value = ScanState.Idle
                        choosePortion(r.foods.first())
                    } else _scanState.value = ScanState.Choose(r.foods)
                is GeminiVision.Result.Failed ->
                    _scanState.value = ScanState.Message(r.message)
                is GeminiVision.Result.ModelRetired ->
                    _scanState.value = ScanState.Message(
                        "Model name is out of date. Set it to \"${r.suggested}\" in SETUP."
                    )
            }
        }
    }

    /** Judge a candidate against what's already in the target meal. */
    fun assess(food: ScannedFood, mealSlot: String? = null) {
        viewModelScope.launch {
            val slot = mealSlot ?: activeMealSlot
            val limit = profileDao.get()?.fatWarnGramsPerMeal ?: 15.0
            val already = entries.value
                .filter { it.mealSlot == slot }
                .sumOf { it.fatGrams }
            _scanState.value = ScanState.Assess(
                food,
                VerdictRules.evaluate(food, already, limit)
            )
        }
    }

    /** The meal slot the UI is currently showing, so scans land in the right place. */
    var activeMealSlot: String = "breakfast"

    fun commitScanned(food: ScannedFood, mealSlot: String) {
        viewModelScope.launch {
            val limit = profileDao.get()?.fatWarnGramsPerMeal ?: 15.0
            val over = food.fatGrams >= limit
            foodDao.insert(
                FoodLogEntry(
                    timestamp = System.currentTimeMillis(),
                    mealSlot = mealSlot,
                    name = food.name,
                    calories = food.calories,
                    fatGrams = food.fatGrams,
                    proteinGrams = food.proteinGrams,
                    carbGrams = food.carbGrams,
                    source = food.source,
                    flagged = over,
                    flagReason = if (over) "OVER MEAL FAT LIMIT" else null
                )
            )
            _scanState.value = ScanState.Idle
        }
    }

    init {
        // Mark profile state as resolved once we've checked the DB at least once,
        // so onboarding doesn't flash before the real profile loads.
        viewModelScope.launch {
            profileDao.get()
            _profileLoaded.value = true
        }
        // Opportunistic sync on launch; silently no-ops without permission.
        viewModelScope.launch { syncQuietly() }
    }

    fun selectEntry(id: Long?) {
        _selectedEntryId.value = if (_selectedEntryId.value == id) null else id
    }

    fun saveProfile(updated: UserProfile) {
        viewModelScope.launch { profileDao.upsert(updated.copy(id = 1)) }
    }

    fun completeOnboarding(p: UserProfile) {
        viewModelScope.launch {
            profileDao.upsert(p.copy(id = 1, onboarded = true))
        }
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
            val limit = profileDao.get()?.fatWarnGramsPerMeal ?: 15.0
            val overLimit = fatGrams >= limit
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
                    flagged = overLimit,
                    flagReason = if (overLimit) "OVER MEAL FAT LIMIT" else null
                )
            )
        }
    }

    fun deleteEntry(entry: FoodLogEntry) {
        viewModelScope.launch {
            if (_selectedEntryId.value == entry.id) _selectedEntryId.value = null
            foodDao.delete(entry)
        }
    }

    fun sync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            _syncStatus.value = runSync()
        }
    }

    private suspend fun syncQuietly() {
        val result = runSync()
        if (result == SyncStatus.OK) _syncStatus.value = SyncStatus.OK
    }

    private suspend fun runSync(): SyncStatus {
        return try {
            if (!health.isAvailable()) return SyncStatus.UNAVAILABLE
            if (!health.hasAllPermissions()) return SyncStatus.NO_PERMISSION
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
            SyncStatus.OK
        } catch (e: Exception) {
            SyncStatus.ERROR
        }
    }
}
