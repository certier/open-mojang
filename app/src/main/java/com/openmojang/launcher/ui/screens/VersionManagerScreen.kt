package com.openmojang.launcher.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.openmojang.launcher.data.ApkInstaller
import com.openmojang.launcher.data.DownloadEngine
import com.openmojang.launcher.data.DownloadProgress
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.McVersion
import com.openmojang.launcher.data.MirrorPref
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.VersionRepository
import com.openmojang.launcher.download.DownloadService
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
    var systemVer by remember { mutableStateOf<String?>(null) }
    var sheetVersion by remember { mutableStateOf<McVersion?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(DownloadProgress(0f, 0, 0)) }
    var statusLine by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed either way — notification is best-effort */ }

    LaunchedEffect(Unit) {
        versions = repo.getVersions()
        systemVer = installed.systemMinecraftVersion()
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Listen for service progress / complete / error
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(DownloadService.ACTION_PROGRESS)
            addAction(DownloadService.ACTION_COMPLETE)
            addAction(DownloadService.ACTION_ERROR)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    DownloadService.ACTION_PROGRESS -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID)
                        if (sheetVersion?.id != null && id != sheetVersion?.id) return
                        val fraction = intent.getFloatExtra(DownloadService.EXTRA_FRACTION, 0f)
                        val read = intent.getLongExtra(DownloadService.EXTRA_BYTES_READ, 0L)
                        val total = intent.getLongExtra(DownloadService.EXTRA_BYTES_TOTAL, 0L)
                        progress = DownloadProgress(fraction, read, total)
                        downloading = true
                        statusLine = if (read == 0L) "Resolving APK…" else "Downloading in background…"
                        error = null
                    }
                    DownloadService.ACTION_COMPLETE -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID)
                        if (sheetVersion?.id != null && id != sheetVersion?.id) return
                        val path = intent.getStringExtra(DownloadService.EXTRA_PATH)
                        val bytes = intent.getLongExtra(DownloadService.EXTRA_BYTES_TOTAL, 0L)
                        apk = path?.let { File(it) }
                        downloading = false
                        statusLine = "Done · ${mb(bytes)} MB" +
                            if (settings.installAfterDownload) " · install started" else ""
                        progress = DownloadProgress(
                            1f,
                            bytes,
                            bytes
                        )
                    }
                    DownloadService.ACTION_ERROR -> {
                        val id = intent.getStringExtra(DownloadService.EXTRA_VERSION_ID)
                        if (sheetVersion?.id != null && id != sheetVersion?.id) return
                        error = intent.getStringExtra(DownloadService.EXTRA_MESSAGE) ?: "Download failed"
                        downloading = false
                        statusLine = ""
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
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
                TextButton(
                    onClick = { apkInstaller.openUnknownSourcesSettings() },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Allow this app to install apps")
                }
            }
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("All") })
                FilterChip(selected = filter == "release", onClick = { filter = "release" }, label = { Text("Stable") })
                FilterChip(selected = filter == "beta", onClick = { filter = "beta" }, label = { Text("Beta") })
            }
            LazyColumn {
                items(shown, key = { it.id }) { v ->
                    val cached = engine.cached("minecraft-${v.id}.apk")
                    ListItem(
                        headlineContent = { Text(v.name) },
                        supportingContent = {
                            Text(
                                buildString {
                                    append("${v.type} · ${v.date} · ~${v.sizeMb} MB")
                                    if (cached != null) {
                                        append(" · saved ${mb(cached.length())} MB")
                                    }
                                }
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
                        trailingContent = {
                            FilledTonalButton(onClick = {
                                sheetVersion = v
                                error = null
                                statusLine = ""
                                progress = DownloadProgress(0f, 0, 0)
                                downloading = false
                                apk = engine.cached("minecraft-${v.id}.apk")
                            }) { Text(if (cached != null) "Install" else "Get") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val target = sheetVersion
    if (target != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetVersion = null }
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                Text(target.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${target.type} · ${target.date} · catalog ~${target.sizeMb} MB",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    target.changelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                if (statusLine.isNotEmpty()) {
                    Text(statusLine, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                }

                if (downloading || progress.bytesRead > 0L) {
                    LinearProgressIndicator(
                        progress = {
                            if (progress.bytesTotal > 0) progress.fraction.coerceIn(0f, 1f) else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    val label = if (progress.bytesTotal > 0) {
                        "${(progress.fraction * 100).roundToInt()}%  ·  ${mb(progress.bytesRead)} / ${mb(progress.bytesTotal)} MB"
                    } else if (progress.bytesRead > 0) {
                        "${mb(progress.bytesRead)} MB"
                    } else {
                        "Resolving…"
                    }
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Runs in background — check the notification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                }

                if (apk == null) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                !context.packageManager.canRequestPackageInstalls()
                            ) {
                                error = "Turn on install permission, then try again"
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                                return@Button
                            }
                            if (settings.wifiOnlyDownloads && !isOnWifi(context)) {
                                error = "Wi‑Fi only is on. Connect to Wi‑Fi or turn it off in Settings."
                                return@Button
                            }
                            if (Build.VERSION.SDK_INT >= 33) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            downloading = true
                            error = null
                            statusLine = "Starting background download…"
                            progress = DownloadProgress(0f, 0, 0)
                            val urls = filterUrls(repo.urlsFor(target), settings.preferredMirror)
                            DownloadService.start(
                                context = context,
                                versionId = target.id,
                                versionName = target.name,
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
                                statusLine = "Installing…"
                                engine.installApk(apk!!)
                            } catch (e: Exception) {
                                error = e.message
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Install saved APK") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            apk?.delete()
                            apk = null
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

private fun mb(bytes: Long): String =
    if (bytes <= 0L) "0.0" else "%.1f".format(bytes / (1024.0 * 1024.0))

private fun isOnWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

private fun filterUrls(urls: List<String>, mirror: MirrorPref): List<String> {
    return when (mirror) {
        MirrorPref.AUTO -> urls
        MirrorPref.MCPEDL -> urls.filter {
            it.contains("mcpedl", ignoreCase = true) || it.contains(".apk", ignoreCase = true)
        }.ifEmpty { urls }
        MirrorPref.MCPE_PLANET -> urls.filter {
            it.contains("mcpe-planet", ignoreCase = true) || it.contains(".apk", ignoreCase = true)
        }.ifEmpty { urls }
    }
}
