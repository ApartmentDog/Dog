package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.evyr.rads.data.ScannedFood
import com.evyr.rads.data.Verdict
import com.evyr.rads.data.VerdictResult
import com.evyr.rads.ui.theme.*

private val PassGreen = Color(0xFF5FD35F)

@Composable
fun VerdictDialog(
    food: ScannedFood,
    verdict: VerdictResult,
    mealSlot: String,
    onDismiss: () -> Unit,
    onConfirm: (ScannedFood) -> Unit
) {
    var name by remember { mutableStateOf(food.name) }
    var calories by remember { mutableStateOf(food.calories.toString()) }
    var fat by remember { mutableStateOf(num(food.fatGrams)) }
    var protein by remember { mutableStateOf(num(food.proteinGrams)) }
    var carbs by remember { mutableStateOf(num(food.carbGrams)) }

    val stampColor = when (verdict.verdict) {
        Verdict.PASS -> PassGreen
        Verdict.CAUTION -> Amber
        Verdict.OVER_LIMIT -> AmberWarn
    }
    val stampText = when (verdict.verdict) {
        Verdict.PASS -> "PASS"
        Verdict.CAUTION -> "CAUTION"
        Verdict.OVER_LIMIT -> "OVER LIMIT"
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "> ASSESSMENT / ${mealSlot.uppercase()}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AmberFaint
            )
            Spacer(Modifier.height(10.dp))

            // The stamp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, stampColor, RoundedCornerShape(4.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stampText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = stampColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                verdict.headline,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = stampColor
            )

            Spacer(Modifier.height(8.dp))
            verdict.reasons.forEach { r ->
                Text(
                    "- $r",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = AmberDim,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }

            // ---- Condition checks ----
            if (verdict.warnings.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Hairline()
                Text(
                    "CONDITION CHECKS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = AmberFaint,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                verdict.warnings.forEach { w ->
                    val c = when (w.severity) {
                        Verdict.OVER_LIMIT -> AmberWarn
                        Verdict.CAUTION -> Amber
                        Verdict.PASS -> AmberFaint
                    }
                    Text(
                        w.condition.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = c
                    )
                    Text(
                        w.details.joinToString(", "),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (w.severity == Verdict.PASS) AmberFaint else AmberDim,
                        modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)
                    )
                }
            }

            // ---- Extra nutrients, when the source has them ----
            val extras = listOfNotNull(
                food.saturatedFatGrams?.let { "SAT FAT" to "${num(it)} g" },
                food.sugarGrams?.let { "SUGAR" to "${num(it)} g" },
                food.fiberGrams?.let { "FIBER" to "${num(it)} g" },
                food.sodiumMg?.let { "SODIUM" to "${it.toInt()} mg" }
            )
            if (extras.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                extras.forEach { (label, value) -> StatRow(label, value) }
            }

            Spacer(Modifier.height(10.dp))
            Hairline()
            Text(
                "ADJUST BEFORE LOGGING",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = AmberFaint,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            EditRow("NAME", name) { name = it }
            EditRow("CAL", calories, numeric = true) { calories = it }
            EditRow("FAT g", fat, numeric = true) { fat = it }
            EditRow("PROTEIN g", protein, numeric = true) { protein = it }
            EditRow("CARBS g", carbs, numeric = true) { carbs = it }

            food.barcode?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    "BARCODE $it",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = AmberFaint
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "[DISCARD]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = AmberDim,
                    modifier = Modifier.clickable { onDismiss() }.padding(6.dp)
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    if (verdict.verdict == Verdict.OVER_LIMIT) "[LOG ANYWAY]" else "[LOG IT]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (verdict.verdict == Verdict.OVER_LIMIT) AmberWarn else AmberBright,
                    modifier = Modifier
                        .clickable {
                            onConfirm(
                                food.copy(
                                    name = name.trim().ifBlank { food.name },
                                    calories = calories.toIntOrNull() ?: 0,
                                    fatGrams = fat.toDoubleOrNull() ?: 0.0,
                                    proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                                    carbGrams = carbs.toDoubleOrNull() ?: 0.0
                                )
                            )
                        }
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun EditRow(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberDim,
            modifier = Modifier.width(80.dp).padding(top = 5.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = if (numeric)
                KeyboardOptions(keyboardType = KeyboardType.Number)
            else KeyboardOptions.Default,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Amber
            ),
            cursorBrush = SolidColor(Amber),
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenInk, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp)
        )
    }
}

private fun num(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
