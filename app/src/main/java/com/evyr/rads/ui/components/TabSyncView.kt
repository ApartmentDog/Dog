package com.evyr.rads.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.local.HealthSnapshot
import com.evyr.rads.health.HealthConnectManager
import com.evyr.rads.ui.TodayViewModel
import com.evyr.rads.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TabSyncView(
    scrollState: ScrollState,
    status: TodayViewModel.SyncStatus,
    availability: HealthConnectManager.Availability,
    health: HealthSnapshot?,
    imperial: Boolean,
    onSync: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onInstall: () -> Unit
) {
    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {

        SectionLabel("HEALTH CONNECT LINK")

        val (statusText, warn) = when {
            availability == HealthConnectManager.Availability.NOT_INSTALLED ->
                "NOT INSTALLED" to true
            availability == HealthConnectManager.Availability.UPDATE_REQUIRED ->
                "UPDATE REQUIRED" to true
            status == TodayViewModel.SyncStatus.IDLE -> "STANDBY" to false
            status == TodayViewModel.SyncStatus.SYNCING -> "READING..." to false
            status == TodayViewModel.SyncStatus.OK -> "LINK OK" to false
            status == TodayViewModel.SyncStatus.NO_PERMISSION -> "ACCESS DENIED" to true
            status == TodayViewModel.SyncStatus.ERROR -> "READ ERROR" to true
            else -> "STANDBY" to false
        }
        StatRow("STATUS", statusText, warn = warn, emphasize = true)
        StatRow(
            "LAST SYNC",
            health?.lastSyncedAt?.takeIf { it > 0 }?.let {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
            } ?: "NEVER"
        )

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("LAST READ VALUES")
        StatRow("STEPS", health?.steps?.toString() ?: "--")
        StatRow("EXERCISE", health?.exerciseMinutes?.let { "$it min" } ?: "-- min")
        StatRow(
            "WEIGHT",
            health?.weightKg?.let {
                "${com.evyr.rads.data.Units.displayWeight(it, imperial)} ${if (imperial) "lb" else "kg"}"
            } ?: "--"
        )

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("ACTIONS")

        when (availability) {
            HealthConnectManager.Availability.NOT_INSTALLED -> {
                TerminalAction("INSTALL HEALTH CONNECT", onInstall)
                Spacer(Modifier.height(4.dp))
                Note(
                    "Health Connect isn't on this device. Install it, open Samsung Health once so it writes data, then come back and grant access."
                )
            }
            HealthConnectManager.Availability.UPDATE_REQUIRED -> {
                TerminalAction("UPDATE HEALTH CONNECT", onInstall)
                Spacer(Modifier.height(4.dp))
                Note("Health Connect is installed but too old to talk to. Update it, then grant access.")
            }
            HealthConnectManager.Availability.READY -> {
                TerminalAction("PULL NOW", onSync)
                TerminalAction("GRANT ACCESS", onRequestPermission)
                TerminalAction("OPEN HEALTH CONNECT SETTINGS", onOpenSettings)
                Spacer(Modifier.height(6.dp))
                Note(
                    if (status == TodayViewModel.SyncStatus.NO_PERMISSION)
                        "Access was denied. Use [GRANT ACCESS] and allow Steps, Weight and Exercise. If no prompt appears, open settings and enable them there."
                    else
                        "Reads from Health Connect only. Samsung Health and the watch write into it; R.A.D.S. never talks to Samsung directly."
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = AmberFaint
    )
}
