package com.openmojang.launcher.ui

import android.graphics.Bitmap
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.SkinManager
import com.openmojang.launcher.data.UpdateChecker
import com.openmojang.launcher.ui.screens.*

@Composable
fun OpenMojangApp() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onVersions = { nav.navigate("versions") },
                onAccount = { nav.navigate("account") },
                onSkins = { nav.navigate("skins") },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable("versions") { VersionManagerScreen(onBack = { nav.popBackStack() }) }
        composable("account") { AccountScreen(onBack = { nav.popBackStack() }) }
        composable("skins") { SkinScreen(onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVersions: () -> Unit,
    onAccount: () -> Unit,
    onSkins: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val checker = remember { UpdateChecker(context) }
    val installed = remember { InstalledVersions(context) }
    val auth = remember { AuthManager(context) }
    val skins = remember { SkinManager(context) }
    val settings = remember { SettingsStore(context) }

    var status by remember { mutableStateOf("Checking…") }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestName by remember { mutableStateOf<String?>(null) }
    var mcInstalled by remember { mutableStateOf(false) }
    var installedVer by remember { mutableStateOf<String?>(null) }
    var accountName by remember { mutableStateOf<String?>(null) }
    var skinName by remember { mutableStateOf("Steve") }
    var head by remember { mutableStateOf<Bitmap?>(null) }
    var cacheMb by remember { mutableStateOf("0 MB") }
    var launchError by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        accountName = auth.current()?.gamertag
        val active = skins.active()
        skinName = active?.name ?: "Steve"
        head = active?.let { skins.headBitmap(it) }
        val bytes = settings.cacheBytes()
        cacheMb = if (bytes <= 0) "0 MB" else "${bytes / (1024 * 1024)} MB"
        installedVer = installed.systemMinecraftVersion()
        mcInstalled = installedVer != null
    }

    LaunchedEffect(Unit) {
        refresh()
        if (settings.autoCheckUpdates) {
            val u = checker.check()
            updateAvailable = u.updateAvailable
            latestName = u.latestStable?.displayName
            status = when {
                u.installed == null && u.latestStable != null ->
                    "Not installed · latest ${u.latestStable.displayName}"
                u.updateAvailable && u.latestStable != null ->
                    "Update ${u.latestStable.displayName} · installed ${u.installed}"
                u.installed != null ->
                    "Up to date · ${u.installed}" +
                        (u.installedMatch?.let { " (${it.displayName})" } ?: "")
                else -> "Catalog ready"
            }
        } else {
            status = installedVer?.let { "Installed $it" } ?: "Minecraft not installed"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Peak Launcher") },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Bedrock · release · Minecraft Patch · beta",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", style = MaterialTheme.typography.labelLarge)
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    if (updateAvailable && latestName != null) {
                        FilledTonalButton(
                            onClick = onVersions,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Get $latestName")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    launchError = null
                    if (!installed.launchSystemMinecraft()) {
                        launchError = "Minecraft is not installed on this device"
                    }
                },
                enabled = mcInstalled,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (mcInstalled) "Play Minecraft" else "Minecraft not installed")
            }
            launchError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            ElevatedCard(onClick = onAccount, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(accountName ?: "Microsoft account") },
                    supportingContent = {
                        Text(if (accountName != null) "Signed in" else "Not signed in")
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                    }
                )
            }

            ElevatedCard(onClick = onSkins, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(skinName) },
                    supportingContent = { Text("Active skin") },
                    leadingContent = {
                        if (head != null) {
                            Image(
                                bitmap = head!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(Icons.Outlined.Face, contentDescription = null)
                        }
                    }
                )
            }

            ElevatedCard(onClick = onVersions, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Versions") },
                    supportingContent = { Text("Downloads · cache $cacheMb") },
                    leadingContent = {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                    },
                    trailingContent = {
                        TextButton(onClick = onVersions) { Text("Open") }
                    }
                )
            }

            Text(
                "Peak Launcher · v0.1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}
