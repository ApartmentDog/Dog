package com.evyr.rads.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evyr.rads.health.HealthConnectManager
import com.evyr.rads.ui.Screen
import com.evyr.rads.ui.TodayViewModel
import com.evyr.rads.ui.components.AddEntryDialog
import com.evyr.rads.ui.components.MealTab
import com.evyr.rads.ui.components.TerminalButtonSpec
import com.evyr.rads.ui.components.TerminalChrome
import com.evyr.rads.ui.components.TerminalLogScreen

private val MEAL_SLOTS = listOf("breakfast", "lunch", "dinner", "snack")

@Composable
fun TodayScreen(onNavigate: (Screen) -> Unit) {
    val vm: TodayViewModel = viewModel()
    val context = LocalContext.current

    val allEntries by vm.entries.collectAsState()
    val health by vm.healthToday.collectAsState()
    val selectedId by vm.selectedEntryId.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()

    var activeMeal by remember { mutableStateOf("breakfast") }
    var showAddDialog by remember { mutableStateOf(false) }

    val healthManager = remember { HealthConnectManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthManager.permissionContract()
    ) { vm.sync() }

    val visibleEntries = allEntries.filter { it.mealSlot == activeMeal }
    val selectedEntry = allEntries.firstOrNull { it.id == selectedId }

    val syncLabel = when (syncStatus) {
        TodayViewModel.SyncStatus.IDLE -> "READY"
        TodayViewModel.SyncStatus.SYNCING -> "..."
        TodayViewModel.SyncStatus.OK -> "OK"
        TodayViewModel.SyncStatus.UNAVAILABLE -> "N/A"
        TodayViewModel.SyncStatus.NO_PERMISSION -> "DENIED"
        TodayViewModel.SyncStatus.ERROR -> "ERR"
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        TerminalChrome(
            buttons = listOf(
                TerminalButtonSpec("LOG", selected = true) { showAddDialog = true },
                TerminalButtonSpec("SCAN", selected = false) { onNavigate(Screen.SCAN) },
                TerminalButtonSpec("SYNC", selected = false) {
                    if (healthManager.isAvailable()) {
                        permissionLauncher.launch(healthManager.permissions)
                    } else {
                        vm.sync()
                    }
                },
            )
        ) {
            TerminalLogScreen(
                mealTabs = MEAL_SLOTS.map { MealTab(it.uppercase(), selected = it == activeMeal) },
                entries = visibleEntries,
                selectedEntry = selectedEntry,
                health = health,
                syncLabel = syncLabel,
                onSelectMeal = { activeMeal = it },
                onSelectEntry = { vm.selectEntry(it.id) }
            )
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
