package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var autoUpdate by remember { mutableStateOf(store.autoCheckUpdates) }
    var keepOld by remember { mutableStateOf(store.keepOldVersions) }

    Scaffold(
        containerColor = DirtDark,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceContainer,
                    titleContentColor = TextLight,
                    navigationIconContentColor = AccentGold
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "DOWNLOADS",
                style = MaterialTheme.typography.labelMedium,
                color = AccentGold
            )

            SettingsCard {
                Text("Source", style = MaterialTheme.typography.titleMedium, color = TextLight)
                Text(
                    "mcpe-planet / mcpedl (auto)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            SettingsCard {
                SettingRow(
                    title = "Auto-check updates",
                    subtitle = if (autoUpdate) "On" else "Off",
                    checked = autoUpdate,
                    onCheckedChange = {
                        autoUpdate = it
                        store.autoCheckUpdates = it
                    }
                )
            }

            SettingsCard {
                SettingRow(
                    title = "Keep old versions",
                    subtitle = if (keepOld) "Yes" else "No",
                    checked = keepOld,
                    onCheckedChange = {
                        keepOld = it
                        store.keepOldVersions = it
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "ABOUT",
                style = MaterialTheme.typography.labelMedium,
                color = AccentGold
            )

            SettingsCard {
                Text("Open Mojang", style = MaterialTheme.typography.titleMedium, color = TextLight)
                Text("v0.1.0", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Not affiliated with Mojang or Microsoft.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextLight)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (checked) GrassLight else TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextLight,
                checkedTrackColor = GrassGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = StoneGray
            )
        )
    }
}
