package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.ui.theme.*

@Composable
fun TabLogView(
    mealSlots: List<String>,
    activeMeal: String,
    dayEntries: List<FoodLogEntry>,
    calorieTarget: Int,
    entries: List<FoodLogEntry>,
    selectedEntry: FoodLogEntry?,
    fatWarnGrams: Double,
    listState: LazyListState,
    onSelectMeal: (String) -> Unit,
    onSelectEntry: (FoodLogEntry) -> Unit,
    onDeleteEntry: (FoodLogEntry) -> Unit
) {
    // Expanding an entry can push its own header off-screen; bring it back.
    LaunchedEffect(selectedEntry?.id) {
        val id = selectedEntry?.id ?: return@LaunchedEffect
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            runCatching { listState.animateScrollToItem(index) }
        }
    }

    Column(Modifier.fillMaxWidth()) {

        // ---- Whole-day totals ----
        val dayCals = dayEntries.sumOf { it.calories }
        val dayFat = dayEntries.sumOf { it.fatGrams }
        val dayProtein = dayEntries.sumOf { it.proteinGrams }
        val dayCarbs = dayEntries.sumOf { it.carbGrams }
        val remaining = calorieTarget - dayCals

        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(
                "TODAY",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = AmberFaint,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (remaining >= 0) "$remaining LEFT" else "${-remaining} OVER",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = if (remaining >= 0) AmberFaint else AmberWarn
            )
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(
                "$dayCals",
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (remaining >= 0) AmberBright else AmberWarn
            )
            Text(
                " / $calorieTarget kcal",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AmberDim,
                modifier = Modifier.padding(top = 7.dp)
            )
        }

        // Calorie progress for the day
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(ScreenInk)
        ) {
            val pct =
                if (calorieTarget > 0) (dayCals.toFloat() / calorieTarget).coerceIn(0f, 1f)
                else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(8.dp)
                    .background(if (remaining >= 0) Amber else AmberWarn)
            )
        }

        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)) {
            DayMacro("PROTEIN", "${trim(dayProtein)}g", Modifier.weight(1f))
            DayMacro("CARBS", "${trim(dayCarbs)}g", Modifier.weight(1f))
            DayMacro("FAT", "${trim(dayFat)}g", Modifier.weight(1f))
        }

        Hairline()
        Spacer(Modifier.height(8.dp))

        // Meal slot selector
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            mealSlots.forEach { slot ->
                val isSel = slot == activeMeal
                Text(
                    text = slot.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) Amber else AmberFaint,
                    modifier = Modifier
                        .clickable { onSelectMeal(slot) }
                        .padding(end = 10.dp, top = 2.dp, bottom = 2.dp)
                )
            }
        }
        Hairline()

        val mealFat = entries.sumOf { it.fatGrams }
        val mealCals = entries.sumOf { it.calories }

        // Per-meal summary — fat is judged per meal, never against a daily budget
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
            Text(
                "$mealCals kcal",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Amber,
                modifier = Modifier.weight(1f)
            )
            Text(
                "FAT ${trim(mealFat)}g / ${trim(fatWarnGrams)}g",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (mealFat >= fatWarnGrams) AmberWarn else Amber
            )
        }
        if (mealFat >= fatWarnGrams && entries.isNotEmpty()) {
            Text(
                "!! MEAL FAT LIMIT EXCEEDED",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AmberWarn,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Hairline()

        if (entries.isEmpty()) {
            Text(
                "> NO ENTRIES FOR ${activeMeal.uppercase()}\n> PRESS [LOG] TO ADD",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AmberDim,
                modifier = Modifier.padding(vertical = 14.dp)
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                items(entries, key = { it.id }) { entry ->
                    val isSel = entry.id == selectedEntry?.id
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(if (isSel) RowHighlight else Color.Transparent)
                            .clickable { onSelectEntry(entry) }
                            .padding(horizontal = 5.dp, vertical = 6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                if (isSel) "> ${entry.name}" else "  ${entry.name}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (entry.flagged) AmberWarn else if (isSel) AmberBright else AmberDim,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${entry.calories}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isSel) AmberBright else AmberDim
                            )
                        }
                        if (isSel) {
                            Spacer(Modifier.height(6.dp))
                            StatRow("FAT", "${trim(entry.fatGrams)} g", warn = entry.flagged)
                            StatRow("PROTEIN", "${trim(entry.proteinGrams)} g")
                            StatRow("CARBS", "${trim(entry.carbGrams)} g")
                            StatRow("SOURCE", entry.source.uppercase())
                            entry.flagReason?.let { StatRow("FLAG", it, warn = true) }
                            TerminalAction("[DELETE ENTRY]", { onDeleteEntry(entry) }, warn = true)
                        }
                    }
                    Hairline()
                }
            }
        }
    }
}

@Composable
private fun DayMacro(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = AmberFaint
        )
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Amber
        )
    }
}

internal fun trim(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
