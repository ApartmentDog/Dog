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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.evyr.rads.data.FoodPortion
import com.evyr.rads.data.ScannedFood
import com.evyr.rads.ui.theme.*

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
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .heightIn(max = 580.dp)
        ) {
            Text(
                "FOOD SEARCH / ${mealSlot.uppercase()}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Amber
                    ),
                    cursorBrush = SolidColor(Amber),
                    modifier = Modifier
                        .weight(1f)
                        .background(ScreenInk, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "FIND",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberBright,
                    modifier = Modifier
                        .clickable { onSearch() }
                        .padding(vertical = 7.dp, horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Hairline()

            when {
                searching -> Text(
                    "SEARCHING...",
                    fontSize = 22.sp,
                    color = AmberDim,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                message != null -> Text(
                    message,
                    fontSize = 22.sp,
                    color = AmberDim,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                results.isEmpty() -> Text(
                    "TYPE A FOOD AND PRESS [FIND]\n> e.g. \"grilled chicken breast\"",
                    fontSize = 22.sp,
                    color = AmberFaint,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                else -> LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(results) { food ->
                        ResultRow(food) { onPick(food) }
                        Hairline()
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "CANCEL",
                    fontSize = 22.sp,
                    color = AmberDim,
                    modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
                )
                Spacer(Modifier.width(18.dp))
                Text(
                    "MANUAL ENTRY",
                    fontSize = 22.sp,
                    color = AmberDim,
                    modifier = Modifier.clickable { onManual() }.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultRow(food: ScannedFood, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "${food.name}",
                fontSize = 22.sp,
                color = AmberBright,
                modifier = Modifier.weight(1f)
            )
            if (food.source == "vision") {
                Text(
                    "EST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarn
                )
            }
        }
        Text(
            "${food.calories} kcal  F ${food.fatGrams}g  P ${food.proteinGrams}g  C ${food.carbGrams}g",
            fontSize = 13.sp,
            color = AmberDim,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
        )
        food.servingNote?.let {
            Text(
                it,
                fontSize = 12.sp,
                color = if (food.basisGrams != null) AmberWarn else AmberFaint,
                modifier = Modifier.padding(start = 12.dp)
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
    val gramsMode = food.basisGrams != null
    val basis = food.basisGrams ?: 1.0

    // A chosen published portion overrides the raw amount box.
    var chosenPortion by remember(food.name) { mutableStateOf<FoodPortion?>(null) }
    var amount by remember { mutableStateOf(if (gramsMode) "100" else "1") }
    val entered = amount.toDoubleOrNull() ?: if (gramsMode) basis else 1.0

    val multiplier = when {
        chosenPortion != null -> (chosenPortion!!.gramWeight / basis) * entered
        gramsMode -> entered / basis
        else -> entered
    }

    val presets = when {
        chosenPortion != null -> listOf("0.5", "1", "1.5", "2", "3")
        gramsMode -> listOf("50", "100", "150", "200", "250")
        else -> listOf("0.5", "1", "1.5", "2", "3")
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(18.dp)
        ) {
            Text(
                "PORTION",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            Spacer(Modifier.height(6.dp))
            Text(
                food.name,
                fontSize = 22.sp,
                color = AmberBright
            )
            food.servingNote?.let {
                Text(
                    it,
                    fontSize = 13.sp,
                    color = AmberFaint,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (gramsMode && portionOptions.isEmpty()) {
                Text(
                    "!! NO SERVING SIZE PUBLISHED — ENTER WEIGHT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarn,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Published portions first — nobody weighs a fast-food biscuit.
            if (portionOptions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "PORTION",
                    fontSize = 13.sp,
                    color = AmberDim
                )
                portionOptions.forEach { option ->
                    val isSel = chosenPortion?.label == option.label
                    Text(
                        text = (if (isSel) "" else "  ") +
                            "${option.label}  (${option.gramWeight.toInt()} g)",
                        fontSize = 22.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) AmberBright else AmberFaint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chosenPortion = if (isSel) null else option
                                amount = "1"
                            }
                            .background(if (isSel) RowHighlight else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                when {
                    chosenPortion != null -> "HOW MANY"
                    gramsMode -> "GRAMS"
                    else -> "SERVINGS"
                },
                fontSize = 13.sp,
                color = AmberDim
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
                presets.forEach { preset ->
                    val isSel = amount.trim() == preset ||
                        (preset == "1" && amount.trim() == "1.0")
                    Text(
                        text = preset,
                        fontSize = 16.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) AmberBright else AmberFaint,
                        modifier = Modifier
                            .clickable { amount = preset }
                            .background(if (isSel) RowHighlight else ScreenInk)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                }
            }

            Row(Modifier.fillMaxWidth()) {
                Text(
                    "OR TYPE",
                    fontSize = 22.sp,
                    color = AmberDim,
                    modifier = Modifier.width(84.dp).padding(top = 6.dp)
                )
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Amber
                    ),
                    cursorBrush = SolidColor(Amber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenInk, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Hairline()
            Spacer(Modifier.height(8.dp))
            StatRow("CAL", "${(food.calories * multiplier).toInt()}")
            StatRow("FAT", "${round1(food.fatGrams * multiplier)} g")
            StatRow("PROTEIN", "${round1(food.proteinGrams * multiplier)} g")
            StatRow("CARBS", "${round1(food.carbGrams * multiplier)} g")

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "BACK",
                    fontSize = 22.sp,
                    color = AmberDim,
                    modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    "ASSESS",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberBright,
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
                                    servingNote = when {
                                        chosenPortion != null ->
                                            "${trimQty(entered)} x ${chosenPortion!!.label}"
                                        gramsMode -> "${entered.toInt()} g"
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

private fun trimQty(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
