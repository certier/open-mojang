package com.openmojang.launcher.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.openmojang.launcher.data.ApkInstaller
import com.openmojang.launcher.data.DownloadEngine
import com.openmojang.launcher.data.DownloadProgress
import com.openmojang.launcher.data.InstalledBuild
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.McVersion
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.UpdateChecker
import com.openmojang.launcher.data.VersionRepository
import com.openmojang.launcher.download.DownloadService
import com.openmojang.launcher.ui.components.VersionCover
import com.openmojang.launcher.ui.components.VersionScreenshotStrip
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
    val settings = remember { SettingsStore(context) }

    var versions by remember { mutableStateOf<List<McVersion>>(emptyList()) }
    var filter by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    var systemVer by remember { mutableStateOf<String?>(null) }
    var sheetVersion by remember { mutableStateOf<McVersion?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(DownloadProgress(0f, 0, 0)) }
    var statusLine by remember { mutableStateOf("") }
    var installing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }
    var cachedIds by remember { mutableStateOf(setOf<String>()) }

    fun mb(bytes: Long): String = "%.1f".format(bytes / (1024.0 * 1024.0))
    fun formatSpeed(bps: Long): String {
        if (bps <= 0) return ""
        return if (bps > 1024 * 1024) "%.1f MB/s".format(bps / (1024.0 * 1024.0))
        else "%.0f KB/s".format(bps / 1024.0)
    }

    fun versionLooksInstalled(v: McVersion, system: String?): Boolean {
        if (system.isNullOrBlank()) return false
        return system.contains(v.id) || system.contains(v.patch) || system.startsWith(v.id)
    }

    fun refresh() {
        versions = repo.getVersions()
        systemVer = installed.systemMinecraftVersion()
        val dir = File(context.getExternalFilesDir(null), "Download")
        cachedIds = dir.listFiles()?.mapNotNull { f ->
            if (f.name.endsWith(".apk") && f.length() > 5_000_000) f.name.removePrefix("minecraft-").removeSuffix(".apk") else null
        }?.toSet().orEmpty()
    }

    LaunchedEffect(Unit) { refresh() }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    DownloadService.ACTION_PROGRESS -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID)
                        if (id != null && sheetVersion?.id == id) {
                            progress = DownloadProgress(
                                fraction = intent.getFloatExtra(DownloadService.EXTRA_FRACTION, 0f),
                                bytesRead = intent.getLongExtra(DownloadService.EXTRA_BYTES_READ, 0L),
                                bytesTotal = intent.getLongExtra(DownloadService.EXTRA_BYTES_TOTAL, 0L),
                                speedBps = intent.getLongExtra(DownloadService.EXTRA_SPEED_BPS, 0L)
                            )
                            downloading = true
                        }
                    }
                    DownloadService.ACTION_DONE -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID) ?: return
                        val path = intent.getStringExtra(DownloadService.EXTRA_PATH)
                        downloading = false
                        if (path != null) {
                            apk = File(path)
                            statusLine = "Download complete"
                            cachedIds = cachedIds + id
                        }
                    }
                    DownloadService.ACTION_ERROR -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID)
                        if (id == null || sheetVersion?.id == id) {
                            downloading = false
                            error = intent.getStringExtra(DownloadService.EXTRA_MESSAGE)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(DownloadService.ACTION_PROGRESS)
            addAction(DownloadService.ACTION_DONE)
            addAction(DownloadService.ACTION_ERROR)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val filtered = versions.filter { v ->
        val okType = filter == "all" || v.type == filter
        val okQuery = query.isBlank() || v.displayName.contains(query, true) || v.id.contains(query, true)
        okType && okQuery
    }

    Scaffold(
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search versions") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "release" to "Release", "beta" to "Beta", "patch" to "Patch").forEach { (id, label) ->
                    FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            systemVer?.let { ver ->
                Text("Installed · $ver", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { v ->
                    ElevatedCard(
                        onClick = {
                            sheetVersion = v
                            error = null
                            statusLine = ""
                            progress = DownloadProgress(0f, 0, 0)
                            apk = File(context.getExternalFilesDir(null), "Download/minecraft-${v.id}.apk").takeIf { it.isFile && it.length() > 5_000_000 }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(v.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${v.typeLabel} · ${v.date}" +
                                    if (cachedIds.contains(v.id)) " · saved" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val target = sheetVersion
        if (target != null) {
            ModalBottomSheet(onDismissRequest = { sheetVersion = null }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    VersionCover(version = target, height = 120.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(target.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${target.typeLabel} · ${target.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    VersionScreenshotStrip(version = target)
                    Spacer(Modifier.height(12.dp))
                    Text("What's new", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(target.changelog, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))

                    if (statusLine.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                statusLine,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (downloading || progress.bytesRead > 0L) {
                        LinearProgressIndicator(
                            progress = { if (progress.bytesTotal > 0) progress.fraction.coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        val speed = formatSpeed(progress.speedBps)
                        val label = when {
                            progress.bytesTotal > 0 ->
                                "${(progress.fraction * 100).roundToInt()}%  ·  ${mb(progress.bytesRead)} / ${mb(progress.bytesTotal)} MB" +
                                    if (speed.isNotEmpty()) "  ·  $speed" else ""
                            progress.bytesRead > 0 ->
                                "${mb(progress.bytesRead)} MB" + if (speed.isNotEmpty()) "  ·  $speed" else ""
                            else -> "Starting…"
                        }
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }

                    val alreadyOnDevice = versionLooksInstalled(target, systemVer)
                    if (alreadyOnDevice) {
                        Text("This version looks installed on the device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (apk == null) {
                        Button(
                            onClick = {
                                if (DownloadService.isRunning()) {
                                    error = "Another download is already running"
                                    return@Button
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                    !context.packageManager.canRequestPackageInstalls()
                                ) {
                                    error = "Turn on install permission, then try again"
                                    apkInstaller.openUnknownSourcesSettings()
                                    return@Button
                                }
                                error = null
                                statusLine = "Starting download…"
                                downloading = true
                                DownloadService.start(
                                    context = context,
                                    versionId = target.id,
                                    versionName = target.displayName,
                                    sizeMb = 0,
                                    urls = repo.urlsFor(target)
                                )
                            },
                            enabled = !downloading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (downloading) "Downloading…" else if (settings.installAfterDownload) "Download and install" else "Download")
                        }
                    } else {
                        Text("Saved ${mb(apk!!.length())} MB on disk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    installing = true
                                    statusLine = "Installing… wait for the system prompt"
                                    apkInstaller.install(apk!!)
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    installing = false
                                }
                            },
                            enabled = !installing,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (installing) "Installing…" else "Install APK") }
                        TextButton(
                            onClick = {
                                apk?.delete()
                                apk = null
                                cachedIds = cachedIds - target.id
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Delete saved APK") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
