package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.DownloadEngine
import com.openmojang.launcher.data.InstalledBuild
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.SettingsStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val installed = remember { InstalledVersions(context) }
    val engine = remember { DownloadEngine(context) }
    val settings = remember { SettingsStore(context) }

    var tracked by remember { mutableStateOf(installed.listTracked()) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        engine.purgeIncomplete()
        tracked = installed.listTracked().filter { b ->
            val f = File(b.apkPath)
            if (b.apkPath.isNotEmpty() && f.exists()) true
            else {
                // drop dead index entries
                if (b.apkPath.isNotEmpty()) installed.untrack(b.id)
                false
            }
        }.also {
            // re-read after untrack
        }
        tracked = installed.listTracked().filter {
            it.apkPath.isEmpty() || File(it.apkPath).exists()
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Library") })
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ListItem(
                headlineContent = { Text("Saved APKs") },
                supportingContent = {
                    val bytes = settings.cacheBytes()
                    val mb = if (bytes <= 0) "0 MB" else "${bytes / (1024 * 1024)} MB on disk"
                    Text("$mb · incomplete files are removed")
                },
                leadingContent = {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null)
                }
            )
            HorizontalDivider()

            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (tracked.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No completed downloads yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Finished APKs show up here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracked, key = { it.id }) { build ->
                        LibraryRow(
                            build = build,
                            onInstall = {
                                try {
                                    val f = File(build.apkPath)
                                    engine.installApk(f)
                                    message = "Install started for ${build.name}"
                                } catch (e: Exception) {
                                    message = e.message
                                    refresh()
                                }
                            },
                            onDelete = {
                                val f = File(build.apkPath)
                                if (f.exists()) f.delete()
                                installed.untrack(build.id)
                                refresh()
                                message = "Removed ${build.name}"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    build: InstalledBuild,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    val file = File(build.apkPath)
    val sizeMb = if (file.exists()) "%.0f".format(file.length() / (1024.0 * 1024.0)) else "—"
    val whenStr = if (build.installedAt > 0) {
        SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(build.installedAt))
    } else ""

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(build.name, style = MaterialTheme.typography.titleMedium)
            Text(
                listOfNotNull(
                    "${sizeMb} MB",
                    whenStr.ifEmpty { null }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onInstall,
                    enabled = file.exists(),
                    modifier = Modifier.weight(1f)
                ) { Text("Install") }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
