package com.evyr.rads.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.Units
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.data.local.HealthSnapshot
import com.evyr.rads.data.local.DEFAULT_FAT_WARN_GRAMS
import com.evyr.rads.data.local.DEFAULT_CALORIE_TARGET
import com.evyr.rads.data.local.UserProfile
import com.evyr.rads.ui.theme.*

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

        SectionLabel(if (isToday) "TODAY / INTAKE" else "SELECTED DAY / INTAKE")
        BarMeter("CALORIES", totalCals.toDouble(), target.toDouble(), "")
        StatRow("PROTEIN", "${trim(totalProtein)} g")
        StatRow("CARBS", "${trim(totalCarbs)} g")
        StatRow("ENTRIES", "${entries.size}")

        Spacer(Modifier.height(6.dp))
        Hairline()
        SectionLabel("FAT BY MEAL")
        Text(
            "Fat is assessed per meal, not per day.",
            fontSize = 12.sp,
            color = AmberFaint,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        val limit = profile?.fatWarnGramsPerMeal ?: DEFAULT_FAT_WARN_GRAMS
        mealSlots.forEach { slot ->
            val slotFat = entries.filter { it.mealSlot == slot }.sumOf { it.fatGrams }
            BarMeter(
                label = slot.uppercase(),
                current = slotFat,
                target = limit,
                unit = "g",
                warn = slotFat >= limit
            )
        }

        Spacer(Modifier.height(6.dp))
        Hairline()
        SectionLabel("BODY / ACTIVITY")
        StatRow("STEPS", health?.steps?.toString() ?: "--", emphasize = true)
        StatRow("EXERCISE", health?.exerciseMinutes?.let { "$it min" } ?: "-- min")
        val imp = profile?.useImperial ?: true
        val wUnit = if (imp) "lb" else "kg"
        val shownKg = health?.weightKg ?: profile?.weightKg?.takeIf { it > 0 }
        StatRow(
            "WEIGHT",
            shownKg?.let { "${Units.displayWeight(it, imp)} $wUnit" } ?: "-- $wUnit"
        )
        profile?.goalWeightKg?.takeIf { it > 0 }?.let { g ->
            StatRow("GOAL WT", "${Units.displayWeight(g, imp)} $wUnit")
        }
        profile?.poundsToGoal()?.let { lbs ->
            StatRow("TO GOAL", "${trim(kotlin.math.abs(lbs))} lb")
        }
        profile?.weeksToGoal()?.let { wk ->
            StatRow("ETA", if (wk == 0) "AT GOAL" else "$wk weeks")
        }

        Spacer(Modifier.height(6.dp))
        Hairline()
        SectionLabel("TARGETS")
        StatRow("BMR", profile?.bmr()?.toInt()?.toString() ?: "--")
        StatRow("TDEE", profile?.tdee()?.toInt()?.toString() ?: "--")
        StatRow("GOAL", profile?.goal?.uppercase() ?: "--")
        StatRow("RATE", profile?.let { "${trim(it.rateLbsPerWeek)} lb/wk" } ?: "--")
        StatRow("TARGET", "$target kcal", emphasize = true)

        Spacer(Modifier.height(10.dp))
    }
}
