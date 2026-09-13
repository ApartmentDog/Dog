package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.data.local.HealthSnapshot

private val Amber = Color(0xFFFFB000)
private val AmberDim = Color(0x99FFB000)
private val AmberWarn = Color(0xFFFF6B2B)
private val RowHighlight = Color(0xFF3A2600)

data class MealTab(val label: String, val selected: Boolean)

@Composable
fun TerminalLogScreen(
    mealTabs: List<MealTab>,
    entries: List<FoodLogEntry>,
    selectedEntry: FoodLogEntry?,
    health: HealthSnapshot?,
    syncLabel: String,
    onSelectMeal: (String) -> Unit,
    onSelectEntry: (FoodLogEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            TabLabel("LOG", selected = true)
            Spacer(Modifier.width(16.dp))
            TabLabel("STATS", selected = false)
            Spacer(Modifier.width(16.dp))
            TabLabel("SYNC", selected = false)
            Spacer(Modifier.width(16.dp))
            TabLabel("SETUP", selected = false)
        }

        Row(modifier = Modifier.padding(bottom = 10.dp)) {
            mealTabs.forEach { tab ->
                Text(
                    text = tab.label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = if (tab.selected) Amber else AmberDim,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { onSelectMeal(tab.label.lowercase()) }
                )
            }
        }

        if (entries.isEmpty()) {
            Text(
                "> NO ENTRIES. PRESS [LOG] TO ADD.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AmberDim,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            entries.forEach { entry ->
                val isSelected = entry.id == selectedEntry?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) RowHighlight else Color.Transparent)
                        .clickable { onSelectEntry(entry) }
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = entry.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (entry.flagged) AmberWarn else if (isSelected) Amber else AmberDim,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${entry.calories}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isSelected) Amber else AmberDim
                    )
                }
            }

            selectedEntry?.let { entry ->
                Spacer(Modifier.height(10.dp))
                Divider()
                StatRow("CAL", "${entry.calories}")
                StatRow(
                    "FAT",
                    "${entry.fatGrams}g" + if (entry.flagged) "  !" else "",
                    warn = entry.flagged
                )
                StatRow("PROTEIN", "${entry.proteinGrams}g")
                StatRow("CARBS", "${entry.carbGrams}g")
                entry.flagReason?.let { StatRow("FLAG", it, warn = true) }
            }
        }

        Spacer(Modifier.height(14.dp))
        Divider()
        Text(
            "HEALTH CONNECT SYNC  [$syncLabel]",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberDim,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        StatRow("STEPS", health?.steps?.toString() ?: "--")
        StatRow("WEIGHT", health?.weightKg?.let { String.format("%.1f kg", it) } ?: "-- kg")
        StatRow("EXERCISE", health?.exerciseMinutes?.let { "$it min" } ?: "-- min")
    }
}

@Composable
private fun Divider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x4DFFB000))
    )
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
private fun StatRow(label: String, value: String, warn: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberDim,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = if (warn) AmberWarn else Amber
        )
    }
}
