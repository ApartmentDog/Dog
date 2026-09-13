package com.evyr.rads.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.evyr.rads.health.HealthConnectManager
import com.evyr.rads.ui.Screen
import com.evyr.rads.ui.TodayViewModel
import com.evyr.rads.ui.components.*

private val MEAL_SLOTS = listOf("breakfast", "lunch", "dinner", "snack")
private val TABS = listOf("LOG", "STATS", "SYNC", "SETUP")

@Composable
fun TodayScreen(onNavigate: (Screen) -> Unit) {
    val vm: TodayViewModel = viewModel()
    val context = LocalContext.current

    val profile by vm.profile.collectAsState()
    val profileLoaded by vm.profileLoaded.collectAsState()
    val entries by vm.entries.collectAsState()
    val health by vm.healthToday.collectAsState()
    val selectedId by vm.selectedEntryId.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()

    val healthManager = remember { HealthConnectManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthManager.permissionContract()
    ) { vm.sync() }

    // Wait for the first DB read so onboarding doesn't flash on every launch.
    if (!profileLoaded) return

    if (profile?.onboarded != true) {
        OnboardingScreen(onComplete = { vm.completeOnboarding(it) })
        return
    }

    var activeTab by remember { mutableStateOf("LOG") }
    var activeMeal by remember { mutableStateOf("breakfast") }
    var showAddDialog by remember { mutableStateOf(false) }

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

    TerminalChrome(
        scrollProgress = scrollProgress,
        buttons = listOf(
            TerminalButtonSpec("LOG", selected = activeTab == "LOG") {
                if (activeTab == "LOG") showAddDialog = true else activeTab = "LOG"
            },
            TerminalButtonSpec("SCAN", selected = false) { onNavigate(Screen.SCAN) },
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
                    health = health,
                    onSync = { vm.sync() },
                    onRequestPermission = {
                        permissionLauncher.launch(healthManager.permissions)
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
}
