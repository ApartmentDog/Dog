package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.evyr.rads.data.FoodPortion
import com.evyr.rads.data.ScannedFood

@Composable
fun FoodSearchDialog(
    mealSlot: String,
    query: String,
    results: List<ScannedFood>,
    searching: Boolean,
    message: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (ScannedFood) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val dim = onSurface.copy(alpha = 0.6f)
    val faint = onSurface.copy(alpha = 0.45f)
    val primary = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .heightIn(max = 580.dp)
        ) {
            Text(
                "Find food — ${mealSlot.replaceFirstChar { it.uppercase() }}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = onSurface
                    ),
                    cursorBrush = SolidColor(primary),
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Find",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    modifier = Modifier
                        .clickable { onSearch() }
                        .padding(vertical = 9.dp, horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            Hairline()

            when {
                searching -> Text(
                    "Searching...",
                    fontSize = 14.sp,
                    color = dim,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
                message != null -> Text(
                    message,
                    fontSize = 13.sp,
                    color = dim,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
                results.isEmpty() -> Text(
                    "Type a food and press Find — e.g. \"grilled chicken breast\"",
                    fontSize = 13.sp,
                    color = faint,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
                else -> LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(results) { food ->
                        ResultRow(food) { onPick(food) }
                        Hairline()
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = dim,
                    modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    "Enter manually",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = dim,
                    modifier = Modifier.clickable { onManual() }.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultRow(food: ScannedFood, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val error = MaterialTheme.colorScheme.error
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                food.name,
                fontSize = 14.sp,
                color = onSurface,
                modifier = Modifier.weight(1f)
            )
            if (food.source == "vision") {
                Text(
                    "Estimated",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = error
                )
            }
        }
        Text(
            "${food.calories} kcal  ·  Fat ${food.fatGrams}g  ·  Protein ${food.proteinGrams}g  ·  Carbs ${food.carbGrams}g",
            fontSize = 12.sp,
            color = onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 3.dp)
        )
        food.servingNote?.let {
            Text(
                it,
                fontSize = 11.sp,
                color = if (food.basisGrams != null) error else onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Portion multiplier shown before the verdict, since servings rarely match. */
@Composable
fun QuantityDialog(
    food: ScannedFood,
    portionOptions: List<FoodPortion> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (ScannedFood) -> Unit
) {
    // When the source gave no serving size, the numbers are per 100 g and the
    // honest question is "how many grams", not "how many servings".
    val forcedAmount = food.basisGrams != null
    val canUseAmount = forcedAmount || food.servingGrams != null
    var byAmount by remember(food.name) { mutableStateOf(forcedAmount) }
    val gramsMode = forcedAmount || byAmount
    // What the nutrition numbers are relative to, in g/ml.
    val basis = food.basisGrams ?: food.servingGrams ?: 1.0

    // A chosen published portion overrides the raw amount box.
    var chosenPortion by remember(food.name) { mutableStateOf<FoodPortion?>(null) }
    var unit by remember(food.name) { mutableStateOf(MeasureUnit.G) }
    var amount by remember(food.name) {
        mutableStateOf(if (forcedAmount) MeasureUnit.G.default else "1")
    }
    val entered = amount.toDoubleOrNull() ?: if (gramsMode) basis else 1.0

    val multiplier = when {
        chosenPortion != null -> (chosenPortion!!.gramWeight / basis) * entered
        gramsMode -> (entered * unit.perUnit) / basis
        else -> entered
    }

    val presets = when {
        chosenPortion != null -> listOf("0.5", "1", "1.5", "2", "3")
        gramsMode -> unit.presets
        else -> listOf("0.5", "1", "1.5", "2", "3")
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val dim = onSurface.copy(alpha = 0.6f)
    val faint = onSurface.copy(alpha = 0.45f)
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val selectedBg = primary.copy(alpha = 0.14f)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                "Portion",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                food.name,
                fontSize = 14.sp,
                color = onSurface
            )
            food.servingNote?.let {
                Text(
                    it,
                    fontSize = 12.sp,
                    color = faint,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (forcedAmount && portionOptions.isEmpty()) {
                Text(
                    "No serving size published — enter an amount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Published portions first — nobody weighs a fast-food biscuit.
            if (portionOptions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Portion",
                    fontSize = 12.sp,
                    color = dim
                )
                portionOptions.forEach { option ->
                    val isSel = chosenPortion?.label == option.label
                    Text(
                        text = "${option.label}  (${option.gramWeight.toInt()} g)",
                        fontSize = 14.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) primary else onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chosenPortion = if (isSel) null else option
                                amount = "1"
                            }
                            .background(if (isSel) selectedBg else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (!forcedAmount && canUseAmount && chosenPortion == null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    listOf(false to "Servings", true to "Amount").forEach { (mode, label) ->
                        val isSel = byAmount == mode
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) primary else faint,
                            modifier = Modifier
                                .clickable {
                                    byAmount = mode
                                    amount = if (mode) unit.default else "1"
                                }
                                .background(if (isSel) selectedBg else surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                food.servingGrams?.let { sg ->
                    Text(
                        "1 serving = ${sg.toInt()} g/ml",
                        fontSize = 11.sp,
                        color = faint,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            if (gramsMode && chosenPortion == null) {
                Text(
                    "Unit",
                    fontSize = 12.sp,
                    color = dim
                )
                Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)) {
                    MeasureUnit.values().forEach { u ->
                        val isSel = u == unit
                        Text(
                            text = u.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) primary else faint,
                            modifier = Modifier
                                .clickable {
                                    if (u != unit) {
                                        unit = u
                                        amount = u.default
                                    }
                                }
                                .background(if (isSel) selectedBg else surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 11.dp, vertical = 8.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                if (unit.isVolume) {
                    Text(
                        "Volume assumes roughly water density — close for most drinks.",
                        fontSize = 11.sp,
                        color = faint,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            Text(
                when {
                    chosenPortion != null -> "How many"
                    gramsMode -> "Amount (${unit.label})"
                    else -> "Servings"
                },
                fontSize = 12.sp,
                color = dim
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)) {
                presets.forEach { preset ->
                    val isSel = amount.trim() == preset ||
                        (preset == "1" && amount.trim() == "1.0")
                    Text(
                        text = preset,
                        fontSize = 14.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) primary else faint,
                        modifier = Modifier
                            .clickable { amount = preset }
                            .background(if (isSel) selectedBg else surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Or type",
                    fontSize = 12.sp,
                    color = dim,
                    modifier = Modifier.width(88.dp).padding(top = 8.dp)
                )
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = onSurface
                    ),
                    cursorBrush = SolidColor(primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            Hairline()
            Spacer(Modifier.height(10.dp))
            StatRow("Calories", "${(food.calories * multiplier).toInt()}")
            StatRow("Fat", "${round1(food.fatGrams * multiplier)} g")
            StatRow("Protein", "${round1(food.proteinGrams * multiplier)} g")
            StatRow("Carbs", "${round1(food.carbGrams * multiplier)} g")
            food.sugarGrams?.let { StatRow("Sugar", "${round1(it * multiplier)} g") }
            food.sodiumMg?.let { StatRow("Sodium", "${kotlin.math.round(it * multiplier).toInt()} mg") }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Back",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = dim,
                    modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
                )
                Spacer(Modifier.width(24.dp))
                Text(
                    "Assess",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    modifier = Modifier
                        .clickable {
                            onConfirm(
                                food.copy(
                                    calories = (food.calories * multiplier).toInt(),
                                    fatGrams = round1(food.fatGrams * multiplier),
                                    proteinGrams = round1(food.proteinGrams * multiplier),
                                    carbGrams = round1(food.carbGrams * multiplier),
                                    saturatedFatGrams = food.saturatedFatGrams
                                        ?.let { round1(it * multiplier) },
                                    sugarGrams = food.sugarGrams?.let { round1(it * multiplier) },
                                    fiberGrams = food.fiberGrams?.let { round1(it * multiplier) },
                                    sodiumMg = food.sodiumMg?.let { kotlin.math.round(it * multiplier) },
                                    servingNote = when {
                                        chosenPortion != null ->
                                            "${trimQty(entered)} x ${chosenPortion!!.label}"
                                        gramsMode -> "${trimQty(entered)} ${unit.label}"
                                        else -> food.servingNote
                                    },
                                    basisGrams = null
                                )
                            )
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

private fun round1(v: Double): Double = kotlin.math.round(v * 10) / 10.0

/**
 * Units for entering an amount when a food only publishes per-100 values.
 * perUnit converts one unit into grams (or ml, treated as grams for liquids).
 */
private enum class MeasureUnit(
    val label: String,
    val perUnit: Double,
    val default: String,
    val presets: List<String>,
    val isVolume: Boolean
) {
    G("g", 1.0, "100", listOf("50", "100", "150", "200", "250"), false),
    OZ("oz", 28.3495, "4", listOf("1", "2", "4", "6", "8"), false),
    ML("ml", 1.0, "250", listOf("100", "250", "355", "500"), true),
    FL_OZ("fl oz", 29.5735, "12", listOf("8", "12", "16", "20"), true),
    CUP("cup", 236.588, "1", listOf("0.5", "1", "1.5", "2"), true)
}

private fun trimQty(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
