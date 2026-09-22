package com.evyr.rads.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.Units
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.data.local.HealthSnapshot
import com.evyr.rads.data.local.DEFAULT_FAT_WARN_GRAMS
import com.evyr.rads.data.local.DEFAULT_CALORIE_TARGET
import com.evyr.rads.data.local.UserProfile

@Composable
fun TabStatsView(
    scrollState: ScrollState,
    isToday: Boolean,
    entries: List<FoodLogEntry>,
    profile: UserProfile?,
    health: HealthSnapshot?,
    mealSlots: List<String>
) {
    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {

        val target = profile?.calorieTarget() ?: DEFAULT_CALORIE_TARGET
        val totalCals = entries.sumOf { it.calories }
        val totalProtein = entries.sumOf { it.proteinGrams }
        val totalCarbs = entries.sumOf { it.carbGrams }

        SectionLabel(if (isToday) "Today's intake" else "Selected day")
        BarMeter("Calories", totalCals.toDouble(), target.toDouble(), "")
        StatRow("Protein", "${trim(totalProtein)} g")
        StatRow("Carbs", "${trim(totalCarbs)} g")
        StatRow("Entries", "${entries.size}")

        Spacer(Modifier.height(6.dp))
        Hairline()
        SectionLabel("Fat by meal")
        Text(
            "Fat is assessed per meal, not per day.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        val limit = profile?.fatWarnGramsPerMeal ?: DEFAULT_FAT_WARN_GRAMS
        mealSlots.forEach { slot ->
            val slotFat = entries.filter { it.mealSlot == slot }.sumOf { it.fatGrams }
            BarMeter(
                label = slot.replaceFirstChar { it.uppercase() },
                current = slotFat,
                target = limit,
                unit = "g",
                warn = slotFat >= limit
            )
        }

        Spacer(Modifier.height(6.dp))
        Hairline()
        SectionLabel("Body & activity")
        StatRow("Steps", health?.steps?.toString() ?: "--", emphasize = true)
        StatRow("Exercise", health?.exerciseMinutes?.let { "$it min" } ?: "-- min")
        val imp = profile?.useImperial ?: true
        val wUnit = if (imp) "lb" else "kg"
        val shownKg = health?.weightKg ?: profile?.weightKg?.takeIf { it > 0 }
        StatRow(
            "Weight",
            shownKg?.let { "${Units.displayWeight(it, imp)} $wUnit" } ?: "-- $wUnit"
        )
        profile?.goalWeightKg?.takeIf { it > 0 }?.let { g ->
            StatRow("Goal weight", "${Units.displayWeight(g, imp)} $wUnit")
        }
        profile?.poundsToGoal()?.let { lbs ->
            StatRow("To goal", "${trim(kotlin.math.abs(lbs))} lb")
        }
        profile?.weeksToGoal()?.let { wk ->
            StatRow("ETA", if (wk == 0) "At goal" else "$wk weeks")
        }

        Spacer(Modifier.height(6.dp))
        Hairline()
        SectionLabel("Targets")
        StatRow("BMR", profile?.bmr()?.toInt()?.toString() ?: "--")
        StatRow("TDEE", profile?.tdee()?.toInt()?.toString() ?: "--")
        StatRow("Goal", profile?.goal?.replaceFirstChar { it.uppercase() } ?: "--")
        StatRow("Rate", profile?.let { "${trim(it.rateLbsPerWeek)} lb/wk" } ?: "--")
        StatRow("Target", "$target kcal", emphasize = true)

        Spacer(Modifier.height(10.dp))
    }
}

