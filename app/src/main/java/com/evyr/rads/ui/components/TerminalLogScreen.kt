package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.local.FoodLogEntry

private val Amber = Color(0xFFFFB000)
private val AmberDim = Color(0x99FFB000)
private val RowHighlight = Color(0xFF3A2600)

data class MealTab(val label: String, val selected: Boolean)

/**
 * The in-screen UI: tab bar, meal sub-tabs, scrollable entry list, and a
 * right-side stat panel — mirrors the Pip-Boy INV/WEAPONS layout pattern.
 * fatGrams is shown per-meal-item deliberately; do not aggregate into a
 * daily total for the warning display (firm requirement).
 */
@Composable
fun TerminalLogScreen(
    mealTabs: List<MealTab>,
    entries: List<FoodLogEntry>,
    selectedEntry: FoodLogEntry?,
    onSelectEntry: (FoodLogEntry) -> Unit
) {
    Column {
        // Top-level tab bar
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            TabLabel("LOG", selected = true)
            Spacer(modifier = Modifier.width(16.dp))
            TabLabel("STATS", selected = false)
            Spacer(modifier = Modifier.width(16.dp))
            TabLabel("SYNC", selected = false)
            Spacer(modifier = Modifier.width(16.dp))
            TabLabel("SETUP", selected = false)
        }

        // Meal sub-tabs
        Row(modifier = Modifier.padding(bottom = 10.dp)) {
            mealTabs.forEach { tab ->
                Text(
                    text = tab.label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = if (tab.selected) Amber else AmberDim,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        Row {
            // Entry list
            Column(modifier = Modifier.weight(1f)) {
                entries.forEach { entry ->
                    val isSelected = entry.id == selectedEntry?.id
                    Text(
                        text = entry.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isSelected) Amber else AmberDim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) RowHighlight else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            // Stat panel for selected entry
            selectedEntry?.let { entry ->
                Column(modifier = Modifier.widthIn(min = 110.dp)) {
                    StatRow("CAL", "${entry.calories}")
                    StatRow("FAT", "${entry.fatGrams}g")
                    StatRow("PROTEIN", "${entry.proteinGrams}g")
                    StatRow("CARBS", "${entry.carbGrams}g")
                    StatRow("FLAG", if (entry.flagged) entry.flagReason ?: "YES" else "NONE")
                }
            }
        }
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = if (selected) Amber else AmberDim
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = AmberDim, modifier = Modifier.width(70.dp))
        Text(text = value, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Amber)
    }
}
