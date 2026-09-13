package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
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
fun TabSetupView(
    profile: UserProfile?,
    onUpdate: (UserProfile) -> Unit
) {
    val p = profile ?: UserProfile()

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        SectionLabel("OPERATOR")
        EditField("NAME", p.name) { onUpdate(p.copy(name = it)) }
        EditNumber("AGE", p.age.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(p.copy(age = it)) }
        }
        ChoiceRow(
            "SEX",
            listOf("male", "female", "unspecified"),
            p.sex
        ) { onUpdate(p.copy(sex = it)) }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("BODY")
        EditNumber("HEIGHT cm", trim(p.heightCm)) { v ->
            v.toDoubleOrNull()?.let { onUpdate(p.copy(heightCm = it)) }
        }
        EditNumber("WEIGHT kg", trim(p.weightKg)) { v ->
            v.toDoubleOrNull()?.let { onUpdate(p.copy(weightKg = it)) }
        }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("PROTOCOL")
        ChoiceRow("GOAL", listOf("lose", "maintain", "gain"), p.goal) {
            onUpdate(p.copy(goal = it))
        }
        ChoiceRow(
            "ACTIVITY",
            listOf("sedentary", "light", "moderate", "active", "very_active"),
            p.activityLevel
        ) { onUpdate(p.copy(activityLevel = it)) }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("FAT LIMIT (PER MEAL)")
        Text(
            "Applied to each meal separately. Not a daily budget.",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = AmberFaint,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        EditNumber("GRAMS", trim(p.fatWarnGramsPerMeal)) { v ->
            v.toDoubleOrNull()?.let { onUpdate(p.copy(fatWarnGramsPerMeal = it)) }
        }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("COMPUTED")
        StatRow("BMR", p.bmr().toInt().toString())
        StatRow("TDEE", p.tdee().toInt().toString())
        StatRow("TARGET", "${p.calorieTarget()} kcal", emphasize = true)

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    FieldShell(label) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
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

@Composable
private fun EditNumber(label: String, value: String, onChange: (String) -> Unit) {
    FieldShell(label) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

@Composable
private fun FieldShell(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberDim,
            modifier = Modifier.width(92.dp).padding(top = 5.dp)
        )
        Column(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberDim
        )
        Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
            options.forEach { opt ->
                val isSel = opt == selected
                Text(
                    text = opt.replace('_', ' ').uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) AmberBright else AmberFaint,
                    modifier = Modifier
                        .clickable { onSelect(opt) }
                        .background(if (isSel) RowHighlight else Color.Transparent)
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(3.dp))
            }
        }
    }
}
