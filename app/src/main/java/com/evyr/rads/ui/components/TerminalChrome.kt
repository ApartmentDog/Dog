package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sandy/dusty wasteland terminal palette
private val SandLight = Color(0xFF8A7D68)
private val SandMid = Color(0xFF5C5040)
private val SandDark = Color(0xFF3D3428)
private val SandPanel = Color(0xFF6B5F4C)
private val SandPanelDark = Color(0xFF3A3226)
private val ScrewLight = Color(0xFFD8C8A8)
private val ScrewDark = Color(0xFF6A5C42)
private val VentColor = Color(0xFF241F16)
private val AmberGlow = Color(0xFFFFB000)

data class TerminalButtonSpec(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * Wraps arbitrary screen content in the R.A.D.S. terminal housing:
 * sandy weathered metal shell, corner screws, vent slats, a decorative
 * scroll-wheel indicator on the left, and a row of physical-style buttons
 * along the bottom (only the selected one glows).
 */
@Composable
fun TerminalChrome(
    buttons: List<TerminalButtonSpec>,
    modifier: Modifier = Modifier,
    screenContent: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(SandLight, SandMid, SandDark)),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 34.dp, vertical = 16.dp)
    ) {
        CornerScrew(Modifier.align(Alignment.TopStart).padding(10.dp))
        CornerScrew(Modifier.align(Alignment.TopEnd).padding(10.dp))
        CornerScrew(Modifier.align(Alignment.BottomStart).padding(10.dp))
        CornerScrew(Modifier.align(Alignment.BottomEnd).padding(10.dp))

        ScrollWheel(Modifier.align(Alignment.CenterStart))
        VentColumn(Modifier.align(Alignment.CenterEnd))

        Column(modifier = Modifier.fillMaxWidth()) {
            // Screen panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SandPanel, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(Color(0xFF0A0800), RoundedCornerShape(6.dp))
                        .padding(14.dp)
                ) {
                    screenContent()
                }
            }

            // Button flap
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SandPanelDark, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                buttons.forEach { spec ->
                    TerminalButton(spec)
                }
            }
        }
    }
}

@Composable
private fun CornerScrew(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(9.dp)
            .height(9.dp)
            .background(
                Brush.radialGradient(listOf(ScrewLight, ScrewDark)),
                CircleShape
            )
    )
}

@Composable
private fun ScrollWheel(modifier: Modifier = Modifier) {
    // Static decorative wheel for now; wire to LazyList scroll state later.
    Box(
        modifier = modifier
            .width(20.dp)
            .height(96.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFF4A3F2E), Color(0xFF241F16))),
                RoundedCornerShape(10.dp)
            )
    )
}

@Composable
private fun VentColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(220.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            VentSlats()
        }
    }
}

@Composable
private fun VentSlats() {
    Column {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .background(VentColor, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun TerminalButton(spec: TerminalButtonSpec) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = spec.onClick)
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(38.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFA89878), SandPanelDark, Color(0xFF6B5F4C))),
                    CircleShape
                )
                .padding(3.dp)
                .background(
                    brush = if (spec.selected) {
                        Brush.radialGradient(listOf(Color(0xFFFFE08A), AmberGlow, Color(0xFF7A4D00)))
                    } else {
                        Brush.radialGradient(listOf(Color(0xFF7A6D58), Color(0xFF453D2E), Color(0xFF2A251A)))
                    },
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = spec.label,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = if (spec.selected) Color(0xFFE8C878) else Color(0xFF9C9280),
            textAlign = TextAlign.Center
        )
    }
}
