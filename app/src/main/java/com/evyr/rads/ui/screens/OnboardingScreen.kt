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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.Allergen
import com.evyr.rads.data.Condition
import com.evyr.rads.data.CustomAllergen
import com.evyr.rads.data.Units
import com.evyr.rads.data.local.DEFAULT_FAT_WARN_GRAMS
import com.evyr.rads.data.local.UserProfile

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
    var conditions by remember { mutableStateOf(emptySet<Condition>()) }
    var allergens by remember { mutableStateOf(emptySet<Allergen>()) }
    var customAllergensText by remember { mutableStateOf("") }

    val lastStep = 6
    val onSurface = MaterialTheme.colorScheme.onSurface
    val dim = onSurface.copy(alpha = 0.6f)
    val faint = onSurface.copy(alpha = 0.45f)
    val primary = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "R.A.D.S.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(
                "Let's get you set up.",
                fontSize = 14.sp,
                color = dim
            )
            Spacer(Modifier.height(14.dp))

            // Step progress bar
            Row(Modifier.fillMaxWidth()) {
                for (i in 0..lastStep) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (i <= step) primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    if (i != lastStep) Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(20.dp))

            when (step) {
                0 -> {
                    Prompt("Units")
                    Choices("Measurement", listOf("imperial", "metric"),
                        if (imperial) "imperial" else "metric") {
                        imperial = it == "imperial"
                    }
                    Note(
                        if (imperial) "Pounds and feet/inches."
                        else "Kilograms and centimeters."
                    )
                }
                1 -> {
                    Prompt("First, a little about you")
                    Note("Just a few things so we can build a calorie goal that fits your life.")
                    Spacer(Modifier.height(10.dp))
                    Field("Name", name) { name = it }
                    Field("Age", age, numeric = true) { age = it }
                    Choices("Sex", listOf("male", "female", "unspecified"), sex) { sex = it }
                }
                2 -> {
                    Prompt("Body")
                    if (imperial) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f)) {
                                Field("Height ft", feet, numeric = true) { feet = it }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                Field("in", inches, numeric = true) { inches = it }
                            }
                        }
                        Field("Weight lb", weightText, numeric = true) { weightText = it }
                    } else {
                        Field("Height cm", heightCmText, numeric = true) { heightCmText = it }
                        Field("Weight kg", weightText, numeric = true) { weightText = it }
                    }
                }
                3 -> {
                    Prompt("What's your goal?")
                    Note("There's no wrong answer. Slower tends to stick — but you know you best.")
                    Spacer(Modifier.height(10.dp))
                    Choices("Goal", listOf("lose", "maintain", "gain"), goal) { goal = it }
                    if (goal != "maintain") {
                        Field(
                            "Target ${if (imperial) "lb" else "kg"}",
                            goalWeightText,
                            numeric = true
                        ) { goalWeightText = it }
                        Choices(
                            "Rate (lb/week)",
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
                        "Activity",
                        listOf("sedentary", "light", "moderate", "active", "very_active"),
                        activity
                    ) { activity = it }
                }
                4 -> {
                    Prompt("Fat ceiling")
                    Note("Maximum fat grams in any single meal. Checked per meal, never as a daily total.")
                    Spacer(Modifier.height(8.dp))
                    Field("Grams / meal", fatLimit, numeric = true) { fatLimit = it }
                }
                5 -> {
                    Prompt("Health conditions")
                    Note(
                        "Optional. Tick any that apply and foods get flagged for them before you log. " +
                            "General guidance only — your doctor's instructions override these. " +
                            "You can change this later in Setup."
                    )
                    Spacer(Modifier.height(10.dp))
                    Condition.values().forEach { c ->
                        val on = c in conditions
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { conditions = if (on) conditions - c else conditions + c }
                                .background(
                                    if (on) primary.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                c.label,
                                fontSize = 14.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                color = if (on) primary else onSurface
                            )
                            Text(
                                "Watches: " + c.watches,
                                fontSize = 12.sp,
                                color = faint,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                6 -> {
                    Prompt("Allergies")
                    Note(
                        "Tick any that apply. A match in a food's name or ingredients gets " +
                            "flagged before you log it. This is not a substitute for reading labels " +
                            "or checking with a restaurant -- it catches what's written in the food's " +
                            "name, not what touched it in the kitchen. General guidance only -- always " +
                            "follow your own or your doctor's judgment. You can change this later in Setup."
                    )
                    Spacer(Modifier.height(10.dp))
                    Allergen.values().forEach { a ->
                        val on = a in allergens
                        Text(
                            a.label,
                            fontSize = 14.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = if (on) primary else onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { allergens = if (on) allergens - a else allergens + a }
                                .background(
                                    if (on) primary.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Note("Other allergies (comma-separated)")
                    Field("e.g. mango, MSG", customAllergensText) { customAllergensText = it }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth()) {
                if (step > 0) {
                    Text(
                        "Back",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = dim,
                        modifier = Modifier.clickable { step-- }.padding(8.dp)
                    )
                    Spacer(Modifier.width(18.dp))
                }
                Text(
                    if (step < lastStep) "Next" else "Get started",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary,
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
                                        fatWarnGramsPerMeal = fatLimit.toDoubleOrNull() ?: DEFAULT_FAT_WARN_GRAMS,
                                        conditions = Condition.toCsv(conditions),
                                        allergens = Allergen.toCsv(allergens),
                                        customAllergens = CustomAllergen.toCsv(
                                            customAllergensText.split(",").map { it.trim() }
                                                .filter { it.isNotBlank() }.map { CustomAllergen(it) }
                                        ),
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
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(96.dp).padding(top = 8.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else KeyboardOptions.Default,
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp)
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
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(5.dp))
        options.forEach { opt ->
            val isSel = opt == selected
            Text(
                text = opt.replace('_', ' ').replaceFirstChar { it.uppercase() },
                fontSize = 14.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) primary else onSurface.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(opt) }
                    .background(
                        if (isSel) primary.copy(alpha = 0.12f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}
