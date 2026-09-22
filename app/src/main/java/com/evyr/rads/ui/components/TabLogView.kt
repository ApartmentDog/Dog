package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.Condition
import com.evyr.rads.data.ConditionFlags
import com.evyr.rads.data.Verdict
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.ui.theme.Terracotta
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
    // listState is accepted for call-site compatibility with the other tabs'
    // scroll-state pattern, but the accordion layout below scrolls itself
    // (verticalScroll on the outer Column) rather than via LazyColumn, so it
    // isn't wired to anything here.

    val dayCals = dayEntries.sumOf { it.calories }
    val dayFat = dayEntries.sumOf { it.fatGrams }
    val dayProtein = dayEntries.sumOf { it.proteinGrams }
    val dayCarbs = dayEntries.sumOf { it.carbGrams }

    // Rough daily macro targets for the bar fills -- protein/carbs don't have
    // their own stored goals, so these scale off the calorie target the same
    // rough ratios a typical macro split uses. Purely a visual fill, not a
    // tracked limit the way fat is.
    val proteinTarget = (calorieTarget * 0.24 / 4).coerceAtLeast(1.0)
    val carbTarget = (calorieTarget * 0.44 / 4).coerceAtLeast(1.0)

    // Which meal cards are expanded. A meal with items in it starts expanded,
    // same behavior as the original PWA (expandedMeals.add on first render).
    val expanded = remember(mealSlots) {
        mutableStateOf(mealSlots.filter { slot -> dayEntries.any { it.mealSlot == slot } }.toMutableSet())
    }

    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        LogHero(
            viewDate = viewDate,
            isToday = isToday,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onJumpToToday = onJumpToToday,
            dayCals = dayCals,
            calorieTarget = calorieTarget,
            dayProtein = dayProtein,
            proteinTarget = proteinTarget,
            dayCarbs = dayCarbs,
            carbTarget = carbTarget
        )

        Spacer(Modifier.height(14.dp))

        Column(Modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            mealSlots.forEach { slot ->
                val slotEntries = dayEntries.filter { it.mealSlot == slot }
                MealAccordionCard(
                    slot = slot,
                    label = slot.replaceFirstChar { it.uppercase() },
                    entries = slotEntries,
                    expanded = slot in expanded.value,
                    onToggle = {
                        expanded.value = if (slot in expanded.value) {
                            expanded.value - slot
                        } else {
                            expanded.value + slot
                        }
                        onSelectMeal(slot)
                    },
                    onSelectEntry = onSelectEntry,
                    onDeleteEntry = onDeleteEntry,
                    onAdd = { onSelectMeal(slot) }
                )
            }
        }

        // Condition warnings for whichever meal is currently the log target
        // (activeMeal), shown below the accordion stack rather than inside
        // each card -- keeps the cards focused on what was eaten, warnings
        // stay in one predictable spot.
        val activeEntries = dayEntries.filter { it.mealSlot == activeMeal }
        val mealFat = activeEntries.sumOf { it.fatGrams }
        if (mealFat >= fatWarnGrams && activeEntries.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Over your fat limit for ${activeMeal}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
        ConditionFlags.forMeal(activeEntries, conditions).forEach { w ->
            Text(
                "${w.condition.short}: ${w.details.joinToString("; ")}",
                fontSize = 12.sp,
                fontWeight = if (w.severity == Verdict.OVER_LIMIT) FontWeight.Bold else FontWeight.Normal,
                color = severityColor(w.severity),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Gradient hero card: date nav, calorie ring, and macro bars with icon
 * chips. Replaces the old flat header row + thin progress bar.
 */
@Composable
private fun LogHero(
    viewDate: LocalDate,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onJumpToToday: () -> Unit,
    dayCals: Int,
    calorieTarget: Int,
    dayProtein: Double,
    proteinTarget: Double,
    dayCarbs: Double,
    carbTarget: Double
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF0A868), Terracotta)))
            .padding(20.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "\u2039",
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.clickable { onPreviousDay() }.padding(end = 8.dp)
                    )
                    Text(
                        if (isToday) "Today" else viewDate.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        "\u203A",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = if (isToday) 0.4f else 1f),
                        modifier = Modifier
                            .clickable(enabled = !isToday) { onNextDay() }
                            .padding(start = 8.dp)
                    )
                }
                if (!isToday) {
                    Text(
                        "Jump to today",
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onJumpToToday() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CalorieRing(eaten = dayCals, target = calorieTarget, modifier = Modifier.size(104.dp))
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    MacroBarRow(emoji = "\uD83E\uDD69", label = "Protein", value = dayProtein, target = proteinTarget)
                    MacroBarRow(emoji = "\uD83C\uDF5E", label = "Carbs", value = dayCarbs, target = carbTarget)
                }
            }
        }
    }
}

@Composable
private fun CalorieRing(eaten: Int, target: Int, modifier: Modifier = Modifier) {
    val pct = if (target > 0) (eaten.toFloat() / target).coerceIn(0f, 1f) else 0f
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(104.dp)) {
            val stroke = 11.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$eaten", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(
                "eaten",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun MacroBarRow(emoji: String, label: String, value: Double, target: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                Text(
                    "${value.toInt()}/${target.toInt()}g",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.height(2.dp))
            val pct = if (target > 0) (value / target).coerceIn(0.0, 1.0) else 0.0
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(pct.toFloat())
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                )
            }
        }
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
