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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.local.FoodLogEntry
import com.evyr.rads.ui.theme.*

private data class MealTint(
    val bg: Color,
    val border: Color,
    val chip: Color,
    val text: Color
)

@Composable
private fun mealTint(slot: String, dark: Boolean): MealTint = when (slot) {
    "breakfast" -> if (dark)
        MealTint(MealBreakfastBgDark, MealBreakfastBorderDark, MealBreakfastChipDark, MealBreakfastTextDark)
    else MealTint(MealBreakfastBg, MealBreakfastBorder, MealBreakfastChip, MealBreakfastText)
    "lunch" -> if (dark)
        MealTint(MealLunchBgDark, MealLunchBorderDark, MealLunchChipDark, MealLunchTextDark)
    else MealTint(MealLunchBg, MealLunchBorder, MealLunchChip, MealLunchText)
    "dinner" -> if (dark)
        MealTint(MealDinnerBgDark, MealDinnerBorderDark, MealDinnerChipDark, MealDinnerTextDark)
    else MealTint(MealDinnerBg, MealDinnerBorder, MealDinnerChip, MealDinnerText)
    else -> if (dark)
        MealTint(MealSnackBgDark, MealSnackBorderDark, MealSnackChipDark, MealSnackTextDark)
    else MealTint(MealSnackBg, MealSnackBorder, MealSnackChip, MealSnackText)
}

private fun mealIcon(slot: String): String = when (slot) {
    "breakfast" -> "\uD83C\uDF73" // fried egg
    "lunch" -> "\uD83E\uDD57" // salad
    "dinner" -> "\uD83C\uDF7D\uFE0F" // fork and plate
    else -> "\uD83C\uDF7F" // popcorn
}

/**
 * Content-matched emoji per food name, ported from the original Gut Check
 * PWA's getFoodEmoji(). Falls back to a plate for anything unmatched. RADS
 * doesn't carry the old fried/spicy/acidic flag field on FoodLogEntry, so
 * only the name-matching half of the original function applies here --
 * condition warnings are already shown separately via ConditionFlags.
 */
private fun foodEmoji(name: String): String {
    val n = name.lowercase()
    return when {
        n.contains("chicken") -> "\uD83C\uDF57"
        n.contains("beef") || n.contains("burger") -> "\uD83C\uDF54"
        n.contains("pizza") -> "\uD83C\uDF55"
        n.contains("salad") -> "\uD83E\uDD57"
        n.contains("fish") || n.contains("tuna") || n.contains("salmon") -> "\uD83D\uDC1F"
        n.contains("egg") -> "\uD83E\uDD5A"
        n.contains("milk") || n.contains("yogurt") -> "\uD83E\uDD5B"
        n.contains("bread") || n.contains("toast") -> "\uD83C\uDF5E"
        n.contains("rice") -> "\uD83C\uDF5A"
        n.contains("pasta") || n.contains("noodle") -> "\uD83C\uDF5D"
        n.contains("coffee") -> "\u2615"
        else -> "\uD83C\uDF7D\uFE0F"
    }
}

/**
 * One meal's accordion card: tinted header (icon, name, item count, total
 * kcal, chevron) that expands to show each logged item plus an "add" button.
 * All four meals render stacked and independently expandable -- this
 * replaces the old single-active-meal tab switcher entirely.
 */
@Composable
fun MealAccordionCard(
    slot: String,
    label: String,
    entries: List<FoodLogEntry>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectEntry: (FoodLogEntry) -> Unit,
    onDeleteEntry: (FoodLogEntry) -> Unit,
    onAdd: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tint = mealTint(slot, dark)
    val total = entries.sumOf { it.calories }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.bg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.chip),
                contentAlignment = Alignment.Center
            ) {
                Text(mealIcon(slot), fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (entries.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tint.chip)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${entries.size}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = tint.text)
                }
            }
            Spacer(Modifier.weight(1f))
            if (total > 0) {
                Text("$total", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = tint.text)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (expanded) "\u25B4" else "\u25BE",
                fontSize = 14.sp,
                color = tint.text
            )
        }

        if (expanded) {
            Column(Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, bottom = 13.dp)) {
                entries.forEach { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onSelectEntry(entry) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(tint.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(foodEmoji(entry.name), fontSize = 17.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${trim(entry.fatGrams)}g fat \u00B7 ${trim(entry.proteinGrams)}g prot",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            "${entry.calories}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .clickable { onAdd() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+ Add to $label",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = tint.text
                    )
                }
            }
        }
    }
}
