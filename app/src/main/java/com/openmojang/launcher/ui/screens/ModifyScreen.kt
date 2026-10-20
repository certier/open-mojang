package com.openmojang.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.OptionsEditor
import com.openmojang.launcher.data.OptionsEditor.GraphicsApi
import com.openmojang.launcher.data.OptionsEditor.Preset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyScreen() {
    val context = LocalContext.current
    val editor = remember { OptionsEditor(context) }
    val installed = remember { InstalledVersions(context) }

    var options by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sourceLabel by remember { mutableStateOf("No options.txt found") }
    var message by remember { mutableStateOf<String?>(null) }
    var api by remember { mutableStateOf(GraphicsApi.DEFAULT) }
    var viewDistance by remember { mutableFloatStateOf(192f) }
    var fov by remember { mutableFloatStateOf(60f) }
    var gamma by remember { mutableFloatStateOf(0.5f) }
    var fancy by remember { mutableStateOf(true) }
    var smooth by remember { mutableStateOf(true) }
    var skies by remember { mutableStateOf(true) }
    var bobbing by remember { mutableStateOf(true) }
    var asyncTex by remember { mutableStateOf(true) }
    var msaa by remember { mutableIntStateOf(2) }
    var maxFps by remember { mutableIntStateOf(0) }
    var vsync by remember { mutableStateOf(false) }
    var multiThread by remember { mutableStateOf(true) }
    var forceStopFirst by remember { mutableStateOf(true) }
    var hasAllFiles by remember { mutableStateOf(editor.hasFullStorageAccess()) }
    val vulkanLabel = remember { editor.vulkanLevelLabel() }

    fun applyMap(map: Map<String, String>) {
        options = map
        api = editor.graphicsApiOf(map)
        viewDistance = map["gfx_viewdistance"]?.toFloatOrNull() ?: 192f
        fov = map["gfx_field_of_view"]?.toFloatOrNull() ?: 60f
        gamma = map["gfx_gamma"]?.toFloatOrNull() ?: 0.5f
        fancy = map["gfx_fancygraphics"] != "0"
        smooth = map["gfx_smoothlighting"] != "0"
        skies = map["gfx_fancyskies"] != "0"
        bobbing = map["gfx_viewbobbing"] != "0"
        asyncTex = map["gfx_async_texture_loads"] != "0"
        msaa = map["gfx_msaa"]?.toIntOrNull() ?: 2
        maxFps = map["gfx_max_framerate"]?.toIntOrNull() ?: 0
        vsync = map["gfx_vsync"] == "1"
        multiThread = map["gfx_multithreaded_renderer"] != "0"
    }

    fun reload() {
        hasAllFiles = editor.hasFullStorageAccess()
        val loaded = editor.loadBest()
        if (loaded == null) {
            sourceLabel = if (!hasAllFiles) {
                "Need All files access · tap Grant below"
            } else {
                "No options.txt found · open Minecraft once then Reload"
            }
            options = emptyMap()
            message = null
        } else {
            sourceLabel = loaded.first
            applyMap(loaded.second)
            message = "Loaded"
        }
    }

    fun buildOptions(): Map<String, String> {
        val m = options.toMutableMap()
        m["gfx_viewdistance"] = viewDistance.toInt().coerceIn(48, 512).toString()
        m["gfx_field_of_view"] = fov.toInt().coerceIn(30, 110).toString()
        m["gfx_gamma"] = "%.2f".format(gamma.coerceIn(0f, 1f))
        m["gfx_fancygraphics"] = if (fancy) "1" else "0"
        m["gfx_smoothlighting"] = if (smooth) "1" else "0"
        m["gfx_fancyskies"] = if (skies) "1" else "0"
        m["gfx_viewbobbing"] = if (bobbing) "1" else "0"
        m["gfx_async_texture_loads"] = if (asyncTex) "1" else "0"
        m["gfx_msaa"] = msaa.toString()
        m["gfx_max_framerate"] = maxFps.toString()
        m["gfx_vsync"] = if (vsync) "1" else "0"
        m["gfx_multithreaded_renderer"] = if (multiThread) "1" else "0"
        editor.setGraphicsApi(m, api)
        return m
    }

    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        editor.pickedUri = uri
        editor.loadFromUri(uri)?.let {
            sourceLabel = "Picked file"
            applyMap(it)
            message = "Loaded from picker"
        } ?: run { message = "Could not read that file" }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modify") },
                actions = {
                    IconButton(onClick = {
                        openDoc.launch(OptionsEditor.optionsMimeTypes())
                    }) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = "Pick options.txt")
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
            Text(
                "Real options.txt edits — same keys Minecraft Bedrock stores.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Source", style = MaterialTheme.typography.labelLarge)
                    Text(sourceLabel, style = MaterialTheme.typography.bodySmall)
                    val ver = installed.systemMinecraftVersion()
                    Text(
                        if (ver != null) "Minecraft · $ver" else "Minecraft not installed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        vulkanLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (hasAllFiles) "All files access · on" else "All files access · off",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasAllFiles) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { reload() }) { Text("Reload") }
                        if (!hasAllFiles) {
                            TextButton(onClick = {
                                editor.openAllFilesSettings()
                                message = "Grant All files access, then come back and tap Reload"
                            }) { Text("Grant access") }
                        }
                    }
                }
            }

            Text("Presets", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = {
                        applyMap(editor.applyPreset(options.ifEmpty { emptyMap() }, Preset.PERFORMANCE))
                    },
                    label = { Text("Perf") }
                )
                AssistChip(
                    onClick = {
                        applyMap(editor.applyPreset(options.ifEmpty { emptyMap() }, Preset.BALANCED))
                    },
                    label = { Text("Balanced") }
                )
                AssistChip(
                    onClick = {
                        applyMap(editor.applyPreset(options.ifEmpty { emptyMap() }, Preset.QUALITY))
                    },
                    label = { Text("Quality") }
                )
                AssistChip(
                    onClick = {
                        applyMap(editor.applyPreset(options.ifEmpty { emptyMap() }, Preset.FULLBRIGHT))
                    },
                    label = { Text("Bright") }
                )
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Graphics API", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Writes backend preference keys. Device: $vulkanLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GraphicsApi.entries.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = api == option,
                                    onClick = { api = option },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = api == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (option) {
                                    GraphicsApi.DEFAULT -> "Default"
                                    GraphicsApi.OPENGLES -> "Prefer OpenGL ES"
                                    GraphicsApi.VULKAN -> "Prefer Vulkan"
                                }
                            )
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Render", style = MaterialTheme.typography.titleSmall)
                    Text("View distance: ${viewDistance.toInt()} blocks")
                    Slider(
                        value = viewDistance,
                        onValueChange = { viewDistance = it },
                        valueRange = 48f..512f,
                        steps = 14
                    )
                    Text("FOV: ${fov.toInt()}°")
                    Slider(
                        value = fov,
                        onValueChange = { fov = it },
                        valueRange = 30f..110f,
                        steps = 15
                    )
                    Text("Gamma: ${"%.2f".format(gamma)}")
                    Slider(
                        value = gamma,
                        onValueChange = { gamma = it },
                        valueRange = 0f..1f
                    )
                    Text("MSAA: ${msaa}x")
                    Slider(
                        value = msaa.toFloat(),
                        onValueChange = { msaa = it.toInt().coerceIn(1, 8) },
                        valueRange = 1f..8f,
                        steps = 6
                    )
                    Text("Max FPS: ${if (maxFps == 0) "Unlimited" else maxFps}")
                    Slider(
                        value = maxFps.toFloat(),
                        onValueChange = { maxFps = it.toInt() },
                        valueRange = 0f..120f,
                        steps = 23
                    )
                    SwitchRow("Fancy graphics", fancy) { fancy = it }
                    SwitchRow("Smooth lighting", smooth) { smooth = it }
                    SwitchRow("Fancy skies", skies) { skies = it }
                    SwitchRow("View bobbing", bobbing) { bobbing = it }
                    SwitchRow("Async textures", asyncTex) { asyncTex = it }
                    SwitchRow("VSync", vsync) { vsync = it }
                    SwitchRow("Multithreaded renderer", multiThread) { multiThread = it }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Apply tools", style = MaterialTheme.typography.titleSmall)
                    SwitchRow("Force-stop Minecraft first", forceStopFirst) { forceStopFirst = it }
                    Text(
                        "Stops the game so it cannot overwrite options.txt when it exits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            val ok = editor.forceStopMinecraft()
                            message = if (ok) "Minecraft process stop requested" else "Could not stop process"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Force-stop Minecraft now") }
                    OutlinedButton(
                        onClick = {
                            val path = editor.backup(buildOptions())
                            message = if (path != null) "Backup: $path" else "Backup failed"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Backup options.txt") }
                }
            }

            Button(
                onClick = {
                    if (forceStopFirst) editor.forceStopMinecraft()
                    val built = buildOptions()
                    options = built
                    val result = editor.saveBest(built)
                    message = result ?: "Save failed · grant All files access or pick options.txt"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Apply to Minecraft")
            }

            OutlinedButton(
                onClick = { installed.launchSystemMinecraft() },
                modifier = Modifier.fillMaxWidth(),
                enabled = installed.isSystemMinecraftInstalled()
            ) {
                Text("Open Minecraft")
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Text(
                "Grant All files access once so the app finds options.txt itself. Force-stop before Apply.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
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
