package com.evyr.rads.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.ui.theme.*
import kotlin.random.Random

data class TerminalButtonSpec(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * Full-bleed R.A.D.S. terminal housing: weathered sand shell with drawn
 * grain/grime, vent slats, corner screws, a scroll-position wheel on the
 * left, and illuminated hardware buttons along the bottom.
 */
@Composable
fun TerminalChrome(
    buttons: List<TerminalButtonSpec>,
    scrollProgress: Float = 0f,
    modifier: Modifier = Modifier,
    screenContent: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SandLight, SandMid, SandDeep)))
    ) {
        WeatheringLayer(Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {

            // ---- Top rail: screws + vent bank ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Screw()
                Spacer(Modifier.width(12.dp))
                VentBank(slats = 5, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Text(
                    "R.A.D.S.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SandShadow
                )
                Spacer(Modifier.width(12.dp))
                Screw()
            }

            // ---- Body: wheel | screen | vents ----
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                ScrollWheel(
                    progress = scrollProgress,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                )

                Spacer(Modifier.width(8.dp))

                // Bezel around the CRT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(BezelLight, BezelDark)))
                        .padding(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(ScreenBlack, ScreenBlackDeep),
                                    radius = 900f
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        screenContent()
                        Scanlines(Modifier.fillMaxSize())
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.fillMaxHeight().padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) { VentBank(slats = 4, vertical = true) }
                }
            }

            // ---- Bottom flap: hardware buttons ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(top = 6.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(SandDark, SandShadow)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    buttons.forEach { HardwareButton(it) }
                }
            }
        }
    }
}

/** Deterministic grain, scratches and rust blooms drawn over the shell. */
@Composable
private fun WeatheringLayer(modifier: Modifier = Modifier) {
    val rng = remember { Random(1847) }
    val specks = remember {
        List(420) {
            Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat())
        }
    }
    val scratches = remember {
        List(14) {
            listOf(rng.nextFloat(), rng.nextFloat(), rng.nextFloat(), rng.nextFloat())
        }
    }
    val blooms = remember {
        List(7) { Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) }
    }

    Canvas(modifier = modifier) {
        blooms.forEach { (bx, by, br) ->
            val r = size.minDimension * (0.08f + br * 0.14f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GrimeRust, Color.Transparent),
                    center = Offset(size.width * bx, size.height * by),
                    radius = r
                ),
                radius = r,
                center = Offset(size.width * bx, size.height * by)
            )
        }
        specks.forEach { (sx, sy, v) ->
            drawCircle(
                color = if (v > 0.5f) Color(0x22FFFFFF) else Color(0x33000000),
                radius = 0.6f + v * 1.4f,
                center = Offset(size.width * sx, size.height * sy)
            )
        }
        scratches.forEach { s ->
            drawLine(
                color = if (s[3] > 0.5f) Color(0x1AFFFFFF) else Color(0x26000000),
                start = Offset(size.width * s[0], size.height * s[1]),
                end = Offset(
                    size.width * (s[0] + (s[2] - 0.5f) * 0.18f),
                    size.height * (s[1] + (s[3] - 0.5f) * 0.22f)
                ),
                strokeWidth = 1f
            )
        }
    }
}

/** Faint horizontal CRT scanlines over the screen content. */
@Composable
private fun Scanlines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color(0x14000000),
                topLeft = Offset(0f, y),
                size = Size(size.width, 1.2f)
            )
            y += 3.4f
        }
    }
}

@Composable
private fun Screw(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(11.dp)
            .background(Brush.radialGradient(listOf(ScrewLight, ScrewDark)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(6.dp)
                .height(1.4.dp)
                .background(SandShadow)
        )
    }
}

@Composable
private fun VentBank(
    slats: Int,
    vertical: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (vertical) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(slats) {
                Box(
                    Modifier
                        .width(14.dp)
                        .height(3.dp)
                        .background(VentBlack, RoundedCornerShape(1.5.dp))
                )
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(slats) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(VentBlack, RoundedCornerShape(1.5.dp))
                )
            }
        }
    }
}

/** Ridged wheel whose ridges shift with the screen's scroll position. */
@Composable
private fun ScrollWheel(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.horizontalGradient(listOf(SandShadow, SandDark, SandShadow)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 11f
            val offset = (progress.coerceIn(0f, 1f) * spacing * 4f) % spacing
            var y = -spacing + offset
            while (y < size.height + spacing) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0x59FFFFFF), Color.Transparent)
                    ),
                    start = Offset(2f, y),
                    end = Offset(size.width - 2f, y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0x4D000000),
                    start = Offset(2f, y + 3f),
                    end = Offset(size.width - 2f, y + 3f),
                    strokeWidth = 2f
                )
                y += spacing
            }
        }
    }
}

@Composable
private fun HardwareButton(spec: TerminalButtonSpec) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = spec.onClick
        )
    ) {
        // Metal bezel ring
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    Brush.verticalGradient(listOf(SandHighlight, SandDark)),
                    CircleShape
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (spec.selected) {
                            Brush.radialGradient(
                                colors = listOf(AmberBright, Amber, Color(0xFF8A5A00)),
                                center = Offset(40f, 34f),
                                radius = 90f
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(ButtonUnlit, ButtonUnlitDeep),
                                center = Offset(40f, 34f),
                                radius = 90f
                            )
                        },
                        shape = CircleShape
                    )
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = spec.label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = if (spec.selected) FontWeight.Bold else FontWeight.Normal,
            color = if (spec.selected) AmberBright else Color(0xFF9C9280)
        )
    }
}
