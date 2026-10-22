package com.openmojang.launcher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.McPack
import com.openmojang.launcher.data.ModManager

@Composable
fun ModsScreen() {
    val context = LocalContext.current
    val manager = remember { ModManager(context) }
    var packs by remember { mutableStateOf<List<McPack>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var hasAccess by remember { mutableStateOf(manager.hasAccess()) }

    fun reload() {
        hasAccess = manager.hasAccess()
        packs = manager.listPacks()
        message = if (!hasAccess) {
            "Grant All files access to manage packs"
        } else {
            "${packs.size} packs · ${packs.count { it.enabled }} enabled"
        }
    }

    val pickPack = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        message = manager.importPack(uri)
        reload()
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Mods & packs", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Resource and behavior packs under games/com.mojang. Enable, disable, import .mcpack/.zip.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hasAccess) "All files access · ON" else "All files access · OFF",
                        color = if (hasAccess) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                if (!hasAccess) {
                    Button(
                        onClick = {
                            manager.openAccessSettings()
                            message = "Enable All files, then Refresh"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Grant All files access") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { reload() }) {
                        Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh")
                    }
                    Button(
                        onClick = {
                            pickPack.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        enabled = hasAccess
                    ) {
                        Icon(Icons.Outlined.Extension, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import pack")
                    }
                }
                message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (packs.isEmpty()) {
                item {
                    Text(
                        if (hasAccess) "No packs found yet. Import a .mcpack or open Minecraft once."
                        else "Storage gate blocks the packs folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(packs, key = { it.id }) { pack ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(pack.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${pack.kind} · ${"%.1f".format(pack.sizeBytes / (1024.0 * 1024.0))} MB · " +
                                if (pack.enabled) "enabled" else "disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            pack.path,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pack.enabled) {
                                OutlinedButton(onClick = {
                                    message = manager.setEnabled(pack, false)
                                    reload()
                                }) { Text("Disable") }
                            } else {
                                Button(onClick = {
                                    message = manager.setEnabled(pack, true)
                                    reload()
                                }) { Text("Enable") }
                            }
                            TextButton(onClick = {
                                message = manager.deletePack(pack)
                                reload()
                            }) {
                                Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
