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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.evyr.rads.data.ScannedFood

@Composable
fun ScanSourceDialog(
    mealSlot: String,
    aiEnabled: Boolean,
    onSearch: () -> Unit,
    onBarcode: () -> Unit,
    onTypeBarcode: () -> Unit,
    onCameraPhoto: () -> Unit,
    onGalleryPhoto: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                "Log to ${mealSlot.replaceFirstChar { it.uppercase() }}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))

            Option("Search database", "USDA + Open Food Facts.", onSearch)
            Option("Scan barcode", "Packaged food. Works offline.", onBarcode)
            Option("Type barcode", "Enter the digits by hand.", onTypeBarcode)
            Option(
                "Photo — camera",
                if (aiEnabled) "Plate or menu. Uses AI estimate."
                else "Needs a Gemini key in Setup.",
                onCameraPhoto,
                enabled = aiEnabled
            )
            Option(
                "Photo — gallery",
                if (aiEnabled) "Pick an existing photo."
                else "Needs a Gemini key in Setup.",
                onGalleryPhoto,
                enabled = aiEnabled
            )
            Option("Enter manually", "Type the numbers yourself.", onManual)

            Spacer(Modifier.height(10.dp))
            Text(
                "Cancel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onDismiss() }.padding(6.dp)
            )
        }
    }
}

@Composable
private fun Option(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) onSurface else onSurface.copy(alpha = 0.4f)
        )
        Text(
            subtitle,
            fontSize = 12.sp,
            color = onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** When vision returns several dishes, pick which one to assess. */
@Composable
fun ScanResultPicker(
    foods: List<ScannedFood>,
    onPick: (ScannedFood) -> Unit,
    onDismiss: () -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                "${foods.size} items detected",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Spacer(Modifier.height(10.dp))

            foods.forEach { f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(f) }
                        .padding(vertical = 9.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            f.name,
                            fontSize = 14.sp,
                            color = onSurface
                        )
                        Text(
                            "${f.calories} kcal / ${f.fatGrams}g fat" +
                                (f.confidence?.let { "  ($it)" } ?: ""),
                            fontSize = 12.sp,
                            color = onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
                Hairline()
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Cancel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onDismiss() }.padding(6.dp)
            )
        }
    }
}

@Composable
fun ScanStatusDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Close",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
            )
        }
    }
}


/** Manual barcode entry for when the scanner won't cooperate. */
@Composable
fun BarcodeEntryDialog(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val valid = code.trim().length in 6..14 && code.trim().all { it.isDigit() }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                "Enter barcode",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(
                "The digits printed under the bars. UPC-A is 12, EAN-13 is 13.",
                fontSize = 11.sp,
                color = onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            BasicTextField(
                value = code,
                onValueChange = { input -> code = input.filter { it.isDigit() }.take(14) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    color = onSurface
                ),
                cursorBrush = SolidColor(primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )

            Text(
                "${code.length} digits",
                fontSize = 11.sp,
                color = if (valid) primary else onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
                )
                Spacer(Modifier.width(24.dp))
                Text(
                    "Look up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (valid) primary else onSurface.copy(alpha = 0.35f),
                    modifier = Modifier
                        .clickable(enabled = valid) { onSubmit(code.trim()) }
                        .padding(4.dp)
                )
            }
        }
    }
}

