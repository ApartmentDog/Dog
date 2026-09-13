package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.evyr.rads.ui.theme.*


@Composable
fun AddEntryDialog(
    mealSlot: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Int, fat: Double, protein: Double, carbs: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenBlack, RoundedCornerShape(6.dp))
                .padding(16.dp)
        ) {
            Text(
                "> NEW ENTRY / ${mealSlot.uppercase()}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Amber
            )
            Spacer(Modifier.height(12.dp))

            TerminalField("NAME", name) { name = it }
            TerminalField("CAL", calories, numeric = true) { calories = it }
            TerminalField("FAT g", fat, numeric = true) { fat = it }
            TerminalField("PROTEIN g", protein, numeric = true) { protein = it }
            TerminalField("CARBS g", carbs, numeric = true) { carbs = it }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "[CANCEL]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = AmberDim,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Spacer(Modifier.width(24.dp))
                Text(
                    "[SAVE]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Amber,
                    modifier = Modifier.clickable {
                        if (name.isNotBlank()) {
                            onConfirm(
                                name.trim(),
                                calories.toIntOrNull() ?: 0,
                                fat.toDoubleOrNull() ?: 0.0,
                                protein.toDoubleOrNull() ?: 0.0,
                                carbs.toDoubleOrNull() ?: 0.0
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TerminalField(
    label: String,
    value: String,
    numeric: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = AmberDim,
            modifier = Modifier.width(90.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Amber
            ),
            cursorBrush = SolidColor(Amber),
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenInk, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
