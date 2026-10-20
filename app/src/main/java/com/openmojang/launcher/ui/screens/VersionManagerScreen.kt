package com.openmojang.launcher.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import com.openmojang.launcher.data.MirrorPref
import com.openmojang.launcher.data.SettingsStore
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

    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        versions = repo.getVersions()
        systemVer = installed.systemMinecraftVersion()
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // seed cache map
        val ids = mutableSetOf<String>()
        for (v in versions) {
            val name = "minecraft-${v.id}.apk"
            if (engine.cached(name) != null) ids += v.id
        }
        cachedIds = ids
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    DownloadService.ACTION_PROGRESS -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID)
                        if (id != sheetVersion?.id) return
                        downloading = true
                        progress = DownloadProgress(
                            fraction = intent.getFloatExtra(DownloadService.EXTRA_FRACTION, 0f),
                            bytesRead = intent.getLongExtra(DownloadService.EXTRA_BYTES_READ, 0L),
                            bytesTotal = intent.getLongExtra(DownloadService.EXTRA_BYTES_TOTAL, 0L),
                            speedBps = intent.getLongExtra(DownloadService.EXTRA_SPEED_BPS, 0L)
                        )
                        statusLine = "Downloading…"
                    }
                    DownloadService.ACTION_COMPLETE -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID) ?: return
                        val path = intent.getStringExtra(DownloadService.EXTRA_PATH)
                        downloading = false
                        if (id == sheetVersion?.id && path != null) {
                            apk = File(path)
                            cachedIds = cachedIds + id
                            statusLine = "Download complete" +
                                if (settings.installAfterDownload) " · install started" else ""
                            installed.track(
                                InstalledBuild(
                                    versionId = id,
                                    fileName = File(path).name,
                                    path = path,
                                    bytes = File(path).length(),
                                    installedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    DownloadService.ACTION_ERROR -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID)
                        if (id != sheetVersion?.id) return
                        downloading = false
                        error = intent.getStringExtra(DownloadService.EXTRA_MESSAGE)
                        statusLine = "Download failed"
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(DownloadService.ACTION_PROGRESS)
            addAction(DownloadService.ACTION_COMPLETE)
            addAction(DownloadService.ACTION_ERROR)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    val filtered = versions.filter { v ->
        val typeOk = when (filter) {
            "release" -> v.type == "release" || v.type == "patch"
            "beta" -> v.type == "beta"
            "patch" -> v.type == "patch"
            else -> true
        }
        val q = query.trim().lowercase()
        val textOk = q.isEmpty() ||
            v.displayName.lowercase().contains(q) ||
            v.id.contains(q) ||
            v.changelog.lowercase().contains(q)
        typeOk && textOk
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
        ) {
            systemVer?.let { ver ->
                val match = repo.findByInstalled(ver)
                ListItem(
                    headlineContent = { Text("Installed on device") },
                    supportingContent = {
                        Text(ver + (match?.let { " · ${it.typeLabel}" } ?: ""))
                    },
                    trailingContent = {
                        TextButton(onClick = { installed.launchSystemMinecraft() }) { Text("Play") }
                    }
                )
            }

            if (!apkInstaller.canRequestPackageInstalls()) {
                TextButton(
                    onClick = { apkInstaller.openUnknownSourcesSettings() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { Text("Allow this app to install apps") }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search versions") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true
            )

            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "release" to "Release", "patch" to "Patch", "beta" to "Beta").forEach { (id, label) ->
                    FilterChip(
                        selected = filter == id,
                        onClick = { filter = id },
                        label = { Text(label) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { v ->
                    val isCached = v.id in cachedIds
                    val already = versionLooksInstalled(v, systemVer)
                    ElevatedCard(
                        onClick = {
                            sheetVersion = v
                            error = null
                            statusLine = ""
                            installing = false
                            progress = DownloadProgress(0f, 0, 0)
                            apk = engine.cached("minecraft-${v.id}.apk")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VersionCover(version = v, height = 88.dp)
                        ListItem(
                            headlineContent = { Text(v.displayName) },
                            supportingContent = {
                                Text(
                                    "${v.typeLabel} · ${v.date} · ~${v.sizeMb} MB" +
                                        when {
                                            already -> " · on device"
                                            isCached -> " · downloaded"
                                            else -> ""
                                        }
                                )
                            },
                            trailingContent = {
                                FilledTonalButton(
                                    onClick = {
                                        if (already) {
                                            installed.launchSystemMinecraft()
                                        } else {
                                            sheetVersion = v
                                            apk = engine.cached("minecraft-${v.id}.apk")
                                        }
                                    }
                                ) {
                                    Text(
                                        when {
                                            already -> "Play"
                                            isCached -> "Install"
                                            else -> "Get"
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val target = sheetVersion
    if (target != null) {
        ModalBottomSheet(onDismissRequest = { if (!downloading && !installing) sheetVersion = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                VersionCover(version = target, height = 120.dp)
                Spacer(Modifier.height(12.dp))
                Text(target.displayName, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${target.typeLabel} · ${target.date} · ~${target.sizeMb} MB",
                    style = MaterialTheme.typography.labelLarge,
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
                    Button(
                        onClick = { installed.launchSystemMinecraft() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Play Minecraft") }
                    if (apk != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                installing = true
                                statusLine = "Installing — confirm the system prompt and wait…"
                                try {
                                    engine.installApk(apk!!)
                                    statusLine = "Install started — finish in the system dialog"
                                } catch (e: Exception) {
                                    error = e.message
                                    statusLine = "Install failed"
                                } finally {
                                    installing = false
                                }
                            },
                            enabled = !installing,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (installing) "Installing…" else "Reinstall APK") }
                    }
                } else if (apk == null) {
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
                            val urls = repo.urlsFor(target)
                            DownloadService.start(
                                context = context,
                                versionId = target.id,
                                versionName = target.displayName,
                                sizeMb = target.sizeMb,
                                urls = urls
                            )
                        },
                        enabled = !downloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                downloading -> "Downloading…"
                                settings.installAfterDownload -> "Download and install"
                                else -> "Download"
                            }
                        )
                    }
                } else {
                    Text(
                        "Saved ${mb(apk!!.length())} MB · expected ~${target.sizeMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            try {
                                installing = true
                                statusLine = "Installing — confirm the system prompt and wait…"
                                error = null
                                engine.installApk(apk!!)
                                statusLine = "Install started — finish in the system dialog"
                            } catch (e: Exception) {
                                error = e.message
                                statusLine = "Install failed"
                            } finally {
                                installing = false
                            }
                        },
                        enabled = !installing && !downloading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (installing) "Installing…" else "Install") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (DownloadService.isRunning()) {
                                error = "Wait for the current download to finish"
                                return@OutlinedButton
                            }
                            apk?.delete()
                            apk = null
                            cachedIds = cachedIds - target.id
                            progress = DownloadProgress(0f, 0, 0)
                            statusLine = ""
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Redownload") }
                }
            }
        }
    }
}

private fun versionLooksInstalled(v: McVersion, systemVer: String?): Boolean {
    if (systemVer.isNullOrBlank()) return false
    if (systemVer == v.patch || systemVer == v.name) return true
    return systemVer.contains(v.id) || v.patch.contains(systemVer) || systemVer.contains(v.patch)
}

private fun mb(bytes: Long): String =
    if (bytes <= 0L) "0.0" else "%.1f".format(bytes / (1024.0 * 1024.0))

private fun formatSpeed(bps: Long): String {
    if (bps <= 0) return ""
    val kb = bps / 1024.0
    return if (kb >= 1024) "%.1f MB/s".format(kb / 1024.0) else "%.0f KB/s".format(kb)
}
