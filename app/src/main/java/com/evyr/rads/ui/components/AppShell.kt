package com.evyr.rads.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ShellTab(val key: String, val label: String, val icon: ImageVector)

val SHELL_TABS = listOf(
    ShellTab("LOG", "Log", Icons.Filled.List),
    ShellTab("STATS", "Stats", Icons.Filled.DateRange),
    ShellTab("SYNC", "Sync", Icons.Filled.Refresh),
    ShellTab("SETUP", "Settings", Icons.Filled.Settings),
)

/**
 * Plain, light app shell: title bar, bottom navigation, and a pair of
 * actions for adding food. Replaces the old hardware-terminal chrome.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    title: String,
    subtitle: String?,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onAdd: () -> Unit,
    onScan: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        subtitle?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                SHELL_TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = tab.key == activeTab,
                        onClick = { onTabSelected(tab.key) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == "LOG") {
                Column {
                    FloatingActionButton(
                        onClick = onScan,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Scan or search")
                    }
                    FloatingActionButton(
                        onClick = onAdd,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add food")
                    }
                }
            }
        }
    ) { inner ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(inner),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}

/** Simple white card used to group sections on the light theme. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}
