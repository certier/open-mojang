package com.openmojang.launcher.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.openmojang.launcher.data.LocalSkin
import com.openmojang.launcher.data.SkinManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { SkinManager(context) }
    var skins by remember { mutableStateOf(manager.list()) }
    var message by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<LocalSkin?>(null) }
    var renameText by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val imported = manager.importFromUri(uri, "Skin ${skins.size}")
        skins = manager.list()
        message = if (imported == null) {
            "Need a classic skin PNG (64×64 or 64×32, or HD multiples)"
        } else {
            "Imported ${imported.name}"
        }
    }

    val active = skins.firstOrNull { it.active }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Skins") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { picker.launch("image/png") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Import skin")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picker.launch("image/png") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Import PNG") }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            SkinPreview(active, manager)
            if (message.isNotEmpty()) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Text(
                "Classic 64×64 or 64×32 PNG. Active skin is stored on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(skins, key = { it.id }) { skin ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                manager.setActive(skin.id)
                                skins = manager.list()
                                message = "Using ${skin.name}"
                            },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (skin.active) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val head = manager.headBitmap(skin)
                            if (head != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = head.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            } else {
                                Icon(Icons.Outlined.Face, contentDescription = null, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(skin.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    when {
                                        skin.active -> "Selected"
                                        skin.isSteve -> "Default"
                                        else -> "Tap to use"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!skin.isSteve) {
                                IconButton(onClick = {
                                    renameTarget = skin
                                    renameText = skin.name
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rename")
                                }
                                IconButton(onClick = {
                                    manager.delete(skin.id)
                                    skins = manager.list()
                                    message = "Deleted ${skin.name}"
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename skin") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (manager.rename(target.id, renameText)) {
                        skins = manager.list()
                        message = "Renamed"
                    }
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SkinPreview(skin: LocalSkin?, manager: SkinManager) {
    val head: Bitmap? = skin?.let { manager.headBitmap(it) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            when {
                skin != null && skin.path.isNotEmpty() && File(skin.path).exists() && head != null -> {
                    androidx.compose.foundation.Image(
                        bitmap = head.asImageBitmap(),
                        contentDescription = skin.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                skin != null && skin.path.isNotEmpty() && File(skin.path).exists() -> {
                    AsyncImage(
                        model = File(skin.path),
                        contentDescription = skin.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Face, contentDescription = null)
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(skin?.name ?: "Steve", style = MaterialTheme.typography.titleLarge)
            Text(
                if (skin?.isSteve == true) "Default" else "Custom",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
