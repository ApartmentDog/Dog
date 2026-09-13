package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
