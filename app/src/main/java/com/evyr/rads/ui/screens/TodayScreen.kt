package com.evyr.rads.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.evyr.rads.data.local.DEFAULT_CALORIE_TARGET
import com.evyr.rads.data.local.DEFAULT_FAT_WARN_GRAMS
import com.evyr.rads.health.HealthConnectManager
import com.evyr.rads.ui.TodayViewModel
import com.evyr.rads.ui.components.*
import java.io.ByteArrayOutputStream

private val MEAL_SLOTS = listOf("breakfast", "lunch", "dinner", "snack")

private enum class AppTab(val label: String) {
    LOG("Log"), STATS("Stats"), SYNC("Sync"), SETUP("Setup")
}

@Composable
fun TodayScreen() {
    val vm: TodayViewModel = viewModel()
    val context = LocalContext.current

    val profile by vm.profile.collectAsState()
    val profileLoaded by vm.profileLoaded.collectAsState()
    val entries by vm.entries.collectAsState()
    val health by vm.healthToday.collectAsState()
    val selectedId by vm.selectedEntryId.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()
    val scanState by vm.scanState.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val searching by vm.searching.collectAsState()
    val searchMessage by vm.searchMessage.collectAsState()
    val pendingPortion by vm.pendingPortion.collectAsState()
    val portionOptions by vm.portionOptions.collectAsState()
    val viewDate by vm.viewDate.collectAsState()
    val isToday by vm.isViewingToday.collectAsState()

    // Crossing midnight while the app sits in the background must roll the day.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val healthManager = remember { HealthConnectManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthManager.permissionContract()
    ) { vm.sync() }

    if (!profileLoaded) return

    if (profile?.onboarded != true) {
        OnboardingScreen(onComplete = { vm.completeOnboarding(it) })
        return
    }

    var activeTab by remember { mutableStateOf(AppTab.LOG) }
    // Default to the meal that matches the clock, not whatever comes first.
    var activeMeal by remember { mutableStateOf(vm.mealSlotForNow()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showBarcodeEntry by remember { mutableStateOf(false) }

    // Keep the ViewModel aware of where scans should land.
    vm.activeMealSlot = activeMeal

    // Gallery pick -> bytes -> vision
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            readBytes(context, uri)?.let { vm.onPhotoCaptured(it) }
        }
    }

    // Camera preview capture -> bitmap -> jpeg bytes -> vision
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val out = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
            vm.onPhotoCaptured(out.toByteArray())
        }
    }

    val listState = rememberLazyListState()
    val statsScroll = rememberScrollState()
    val syncScroll = rememberScrollState()
    val setupScroll = rememberScrollState()

    val mealEntries = entries.filter { it.mealSlot == activeMeal }
    val selectedEntry = entries.firstOrNull { it.id == selectedId }
    val fatLimit = profile?.fatWarnGramsPerMeal ?: DEFAULT_FAT_WARN_GRAMS

    fun launchBarcode() {
        val options = GmsBarcodeScannerOptions.Builder()
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let { vm.onBarcodeScanned(it) }
            }
            .addOnFailureListener {
                vm.clearScan()
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { activeTab = AppTab.LOG; showScanPicker = true }) {
                Text("+", fontSize = 22.sp)
            }
        },
        bottomBar = {
            NavigationBar {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = {
                            if (tab == AppTab.SYNC) vm.sync()
                            activeTab = tab
                        },
                        icon = {},
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            when (activeTab) {
                AppTab.LOG -> TabLogView(
                    viewDate = viewDate,
                    isToday = isToday,
                    onPreviousDay = { vm.previousDay() },
                    onNextDay = { vm.nextDay() },
                    onJumpToToday = { vm.jumpToToday() },
                    mealSlots = MEAL_SLOTS,
                    activeMeal = activeMeal,
                    dayEntries = entries,
                    calorieTarget = profile?.calorieTarget() ?: DEFAULT_CALORIE_TARGET,
                    entries = mealEntries,
                    selectedEntry = selectedEntry,
                    fatWarnGrams = fatLimit,
                    conditions = profile?.conditionSet() ?: emptySet(),
                    listState = listState,
                    onSelectMeal = { activeMeal = it },
                    onSelectEntry = { vm.selectEntry(it.id) },
                    onDeleteEntry = { vm.deleteEntry(it) }
                )
                AppTab.STATS -> TabStatsView(
                    scrollState = statsScroll,
                    isToday = isToday,
                    entries = entries,
                    profile = profile,
                    health = health,
                    mealSlots = MEAL_SLOTS
                )
                AppTab.SYNC -> TabSyncView(
                    scrollState = syncScroll,
                    status = syncStatus,
                    availability = healthManager.availability(),
                    health = health,
                    imperial = profile?.useImperial ?: true,
                    onSync = { vm.sync() },
                    onRequestPermission = {
                        permissionLauncher.launch(healthManager.permissions)
                    },
                    onOpenSettings = {
                        runCatching { context.startActivity(healthManager.settingsIntent()) }
                    },
                    onInstall = {
                        runCatching { context.startActivity(healthManager.installIntent()) }
                    }
                )
                AppTab.SETUP -> TabSetupView(
                    scrollState = setupScroll,
                    profile = profile,
                    onUpdate = { vm.saveProfile(it) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddEntryDialog(
            mealSlot = activeMeal,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cal, fat, protein, carbs ->
                vm.addEntry(name, activeMeal, cal, fat, protein, carbs)
                showAddDialog = false
            }
        )
    }

    if (showScanPicker) {
        ScanSourceDialog(
            mealSlot = activeMeal,
            aiEnabled = vm.aiEnabled(),
            onSearch = { showScanPicker = false; vm.resetSearch(); showSearch = true },
            onBarcode = { showScanPicker = false; launchBarcode() },
            onTypeBarcode = { showScanPicker = false; showBarcodeEntry = true },
            onCameraPhoto = { showScanPicker = false; cameraLauncher.launch(null) },
            onGalleryPhoto = {
                showScanPicker = false
                galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onManual = { showScanPicker = false; showAddDialog = true },
            onDismiss = { showScanPicker = false }
        )
    }

    if (showBarcodeEntry) {
        BarcodeEntryDialog(
            onSubmit = { code ->
                showBarcodeEntry = false
                vm.onBarcodeScanned(code)
            },
            onDismiss = { showBarcodeEntry = false }
        )
    }

    if (showSearch) {
        FoodSearchDialog(
            mealSlot = activeMeal,
            query = searchQuery,
            results = searchResults,
            searching = searching,
            message = searchMessage,
            onQueryChange = { vm.setSearchQuery(it) },
            onSearch = { vm.runSearch() },
            onPick = { vm.choosePortion(it) },
            onManual = { showSearch = false; showAddDialog = true },
            onDismiss = { showSearch = false; vm.resetSearch() }
        )
    }

    pendingPortion?.let { food ->
        QuantityDialog(
            food = food,
            portionOptions = portionOptions,
            onDismiss = { vm.choosePortion(null) },
            onConfirm = { scaled ->
                vm.choosePortion(null)
                showSearch = false
                vm.assess(scaled, activeMeal)
            }
        )
    }

    when (val s = scanState) {
        is TodayViewModel.ScanState.Working ->
            ScanStatusDialog(s.message) { vm.clearScan() }
        is TodayViewModel.ScanState.Message ->
            ScanStatusDialog(s.text) { vm.clearScan() }
        is TodayViewModel.ScanState.Choose ->
            ScanResultPicker(
                foods = s.foods,
                onPick = { vm.clearScan(); vm.choosePortion(it) },
                onDismiss = { vm.clearScan() }
            )
        is TodayViewModel.ScanState.Assess ->
            VerdictDialog(
                food = s.food,
                verdict = s.verdict,
                mealSlot = activeMeal,
                onDismiss = { vm.clearScan() },
                onConfirm = { vm.commitScanned(it, activeMeal) }
            )
        else -> Unit
    }
}

private fun readBytes(context: Context, uri: android.net.Uri): ByteArray? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

