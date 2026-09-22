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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.Condition
import com.evyr.rads.data.ConditionFlags
import com.evyr.rads.data.Verdict
import com.evyr.rads.data.local.FoodLogEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TabLogView(
    viewDate: LocalDate,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onJumpToToday: () -> Unit,
    mealSlots: List<String>,
    activeMeal: String,
    dayEntries: List<FoodLogEntry>,
    calorieTarget: Int,
    entries: List<FoodLogEntry>,
    selectedEntry: FoodLogEntry?,
    fatWarnGrams: Double,
    conditions: Set<Condition>,
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

    val onSurface = MaterialTheme.colorScheme.onSurface
    val dim = onSurface.copy(alpha = 0.6f)
    val faint = onSurface.copy(alpha = 0.4f)
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    Column(Modifier.fillMaxWidth()) {

        // ---- Whole-day totals ----
        val dayCals = dayEntries.sumOf { it.calories }
        val dayFat = dayEntries.sumOf { it.fatGrams }
        val dayProtein = dayEntries.sumOf { it.proteinGrams }
        val dayCarbs = dayEntries.sumOf { it.carbGrams }
        val remaining = calorieTarget - dayCals

        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                fontSize = 16.sp,
                color = dim,
                modifier = Modifier
                    .clickable { onPreviousDay() }
                    .padding(end = 10.dp, top = 2.dp, bottom = 2.dp)
            )
            Text(
                if (isToday) "Today"
                else viewDate.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                fontSize = 13.sp,
                fontWeight = if (isToday) FontWeight.Normal else FontWeight.Bold,
                color = if (isToday) faint else primary
            )
            Text(
                "›",
                fontSize = 16.sp,
                color = if (isToday) faint.copy(alpha = 0.3f) else dim,
                modifier = Modifier
                    .clickable(enabled = !isToday) { onNextDay() }
                    .padding(start = 10.dp, top = 2.dp, bottom = 2.dp)
            )
            if (!isToday) {
                Text(
                    "Jump to today",
                    fontSize = 12.sp,
                    color = primary,
                    modifier = Modifier
                        .clickable { onJumpToToday() }
                        .padding(start = 10.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (remaining >= 0) "$remaining left" else "${-remaining} over",
                fontSize = 12.sp,
                color = if (remaining >= 0) dim else error
            )
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(
                "$dayCals",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (remaining >= 0) onSurface else error
            )
            Text(
                " / $calorieTarget kcal",
                fontSize = 13.sp,
                color = dim,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Calorie progress for the day
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val pct =
                if (calorieTarget > 0) (dayCals.toFloat() / calorieTarget).coerceIn(0f, 1f)
                else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (remaining >= 0) primary else error)
            )
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp)) {
            DayMacro("Protein", "${trim(dayProtein)}g", Modifier.weight(1f))
            DayMacro("Carbs", "${trim(dayCarbs)}g", Modifier.weight(1f))
            DayMacro("Fat", "${trim(dayFat)}g", Modifier.weight(1f))
        }

        Hairline()
        Spacer(Modifier.height(10.dp))

        // Meal slot selector
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            mealSlots.forEach { slot ->
                val isSel = slot == activeMeal
                Text(
                    text = slot.replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) primary else faint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectMeal(slot) }
                        .background(if (isSel) primary.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Hairline()

        val mealFat = entries.sumOf { it.fatGrams }
        val mealCals = entries.sumOf { it.calories }

        // Per-meal summary — fat is judged per meal, never against a daily budget
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Text(
                "$mealCals kcal",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Fat ${trim(mealFat)}g / ${trim(fatWarnGrams)}g",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (mealFat >= fatWarnGrams) error else dim
            )
        }
        if (mealFat >= fatWarnGrams && entries.isNotEmpty()) {
            Text(
                "Over your fat limit for this meal",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = error,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        ConditionFlags.forMeal(entries, conditions).forEach { w ->
            Text(
                "${w.condition.short}: ${w.details.joinToString("; ")}",
                fontSize = 12.sp,
                fontWeight = if (w.severity == Verdict.OVER_LIMIT) FontWeight.Bold else FontWeight.Normal,
                color = severityColor(w.severity),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Hairline()

        if (entries.isEmpty()) {
            Text(
                "Nothing logged for ${activeMeal} yet. Tap add to log something.",
                fontSize = 13.sp,
                color = dim,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                items(entries, key = { it.id }) { entry ->
                    val isSel = entry.id == selectedEntry?.id
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onSelectEntry(entry) }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        val flags = ConditionFlags.forEntry(entry, conditions)
                        val worstFlag = flags.maxByOrNull { it.severity.ordinal }?.severity
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                entry.name,
                                fontSize = 14.sp,
                                color = when {
                                    worstFlag == Verdict.OVER_LIMIT -> error
                                    isSel -> onSurface
                                    else -> dim
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${entry.calories}",
                                fontSize = 14.sp,
                                color = if (isSel) onSurface else dim
                            )
                        }
                        if (flags.isNotEmpty()) {
                            Row(Modifier.padding(top = 3.dp)) {
                                flags.forEachIndexed { i, w ->
                                    if (i > 0) {
                                        Text(
                                            " · ",
                                            fontSize = 12.sp,
                                            color = faint
                                        )
                                    }
                                    Text(
                                        w.condition.short,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = severityColor(w.severity)
                                    )
                                }
                            }
                        }
                        if (isSel) {
                            Spacer(Modifier.height(8.dp))
                            StatRow("Fat", "${trim(entry.fatGrams)} g")
                            StatRow("Protein", "${trim(entry.proteinGrams)} g")
                            StatRow("Carbs", "${trim(entry.carbGrams)} g")
                            entry.saturatedFatGrams?.let { StatRow("Sat fat", "${trim(it)} g") }
                            entry.sugarGrams?.let { StatRow("Sugar", "${trim(it)} g") }
                            entry.fiberGrams?.let { StatRow("Fiber", "${trim(it)} g") }
                            entry.sodiumMg?.let { StatRow("Sodium", "${it.toInt()} mg") }
                            StatRow("Source", entry.source.replaceFirstChar { it.uppercase() })
                            flags.forEach { w ->
                                Text(
                                    "${w.condition.label}: ${w.details.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = severityColor(w.severity),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            ConditionFlags.irrelevantTriggers(entry, conditions)
                                .takeIf { it.isNotEmpty() }
                                ?.let { other ->
                                    Text(
                                        "Also watched: " + other.joinToString(", ") { it.label },
                                        fontSize = 11.sp,
                                        color = faint,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            AppAction("Delete entry", { onDeleteEntry(entry) }, warn = true)
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
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        Text(
            label,
            fontSize = 11.sp,
            color = onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface
        )
    }
}

/** Caution reads in the warning tone, over-limit in the error tone. */
@Composable
internal fun severityColor(v: Verdict) = when (v) {
    Verdict.OVER_LIMIT -> MaterialTheme.colorScheme.error
    Verdict.CAUTION -> MaterialTheme.colorScheme.primary
    Verdict.PASS -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
}

internal fun trim(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)

