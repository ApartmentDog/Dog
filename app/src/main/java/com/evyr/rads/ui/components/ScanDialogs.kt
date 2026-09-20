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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.evyr.rads.data.ScannedFood
import com.evyr.rads.ui.theme.*

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
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(18.dp)
        ) {
            Text(
                "> IDENTIFY ITEM / ${mealSlot.uppercase()}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            Spacer(Modifier.height(12.dp))

            Option("SEARCH DATABASE", "USDA + Open Food Facts.", onSearch)
            Option("SCAN BARCODE", "Packaged food. Works offline.", onBarcode)
            Option("TYPE BARCODE", "Enter the digits by hand.", onTypeBarcode)
            Option(
                "PHOTO — CAMERA",
                if (aiEnabled) "Plate or menu. Uses AI estimate."
                else "Needs a Gemini key in SETUP.",
                onCameraPhoto,
                enabled = aiEnabled
            )
            Option(
                "PHOTO — GALLERY",
                if (aiEnabled) "Pick an existing photo."
                else "Needs a Gemini key in SETUP.",
                onGalleryPhoto,
                enabled = aiEnabled
            )
            Option("ENTER MANUALLY", "Type the numbers yourself.", onManual)

            Spacer(Modifier.height(10.dp))
            Text(
                "[CANCEL]",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AmberDim,
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
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            "> $title",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) AmberBright else AmberFaint
        )
        Text(
            subtitle,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberFaint,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
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
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(18.dp)
        ) {
            Text(
                "> ${foods.size} ITEMS DETECTED",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            Spacer(Modifier.height(10.dp))

            foods.forEach { f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(f) }
                        .padding(vertical = 7.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "> ${f.name}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = AmberBright
                        )
                        Text(
                            "${f.calories} kcal / ${f.fatGrams}g fat" +
                                (f.confidence?.let { "  [$it]" } ?: ""),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = AmberFaint,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
                Hairline()
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "[CANCEL]",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AmberDim,
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
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(20.dp)
        ) {
            Text(
                message,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Amber
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "[CLOSE]",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AmberDim,
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

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ScreenBlack, RoundedCornerShape(8.dp))
                .padding(18.dp)
        ) {
            Text(
                "> ENTER BARCODE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            Text(
                "The digits printed under the bars. UPC-A is 12, EAN-13 is 13.",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = AmberFaint,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
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
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    color = Amber
                ),
                cursorBrush = SolidColor(Amber),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenInk, RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )

            Text(
                "${code.length} DIGITS",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = if (valid) AmberBright else AmberFaint,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "[CANCEL]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = AmberDim,
                    modifier = Modifier.clickable { onDismiss() }.padding(4.dp)
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    "[LOOK UP]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (valid) AmberBright else AmberFaint,
                    modifier = Modifier
                        .clickable(enabled = valid) { onSubmit(code.trim()) }
                        .padding(4.dp)
                )
            }
        }
    }
}
