package com.evyr.rads.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.ui.Screen
import com.evyr.rads.ui.components.MealTab
import com.evyr.rads.ui.components.TerminalButtonSpec
import com.evyr.rads.ui.components.TerminalChrome
import com.evyr.rads.ui.components.TerminalLogScreen

@Composable
fun TodayScreen(onNavigate: (Screen) -> Unit) {
    // Placeholder data until wired to Room + ViewModel.
    val sampleEntries = remember {
        listOf(
            FoodLogEntry(id = 1, timestamp = 0L, mealSlot = "breakfast", name = "Oatmeal + berries", calories = 310, fatGrams = 6.0, proteinGrams = 11.0, carbGrams = 48.0, source = "manual"),
            FoodLogEntry(id = 2, timestamp = 0L, mealSlot = "breakfast", name = "Scrambled eggs (2)", calories = 180, fatGrams = 12.0, proteinGrams = 14.0, carbGrams = 2.0, source = "manual"),
        )
    }
    var selected by remember { mutableStateOf<FoodLogEntry?>(sampleEntries.firstOrNull()) }
    var activeButton by remember { mutableStateOf("LOG") }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        TerminalChrome(
            buttons = listOf(
                TerminalButtonSpec("LOG", selected = activeButton == "LOG", onClick = { activeButton = "LOG" }),
                TerminalButtonSpec("SCAN", selected = activeButton == "SCAN", onClick = { activeButton = "SCAN"; onNavigate(Screen.SCAN) }),
                TerminalButtonSpec("SYNC", selected = activeButton == "SYNC", onClick = { activeButton = "SYNC"; onNavigate(Screen.HEALTH_SYNC) }),
            )
        ) {
            TerminalLogScreen(
                mealTabs = listOf(
                    MealTab("BREAKFAST", selected = true),
                    MealTab("LUNCH", selected = false),
                    MealTab("DINNER", selected = false),
                    MealTab("SNACK", selected = false),
                ),
                entries = sampleEntries,
                selectedEntry = selected,
                onSelectEntry = { selected = it }
            )
        }
    }
}
