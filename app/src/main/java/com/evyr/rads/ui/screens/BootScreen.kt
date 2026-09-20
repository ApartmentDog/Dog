package com.evyr.rads.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.ui.theme.Amber
import com.evyr.rads.ui.theme.AmberBright
import com.evyr.rads.ui.theme.AmberDim
import com.evyr.rads.ui.theme.AmberFaint
import com.evyr.rads.ui.theme.ScreenBlack
import kotlinx.coroutines.delay

private data class BootLine(val text: String, val delayMs: Long, val bright: Boolean = false)

private val SEQUENCE = listOf(
    BootLine("R.A.D.S. TERMINAL", 0, bright = true),
    BootLine("RATION ASSESSMENT & DIET SYSTEM", 90),
    BootLine("", 60),
    BootLine("MEM CHECK .......... OK", 220),
    BootLine("LOCAL STORE ........ MOUNTED", 200),
    BootLine("HEALTH LINK ........ POLLING", 200),
    BootLine("OPERATOR PROFILE ... LOADED", 200),
    BootLine("", 80),
    BootLine("> SYSTEM READY", 260, bright = true)
)

/**
 * Short cold-boot animation. Purely cosmetic, skippable by tapping,
 * and switched off entirely from SETUP.
 */
@Composable
fun BootScreen(onFinished: () -> Unit) {
    var visibleCount by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SEQUENCE.forEachIndexed { index, line ->
            delay(line.delayMs)
            visibleCount = index + 1
        }
        delay(420)
        done = true
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!done) {
                    done = true
                    onFinished()
                }
            }
            .padding(24.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            SEQUENCE.take(visibleCount).forEach { line ->
                if (line.text.isBlank()) {
                    Spacer(Modifier.height(10.dp))
                } else {
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (line.bright) 14.sp else 11.sp,
                        fontWeight = if (line.bright) FontWeight.Bold else FontWeight.Normal,
                        color = if (line.bright) AmberBright else Amber,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            if (visibleCount in 1 until SEQUENCE.size) {
                Text(
                    "_",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = AmberDim
                )
            }
        }

        Text(
            "TAP TO SKIP",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = AmberFaint,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}
