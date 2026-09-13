package com.evyr.rads.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.evyr.rads.health.HealthConnectManager
import com.evyr.rads.ui.TodayViewModel
import com.evyr.rads.ui.components.*
import java.io.ByteArrayOutputStream

private val MEAL_SLOTS = listOf("breakfast", "lunch", "dinner", "snack")
private val TABS = listOf("LOG", "STATS", "SYNC", "SETUP")

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

    val healthManager = remember { HealthConnectManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthManager.permissionContract()
    ) { vm.sync() }

    if (!profileLoaded) return

    if (profile?.onboarded != true) {
        OnboardingScreen(onComplete = { vm.completeOnboarding(it) })
        return
    }

    var activeTab by remember { mutableStateOf("LOG") }
    var activeMeal by remember { mutableStateOf("breakfast") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

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
    val scrollProgress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 1) 0f
            else listState.firstVisibleItemIndex.toFloat() / (total - 1).toFloat()
        }
    }

    val mealEntries = entries.filter { it.mealSlot == activeMeal }
    val selectedEntry = entries.firstOrNull { it.id == selectedId }
    val fatLimit = profile?.fatWarnGramsPerMeal ?: 15.0

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

    // Activity lamp: lit while anything is actually working.
    val busy = searching ||
        syncStatus == TodayViewModel.SyncStatus.SYNCING ||
        scanState is TodayViewModel.ScanState.Working

    TerminalChrome(
        scrollProgress = scrollProgress,
        busy = busy,
        buttons = listOf(
            TerminalButtonSpec("LOG", selected = activeTab == "LOG") {
                if (activeTab == "LOG") {
                    vm.resetSearch()
                    showSearch = true
                } else activeTab = "LOG"
            },
            TerminalButtonSpec("SCAN", selected = false) {
                activeTab = "LOG"
                showScanPicker = true
            },
            TerminalButtonSpec("SYNC", selected = activeTab == "SYNC") {
                activeTab = "SYNC"
                vm.sync()
            },
        )
    ) {
        Column(Modifier.fillMaxWidth()) {
            TerminalTabBar(TABS, activeTab) { activeTab = it }
            Hairline()

            when (activeTab) {
                "LOG" -> TabLogView(
                    mealSlots = MEAL_SLOTS,
                    activeMeal = activeMeal,
                    entries = mealEntries,
                    selectedEntry = selectedEntry,
                    fatWarnGrams = fatLimit,
                    listState = listState,
                    onSelectMeal = { activeMeal = it },
                    onSelectEntry = { vm.selectEntry(it.id) },
                    onDeleteEntry = { vm.deleteEntry(it) }
                )
                "STATS" -> TabStatsView(
                    entries = entries,
                    profile = profile,
                    health = health,
                    mealSlots = MEAL_SLOTS
                )
                "SYNC" -> TabSyncView(
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
                "SETUP" -> TabSetupView(
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
                onPick = { vm.assess(it, activeMeal) },
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
