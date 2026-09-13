package com.evyr.rads.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.Units
import com.evyr.rads.data.local.UserProfile
import com.evyr.rads.ui.theme.*

@Composable
fun OnboardingScreen(onComplete: (UserProfile) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var imperial by remember { mutableStateOf(true) }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("unspecified") }

    var feet by remember { mutableStateOf("") }
    var inches by remember { mutableStateOf("") }
    var heightCmText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }

    var goal by remember { mutableStateOf("lose") }
    var goalWeightText by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("1.0") }
    var activity by remember { mutableStateOf("moderate") }
    var fatLimit by remember { mutableStateOf("15") }

    val lastStep = 4

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SandMid, SandDeep)))
            .padding(14.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "R.A.D.S.",
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            Text(
                "RATION ASSESSMENT & DIET SYSTEM",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = AmberDim
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(AmberHairline))
            Spacer(Modifier.height(12.dp))

            Text(
                "> SETUP [${step + 1}/${lastStep + 1}]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AmberFaint
            )
            Spacer(Modifier.height(10.dp))

            when (step) {
                0 -> {
                    Prompt("UNITS")
                    Choices("MEASUREMENT", listOf("imperial", "metric"),
                        if (imperial) "imperial" else "metric") {
                        imperial = it == "imperial"
                    }
                    Note(
                        if (imperial) "Pounds and feet/inches."
                        else "Kilograms and centimeters."
                    )
                }
                1 -> {
                    Prompt("OPERATOR")
                    Field("NAME", name) { name = it }
                    Field("AGE", age, numeric = true) { age = it }
                    Choices("SEX", listOf("male", "female", "unspecified"), sex) { sex = it }
                }
                2 -> {
                    Prompt("BODY")
                    if (imperial) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f)) {
                                Field("HEIGHT ft", feet, numeric = true) { feet = it }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                Field("in", inches, numeric = true) { inches = it }
                            }
                        }
                        Field("WEIGHT lb", weightText, numeric = true) { weightText = it }
                    } else {
                        Field("HEIGHT cm", heightCmText, numeric = true) { heightCmText = it }
                        Field("WEIGHT kg", weightText, numeric = true) { weightText = it }
                    }
                }
                3 -> {
                    Prompt("OBJECTIVE")
                    Choices("GOAL", listOf("lose", "maintain", "gain"), goal) { goal = it }
                    if (goal != "maintain") {
                        Field(
                            "TARGET ${if (imperial) "lb" else "kg"}",
                            goalWeightText,
                            numeric = true
                        ) { goalWeightText = it }
                        Choices(
                            "RATE (lb/week)",
                            listOf("0.5", "1.0", "1.5", "2.0"),
                            rate
                        ) { rate = it }
                        val r = rate.toDoubleOrNull() ?: 1.0
                        Note(
                            "${if (goal == "lose") "Losing" else "Gaining"} $r lb/week " +
                                "≈ ${((r * 3500) / 7).toInt()} kcal/day " +
                                (if (goal == "lose") "deficit." else "surplus.") +
                                if (r > 2.0) " Above 2 lb/week is rarely advisable." else ""
                        )
                    }
                    Choices(
                        "ACTIVITY",
                        listOf("sedentary", "light", "moderate", "active", "very_active"),
                        activity
                    ) { activity = it }
                }
                4 -> {
                    Prompt("FAT CEILING")
                    Note("Maximum fat grams in any single meal. Checked per meal, never as a daily total.")
                    Spacer(Modifier.height(6.dp))
                    Field("GRAMS/MEAL", fatLimit, numeric = true) { fatLimit = it }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                if (step > 0) {
                    Text(
                        "[BACK]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AmberDim,
                        modifier = Modifier.clickable { step-- }.padding(8.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Text(
                    if (step < lastStep) "[NEXT]" else "[INITIALIZE]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberBright,
                    modifier = Modifier
                        .clickable {
                            if (step < lastStep) {
                                step++
                            } else {
                                val heightCm = if (imperial) {
                                    Units.feetInchesToCm(
                                        feet.toIntOrNull() ?: 0,
                                        inches.toIntOrNull() ?: 0
                                    )
                                } else heightCmText.toDoubleOrNull() ?: 0.0

                                val weightKg =
                                    Units.parseWeightToKg(weightText, imperial) ?: 0.0
                                val goalKg =
                                    Units.parseWeightToKg(goalWeightText, imperial) ?: 0.0

                                onComplete(
                                    UserProfile(
                                        name = name.trim(),
                                        age = age.toIntOrNull() ?: 0,
                                        sex = sex,
                                        heightCm = heightCm,
                                        weightKg = weightKg,
                                        goal = goal,
                                        goalWeightKg = goalKg,
                                        rateLbsPerWeek = rate.toDoubleOrNull() ?: 1.0,
                                        activityLevel = activity,
                                        useImperial = imperial,
                                        fatWarnGramsPerMeal = fatLimit.toDoubleOrNull() ?: 15.0,
                                        onboarded = true
                                    )
                                )
                            }
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun Prompt(text: String) {
    Text(
        text,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Amber,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = AmberFaint,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = AmberDim,
            modifier = Modifier.width(92.dp).padding(top = 5.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else KeyboardOptions.Default,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Amber
            ),
            cursorBrush = SolidColor(Amber),
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenInk, RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun Choices(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AmberDim)
        Spacer(Modifier.height(3.dp))
        options.forEach { opt ->
            val isSel = opt == selected
            Text(
                text = (if (isSel) "> " else "  ") + opt.replace('_', ' ').uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) AmberBright else AmberFaint,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(opt) }
                    .background(if (isSel) RowHighlight else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}
