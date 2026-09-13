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
import com.evyr.rads.data.local.UserProfile
import com.evyr.rads.ui.theme.*

@Composable
fun OnboardingScreen(onComplete: (UserProfile) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("unspecified") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("maintain") }
    var activity by remember { mutableStateOf("moderate") }
    var fatLimit by remember { mutableStateOf("15") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SandMid, SandDeep)))
            .padding(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(18.dp)
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
            Spacer(Modifier.height(14.dp))

            Text(
                "> INITIAL CONFIGURATION [${step + 1}/4]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AmberFaint
            )
            Spacer(Modifier.height(12.dp))

            when (step) {
                0 -> {
                    Prompt("IDENTIFY OPERATOR")
                    Field("NAME", name) { name = it }
                    Field("AGE", age, numeric = true) { age = it }
                    Choices("SEX", listOf("male", "female", "unspecified"), sex) { sex = it }
                }
                1 -> {
                    Prompt("BODY METRICS")
                    Field("HEIGHT cm", heightCm, numeric = true) { heightCm = it }
                    Field("WEIGHT kg", weightKg, numeric = true) { weightKg = it }
                }
                2 -> {
                    Prompt("PROTOCOL")
                    Choices("GOAL", listOf("lose", "maintain", "gain"), goal) { goal = it }
                    Choices(
                        "ACTIVITY",
                        listOf("sedentary", "light", "moderate", "active", "very_active"),
                        activity
                    ) { activity = it }
                }
                3 -> {
                    Prompt("FAT CEILING")
                    Text(
                        "Maximum fat grams for any single meal.\nThis is checked per meal, never as a daily total.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = AmberFaint,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Field("GRAMS/MEAL", fatLimit, numeric = true) { fatLimit = it }
                }
            }

            Spacer(Modifier.height(20.dp))
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
                    if (step < 3) "[NEXT]" else "[INITIALIZE]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberBright,
                    modifier = Modifier
                        .clickable {
                            if (step < 3) {
                                step++
                            } else {
                                onComplete(
                                    UserProfile(
                                        name = name.trim(),
                                        age = age.toIntOrNull() ?: 0,
                                        sex = sex,
                                        heightCm = heightCm.toDoubleOrNull() ?: 0.0,
                                        weightKg = weightKg.toDoubleOrNull() ?: 0.0,
                                        goal = goal,
                                        activityLevel = activity,
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
        modifier = Modifier.padding(bottom = 10.dp)
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
            modifier = Modifier.width(100.dp).padding(top = 5.dp)
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
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AmberDim)
        Spacer(Modifier.height(4.dp))
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
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            )
        }
    }
}
