package com.evyr.rads.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evyr.rads.data.local.HealthSnapshot
import com.evyr.rads.ui.TodayViewModel
import com.evyr.rads.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TabSyncView(
    status: TodayViewModel.SyncStatus,
    health: HealthSnapshot?,
    onSync: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        SectionLabel("HEALTH CONNECT LINK")

        val (statusText, warn) = when (status) {
            TodayViewModel.SyncStatus.IDLE -> "STANDBY" to false
            TodayViewModel.SyncStatus.SYNCING -> "READING..." to false
            TodayViewModel.SyncStatus.OK -> "LINK OK" to false
            TodayViewModel.SyncStatus.UNAVAILABLE -> "NOT INSTALLED" to true
            TodayViewModel.SyncStatus.NO_PERMISSION -> "PERMISSION DENIED" to true
            TodayViewModel.SyncStatus.ERROR -> "READ ERROR" to true
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
        StatRow("WEIGHT", health?.weightKg?.let { "${trim(it)} kg" } ?: "-- kg")

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("ACTIONS")
        TerminalAction("[PULL NOW]", onSync)
        TerminalAction("[GRANT PERMISSIONS]", onRequestPermission)

        Spacer(Modifier.height(8.dp))
        Text(
            when (status) {
                TodayViewModel.SyncStatus.UNAVAILABLE ->
                    "Health Connect is not available on this device. Install it from the Play Store, then grant access."
                TodayViewModel.SyncStatus.NO_PERMISSION ->
                    "Access was denied. Use [GRANT PERMISSIONS] and allow steps, weight and exercise."
                else ->
                    "Reads from Health Connect only. Samsung Health and the watch write into Health Connect; R.A.D.S. never talks to Samsung directly."
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberFaint
        )
        Spacer(Modifier.height(10.dp))
    }
}
