package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.AbiPref
import com.openmojang.launcher.data.MirrorPref
import com.openmojang.launcher.data.MusicPref
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var autoUpdate by remember { mutableStateOf(store.autoCheckUpdates) }
    var keepOld by remember { mutableStateOf(store.keepOldApks) }
    var installAfter by remember { mutableStateOf(store.installAfterDownload) }
    var wifiOnly by remember { mutableStateOf(store.wifiOnly) }
    var mirror by remember { mutableStateOf(store.preferredMirror) }
    var abi by remember { mutableStateOf(store.preferredAbi) }
    var music by remember { mutableStateOf(store.preferredMusic) }
    var theme by remember { mutableStateOf(store.themeMode) }
    var cacheLabel by remember { mutableStateOf(formatCache(store.cacheBytes())) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Downloads", style = MaterialTheme.typography.titleSmall)
                    SwitchRow("Auto-check updates", autoUpdate) {
                        autoUpdate = it
                        store.autoCheckUpdates = it
                    }
                    SwitchRow("Keep old APKs", keepOld) {
                        keepOld = it
                        store.keepOldApks = it
                    }
                    SwitchRow("Install after download", installAfter) {
                        installAfter = it
                        store.installAfterDownload = it
                    }
                    SwitchRow("Wi‑Fi only", wifiOnly) {
                        wifiOnly = it
                        store.wifiOnly = it
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mirror", style = MaterialTheme.typography.titleSmall)
                    MirrorPref.entries.forEach { option ->
                        RadioRow(
                            label = when (option) {
                                MirrorPref.AUTO -> "Auto"
                                MirrorPref.MCPEDL -> "MCPEDL"
                                MirrorPref.MCPE_PLANET -> "MCPE-Planet"
                            },
                            selected = mirror == option,
                            onClick = {
                                mirror = option
                                store.preferredMirror = option
                            }
                        )
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ABI", style = MaterialTheme.typography.titleSmall)
                    AbiPref.entries.forEach { option ->
                        RadioRow(
                            label = when (option) {
                                AbiPref.AUTO -> "Auto (prefer arm64)"
                                AbiPref.ARM64 -> "arm64-v8a only"
                                AbiPref.ARM32 -> "armeabi-v7a only"
                            },
                            selected = abi == option,
                            onClick = {
                                abi = option
                                store.preferredAbi = option
                            }
                        )
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Music package", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Mirrors ship two APKs — full (with music) or slim (no music). " +
                            "After changing this, open the version and tap Redownload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MusicPref.entries.forEach { option ->
                        RadioRow(
                            label = when (option) {
                                MusicPref.NO_MUSIC -> "No music (smaller)"
                                MusicPref.WITH_MUSIC -> "With music (full)"
                                MusicPref.AUTO -> "Auto"
                            },
                            selected = music == option,
                            onClick = {
                                music = option
                                store.preferredMusic = option
                            }
                        )
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    ThemeMode.entries.forEach { option ->
                        RadioRow(
                            label = when (option) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            selected = theme == option,
                            onClick = {
                                theme = option
                                store.themeMode = option
                            }
                        )
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cache", style = MaterialTheme.typography.titleSmall)
                    Text(cacheLabel, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = {
                            val n = store.clearCache()
                            cacheLabel = formatCache(store.cacheBytes())
                            message = "Cleared $n file(s)"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear download cache") }
                }
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Text(
                "Maple Launcher 0.1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun formatCache(bytes: Long): String {
    if (bytes <= 0L) return "0 MB cached"
    return "${bytes / (1024 * 1024)} MB cached"
}
