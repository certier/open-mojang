package com.openmojang.launcher.ui.screens

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.ApkInstaller
import com.openmojang.launcher.data.DownloadEngine
import com.openmojang.launcher.data.DownloadProgress
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.McVersion
import com.openmojang.launcher.data.VersionRepository
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { VersionRepository() }
    val engine = remember { DownloadEngine(context) }
    val installed = remember { InstalledVersions(context) }
    val apkInstaller = remember { ApkInstaller(context) }
    val scope = rememberCoroutineScope()

    var versions by remember { mutableStateOf<List<McVersion>>(emptyList()) }
    var filter by remember { mutableStateOf("all") }
    var systemVer by remember { mutableStateOf<String?>(null) }
    var sheetVersion by remember { mutableStateOf<McVersion?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(DownloadProgress(0f, 0, 0)) }
    var error by remember { mutableStateOf<String?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        versions = repo.getVersions()
        systemVer = installed.systemMinecraftVersion()
    }

    val shown = when (filter) {
        "release" -> versions.filter { it.type == "release" }
        "beta" -> versions.filter { it.type == "beta" }
        else -> versions
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Versions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            systemVer?.let {
                ListItem(
                    headlineContent = { Text("Installed") },
                    supportingContent = { Text(it) },
                    trailingContent = {
                        TextButton(onClick = { installed.launchSystemMinecraft() }) { Text("Open") }
                    }
                )
                HorizontalDivider()
            }
            if (!apkInstaller.canRequestPackageInstalls()) {
                TextButton(onClick = { apkInstaller.openUnknownSourcesSettings() }) {
                    Text("Allow this app to install apps")
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("All") })
                FilterChip(selected = filter == "release", onClick = { filter = "release" }, label = { Text("Stable") })
                FilterChip(selected = filter == "beta", onClick = { filter = "beta" }, label = { Text("Beta") })
            }
            LazyColumn {
                items(shown, key = { it.id }) { v ->
                    ListItem(
                        headlineContent = { Text(v.name) },
                        supportingContent = { Text("${v.type} · ${v.date} · ${v.sizeMb} MB") },
                        leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
                        trailingContent = {
                            FilledTonalButton(onClick = {
                                sheetVersion = v
                                error = null
                                apk = null
                                progress = DownloadProgress(0f, 0, 0)
                            }) { Text("Get") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val target = sheetVersion
    if (target != null) {
        ModalBottomSheet(onDismissRequest = { if (!downloading) sheetVersion = null }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(target.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    target.changelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                if (downloading || progress.bytesRead > 0) {
                    val label = if (progress.bytesTotal > 0) {
                        "${(progress.fraction * 100).roundToInt()}% · ${mb(progress.bytesRead)} / ${mb(progress.bytesTotal)} MB"
                    } else {
                        "${mb(progress.bytesRead)} MB"
                    }
                    LinearProgressIndicator(
                        progress = { if (progress.bytesTotal > 0) progress.fraction else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            !context.packageManager.canRequestPackageInstalls()
                        ) {
                            error = "Allow installs from this app first"
                            val intent = android.content.Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            return@Button
                        }
                        downloading = true
                        error = null
                        scope.launch {
                            try {
                                val file = engine.downloadApk("mc-${target.id}.apk".let { name ->
                                    target.downloadUrl.let { url ->
                                        engine.downloadApk(url, "mc-${target.id}.apk") { p -> progress = p }
                                    }
                                }.name) { }
                                // unreachable — rewritten below
                                apk = file
                            } catch (e: Exception) {
                                error = e.message ?: "Download failed"
                            } finally {
                                downloading = false
                            }
                        }
                    },
                    enabled = !downloading && apk == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (downloading) "Downloading" else "Download")
                }
            }
        }
    }
}

private fun mb(bytes: Long): String = "%.1f".format(bytes / (1024.0 * 1024.0))
