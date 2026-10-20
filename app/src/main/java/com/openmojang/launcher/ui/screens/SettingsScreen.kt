package com.openmojang.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.AbiPref
import com.openmojang.launcher.data.MirrorPref
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var autoUpdate by remember { mutableStateOf(store.autoCheckUpdates) }
    var keepOld by remember { mutableStateOf(store.keepOldVersions) }
    var installAfter by remember { mutableStateOf(store.installAfterDownload) }
    var wifiOnly by remember { mutableStateOf(store.wifiOnlyDownloads) }
    var mirror by remember { mutableStateOf(store.preferredMirror) }
    var abi by remember { mutableStateOf(store.preferredAbi) }
    var theme by remember { mutableStateOf(store.themeMode) }
    var cacheLabel by remember { mutableStateOf(formatBytes(store.cacheBytes())) }
    var clearMsg by remember { mutableStateOf<String?>(null) }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Downloads")

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mirror", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Where APKs are resolved from",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    MirrorPref.entries.forEach { option ->
                        RadioRow(
                            label = when (option) {
                                MirrorPref.AUTO -> "Auto (try all)"
                                MirrorPref.MCPEDL -> "MCPEDL only"
                                MirrorPref.MCPE_PLANET -> "MCPE-Planet only"
                            },
                            selected = mirror == option,
                            onSelect = {
                                mirror = option
                                store.preferredMirror = option
                            }
                        )
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Architecture", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "arm64 is correct for almost every modern phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    AbiPref.entries.forEach { option ->
                        RadioRow(
                            label = when (option) {
                                AbiPref.AUTO -> "Auto (prefer arm64)"
                                AbiPref.ARM64 -> "arm64-v8a only"
                                AbiPref.ARM32 -> "armeabi-v7a only"
                            },
                            selected = abi == option,
                            onSelect = {
                                abi = option
                                store.preferredAbi = option
                            }
                        )
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    SwitchRow(
                        title = "Auto-check updates",
                        subtitle = "Compare catalog to installed Minecraft on open",
                        checked = autoUpdate,
                        onCheckedChange = {
                            autoUpdate = it
                            store.autoCheckUpdates = it
                        }
                    )
                    HorizontalDivider()
                    SwitchRow(
                        title = "Keep old APKs",
                        subtitle = "Leave downloaded files after install",
                        checked = keepOld,
                        onCheckedChange = {
                            keepOld = it
                            store.keepOldVersions = it
                        }
                    )
                    HorizontalDivider()
                    SwitchRow(
                        title = "Install after download",
                        subtitle = "Open the system install sheet automatically",
                        checked = installAfter,
                        onCheckedChange = {
                            installAfter = it
                            store.installAfterDownload = it
                        }
                    )
                    HorizontalDivider()
                    SwitchRow(
                        title = "Wi‑Fi only",
                        subtitle = "Block downloads on mobile data",
                        checked = wifiOnly,
                        onCheckedChange = {
                            wifiOnly = it
                            store.wifiOnlyDownloads = it
                        }
                    )
                }
            }

            SectionHeader("Appearance")

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    ThemeMode.entries.forEach { option ->
                        RadioRow(
                            label = when (option) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            selected = theme == option,
                            onSelect = {
                                theme = option
                                store.themeMode = option
                            }
                        )
                    }
                    Text(
                        "Theme applies on next app open.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            SectionHeader("Storage")

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Downloaded APKs", style = MaterialTheme.typography.titleSmall)
                            Text(
                                cacheLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val n = store.clearCache()
                            cacheLabel = formatBytes(store.cacheBytes())
                            clearMsg = if (n == 0) "Nothing to clear" else "Removed $n file(s)"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear download cache")
                    }
                    clearMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            SectionHeader("About")

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Peak Launcher", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "v0.1.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "Unofficial Bedrock launcher. Not affiliated with Mojang or Microsoft.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/certier/open-mojang")
                                )
                            )
                        }
                    ) { Text("GitHub repository") }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) "${(mb / 1024.0).let { "%.1f".format(it) }} GB"
    else "${mb.roundToInt()} MB"
}
