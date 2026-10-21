package com.openmojang.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.OptionsEditor
import com.openmojang.launcher.data.OptionsEditor.GraphicsApi
import com.openmojang.launcher.data.OptionsEditor.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editor = remember { OptionsEditor(context) }
    val installed = remember { InstalledVersions(context) }

    var options by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sourceLabel by remember { mutableStateOf("—") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var packageInfo by remember { mutableStateOf("") }
    var hasAllFiles by remember { mutableStateOf(editor.hasFullStorageAccess()) }
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
        packageInfo = editor.installedPackageInfo()
        val loaded = editor.loadBest()
        if (loaded == null) {
            sourceLabel = if (!hasAllFiles) "Need All files access" else "No options.txt yet"
            applyMap(editor.defaultOptions())
            message = editor.createGameOptions()
        } else {
            sourceLabel = loaded.first
            applyMap(loaded.second)
            message = "Loaded ${loaded.second.size} keys"
        }
    }

    fun currentPatch(): Map<String, String> {
        val m = options.toMutableMap()
        m["gfx_viewdistance"] = viewDistance.toInt().coerceIn(48, 512).toString()
        m["gfx_field_of_view"] = fov.toInt().coerceIn(30, 110).toString()
        m["gfx_gamma"] = "%.2f".format(gamma.coerceIn(0f, 1f))
        m["gfx_fancygraphics"] = if (fancy) "1" else "0"
        m["gfx_smoothlighting"] = if (smooth) "1" else "0"
        m["gfx_fancyskies"] = if (skies) "1" else "0"
        m["gfx_viewbobbing"] = if (bobbing) "1" else "0"
        m["gfx_async_texture_loads"] = if (asyncTex) "1" else "0"
        m["gfx_msaa"] = msaa.coerceIn(1, 8).toString()
        m["gfx_max_framerate"] = maxFps.toString()
        m["gfx_vsync"] = if (vsync) "1" else "0"
        m["gfx_multithreaded_renderer"] = if (multiThread) "1" else "0"
        editor.setGraphicsApi(m, api)
        return m
    }

    fun applyPatch(extra: Map<String, String> = emptyMap()) {
        scope.launch {
            busy = true
            message = null
            val result = withContext(Dispatchers.IO) {
                if (forceStopFirst) editor.forceStopMinecraft()
                val patch = currentPatch().toMutableMap()
                patch.putAll(extra)
                editor.saveBest(patch)
            }
            message = result ?: "Write failed — grant All files access"
            if (result != null) reload()
            busy = false
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        editor.pickedUri = uri
        reload()
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Engine", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Live write into Minecraft Bedrock options.txt. Uses All-files access, merges keys, force-stops the game so the next launch reloads.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Storage gate", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    if (hasAllFiles) "All files access · ON"
                    else "All files access · OFF — scoped storage blocks the game folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasAllFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                if (!hasAllFiles) {
                    Button(
                        onClick = {
                            editor.openAllFilesSettings()
                            message = "Enable All files access, then tap Refresh"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Grant All files access") }
                }
                OutlinedButton(onClick = { reload() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh paths")
                }
                Text(
                    sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (packageInfo.isNotBlank()) {
                    Text(
                        packageInfo,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Instant patches", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "One tap · merge into options.txt · force-stop Minecraft",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            gamma = 1f; smooth = false
                            applyPatch(mapOf("gfx_gamma" to "1", "gfx_smoothlighting" to "0"))
                        },
                        label = { Text("Fullbright") },
                        leadingIcon = { Icon(Icons.Outlined.Visibility, null, Modifier.size(16.dp)) },
                        enabled = !busy && hasAllFiles
                    )
                    AssistChip(
                        onClick = {
                            maxFps = 0; vsync = false
                            applyPatch(mapOf("gfx_max_framerate" to "0", "gfx_vsync" to "0"))
                        },
                        label = { Text("Unlock FPS") },
                        leadingIcon = { Icon(Icons.Outlined.Speed, null, Modifier.size(16.dp)) },
                        enabled = !busy && hasAllFiles
                    )
                    AssistChip(
                        onClick = { api = GraphicsApi.VULKAN; applyPatch() },
                        label = { Text("Vulkan") },
                        leadingIcon = { Icon(Icons.Outlined.Memory, null, Modifier.size(16.dp)) },
                        enabled = !busy && hasAllFiles
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { api = GraphicsApi.OPENGLES; applyPatch() },
                        label = { Text("OpenGL ES") },
                        enabled = !busy && hasAllFiles
                    )
                    AssistChip(
                        onClick = {
                            viewDistance = 256f
                            applyPatch(mapOf("gfx_viewdistance" to "256"))
                        },
                        label = { Text("Render 256") },
                        enabled = !busy && hasAllFiles
                    )
                    AssistChip(
                        onClick = {
                            applyMap(editor.applyPreset(currentPatch(), Preset.PERFORMANCE))
                            applyPatch()
                        },
                        label = { Text("Performance") },
                        enabled = !busy && hasAllFiles
                    )
                }
                Text("GPU: $vulkanLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Graphics backend", style = MaterialTheme.typography.titleMedium)
                GraphicsApi.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = api == option, onClick = { api = option }, role = Role.RadioButton).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = api == option, onClick = { api = option })
                        Spacer(Modifier.width(8.dp))
                        Text(when (option) {
                            GraphicsApi.DEFAULT -> "Default (game decides)"
                            GraphicsApi.OPENGLES -> "OpenGL ES"
                            GraphicsApi.VULKAN -> "Vulkan"
                        })
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Render", style = MaterialTheme.typography.titleMedium)
                Text("View distance · ${viewDistance.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = viewDistance, onValueChange = { viewDistance = it }, valueRange = 48f..512f)
                Text("FOV · ${fov.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = fov, onValueChange = { fov = it }, valueRange = 30f..110f)
                Text("Gamma · ${"%.2f".format(gamma)}", style = MaterialTheme.typography.labelLarge)
                Slider(value = gamma, onValueChange = { gamma = it }, valueRange = 0f..1f)
                Text("MSAA · $msaa", style = MaterialTheme.typography.labelLarge)
                Slider(value = msaa.toFloat(), onValueChange = { msaa = it.toInt().coerceIn(1, 8) }, valueRange = 1f..8f, steps = 6)
                Text("Max FPS · ${if (maxFps == 0) "unlimited" else maxFps}", style = MaterialTheme.typography.labelLarge)
                Slider(value = maxFps.toFloat().coerceAtMost(120f), onValueChange = { maxFps = it.toInt() }, valueRange = 0f..120f, steps = 23)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Toggles", style = MaterialTheme.typography.titleMedium)
                SwitchRow("Fancy graphics", fancy) { fancy = it }
                SwitchRow("Smooth lighting", smooth) { smooth = it }
                SwitchRow("Fancy skies", skies) { skies = it }
                SwitchRow("View bobbing", bobbing) { bobbing = it }
                SwitchRow("Async textures", asyncTex) { asyncTex = it }
                SwitchRow("VSync", vsync) { vsync = it }
                SwitchRow("Multithreaded renderer", multiThread) { multiThread = it }
                SwitchRow("Force-stop Minecraft before write", forceStopFirst) { forceStopFirst = it }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Presets", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Preset.PERFORMANCE to "Perf", Preset.BALANCED to "Balanced", Preset.QUALITY to "Quality", Preset.FULLBRIGHT to "Bright").forEach { (p, label) ->
                        FilterChip(selected = false, onClick = { applyMap(editor.applyPreset(currentPatch(), p)) }, label = { Text(label) })
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Game folders", style = MaterialTheme.typography.titleMedium)
                val packs = remember(hasAllFiles, sourceLabel) { editor.resourcePackDirs() }
                if (packs.isEmpty()) {
                    Text("Grant All files access to list resource packs / worlds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    packs.forEach { dir ->
                        Text("${dir.name} · ${dir.listFiles()?.size ?: 0} entries", style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                    }
                }
                editor.worldsDir()?.let { w ->
                    Text("worlds · ${w.listFiles()?.size ?: 0}", style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                }
                OutlinedButton(onClick = { openDocument.launch(arrayOf("text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pick options.txt manually")
                }
                OutlinedButton(onClick = {
                    val ok = editor.forceStopMinecraft()
                    message = if (ok) "Minecraft process stop requested" else "Could not stop process"
                }, modifier = Modifier.fillMaxWidth()) { Text("Force-stop Minecraft now") }
                OutlinedButton(onClick = {
                    val path = editor.backup(currentPatch())
                    message = if (path != null) "Backup · $path" else "Backup failed"
                }, modifier = Modifier.fillMaxWidth()) { Text("Backup options.txt") }
            }
        }

        AnimatedVisibility(visible = message != null) {
            message?.let {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Button(onClick = { applyPatch() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Outlined.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(if (busy) "Writing…" else "Apply to Minecraft")
        }

        OutlinedButton(
            onClick = { installed.launchSystemMinecraft() },
            modifier = Modifier.fillMaxWidth(),
            enabled = installed.isSystemMinecraftInstalled()
        ) { Text("Open Minecraft") }

        Text(
            "Installed: ${installed.systemMinecraftVersion() ?: "not installed"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
