package com.openmojang.launcher.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.McVersion
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.SkinManager
import com.openmojang.launcher.data.UpdateChecker
import com.openmojang.launcher.data.VersionRepository
import com.openmojang.launcher.ui.components.VersionCover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVersions: () -> Unit,
    onAccount: () -> Unit,
    onSkins: () -> Unit,
    onSettings: () -> Unit,
    onLibrary: () -> Unit
) {
    val context = LocalContext.current
    val checker = remember { UpdateChecker(context) }
    val installed = remember { InstalledVersions(context) }
    val auth = remember { AuthManager(context) }
    val skins = remember { SkinManager(context) }
    val settings = remember { SettingsStore(context) }
    val repo = remember { VersionRepository() }

    var status by remember { mutableStateOf("Checking…") }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestName by remember { mutableStateOf<String?>(null) }
    var featured by remember { mutableStateOf<McVersion?>(null) }
    var mcInstalled by remember { mutableStateOf(false) }
    var installedVer by remember { mutableStateOf<String?>(null) }
    var accountName by remember { mutableStateOf<String?>(null) }
    var skinName by remember { mutableStateOf("Steve") }
    var head by remember { mutableStateOf<Bitmap?>(null) }
    var cacheMb by remember { mutableStateOf("0 MB") }
    var libraryCount by remember { mutableStateOf(0) }
    var launchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        accountName = auth.current()?.gamertag
        val active = skins.active()
        skinName = active?.name ?: "Steve"
        head = active?.let { skins.headBitmap(it) }
        val bytes = settings.cacheBytes()
        cacheMb = if (bytes <= 0) "0 MB" else "${bytes / (1024 * 1024)} MB"
        installedVer = installed.systemMinecraftVersion()
        mcInstalled = installedVer != null
        libraryCount = installed.listTracked().size

        val versions = withContext(Dispatchers.IO) { repo.getVersions() }
        featured = versions.maxWithOrNull(
            compareBy { UpdateChecker.versionKey(it.patch.ifBlank { it.name }) }
        )

        if (settings.autoCheckUpdates) {
            val u = withContext(Dispatchers.IO) { checker.check() }
            updateAvailable = u.updateAvailable
            latestName = u.updateTargetName
            status = when {
                u.installed == null && u.newestInCatalog != null ->
                    "Minecraft not installed · catalog ${u.newestInCatalog.displayName}"
                u.updateAvailable && u.updateTargetName != null ->
                    "Update available · ${u.updateTargetName}"
                u.installed != null ->
                    "Ready · ${u.installed}" +
                        (u.installedMatch?.let { " (${it.typeLabel})" } ?: "")
                else -> "Catalog ready"
            }
        } else {
            status = installedVer?.let { "Ready · $it" } ?: "Minecraft not installed"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Maple Launcher", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Bedrock",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (mcInstalled) "Minecraft is installed" else "Minecraft is not installed",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedContent(
                        targetState = mcInstalled to updateAvailable,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "playButtons"
                    ) { (hasMc, needsUpdate) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hasMc) {
                                Button(
                                    onClick = {
                                        launchError = null
                                        if (!installed.launchSystemMinecraft()) {
                                            launchError = "Could not open Minecraft"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play")
                                }
                                if (needsUpdate && latestName != null) {
                                    OutlinedButton(
                                        onClick = onVersions,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Update to $latestName")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = onVersions,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Icon(Icons.Outlined.Download, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (latestName != null) "Get Minecraft $latestName"
                                        else "Browse versions"
                                    )
                                }
                            }
                        }
                    }

                    launchError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            AnimatedVisibility(visible = featured != null, enter = fadeIn()) {
                featured?.let { v ->
                    ElevatedCard(
                        onClick = onVersions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VersionCover(version = v, height = 120.dp)
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Featured",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(v.displayName, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${v.typeLabel} · ${v.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text("Quick links", style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedCard(onClick = onAccount, modifier = Modifier.weight(1f)) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                        Text("Account", style = MaterialTheme.typography.labelLarge)
                        Text(
                            accountName ?: "Sign in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ElevatedCard(onClick = onSkins, modifier = Modifier.weight(1f)) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (head != null) {
                            Image(
                                bitmap = head!!.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.None,
                                modifier = Modifier.size(28.dp).clip(CircleShape)
                            )
                        } else {
                            Icon(Icons.Outlined.Face, contentDescription = null)
                        }
                        Text("Skins", style = MaterialTheme.typography.labelLarge)
                        Text(
                            skinName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedCard(onClick = onLibrary, modifier = Modifier.weight(1f)) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null)
                        Text("Library", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "$libraryCount tracked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ElevatedCard(onClick = onVersions, modifier = Modifier.weight(1f)) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Text("Versions", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Cache $cacheMb",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                "Maple · 0.1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 28.dp)
            )
        }
    }
}
