package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.ui.theme.*

/** Shared bits used by every terminal tab. */

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AmberHairline)
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = AmberFaint,
        modifier = modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun StatRow(
    label: String,
    value: String,
    warn: Boolean = false,
    emphasize: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = AmberDim,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = if (emphasize) 13.sp else 10.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (warn) AmberWarn else Amber
        )
    }
}

/** Horizontal bar meter, e.g. calories consumed vs target. */
@Composable
fun BarMeter(
    label: String,
    current: Double,
    target: Double,
    unit: String,
    warn: Boolean = false
) {
    val pct = if (target > 0) (current / target).coerceIn(0.0, 1.0) else 0.0
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = AmberDim,
            modifier = Modifier.width(70.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(ScreenInk)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct.toFloat())
                    .height(10.dp)
                    .background(if (warn) AmberWarn else Amber)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${current.toInt()}/${target.toInt()}$unit",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = if (warn) AmberWarn else Amber
        )
    }
}

@Composable
fun TerminalTabBar(
    tabs: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        tabs.forEach { tab ->
            val isSel = tab == selected
            Text(
                text = tab,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) AmberBright else AmberFaint,
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .background(if (isSel) RowHighlight else Color.Transparent)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
fun TerminalAction(text: String, onClick: () -> Unit, warn: Boolean = false) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = if (warn) AmberWarn else AmberBright,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
    )
}
