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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.McVersion
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.SkinManager
import com.openmojang.launcher.data.UpdateChecker
import com.openmojang.launcher.data.VersionRepository
import com.openmojang.launcher.ui.components.VersionCover
import com.openmojang.launcher.ui.screens.*
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun OpenMojangApp() {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "Home", Icons.Outlined.Home),
        Tab("versions", "Versions", Icons.Outlined.Download),
        Tab("library", "Library", Icons.Outlined.Inventory2),
        Tab("more", "More", Icons.Outlined.MoreHoriz)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "home"
    val showBar = current in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onVersions = {
                        nav.navigate("versions") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAccount = { nav.navigate("account") },
                    onSkins = { nav.navigate("skins") },
                    onSettings = { nav.navigate("settings") },
                    onLibrary = {
                        nav.navigate("library") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("versions") {
                VersionManagerScreen(onBack = { nav.popBackStack() })
            }
            composable("library") { LibraryScreen() }
            composable("more") {
                MoreScreen(
                    onAccount = { nav.navigate("account") },
                    onSkins = { nav.navigate("skins") },
                    onSettings = { nav.navigate("settings") }
                )
            }
            composable("account") { AccountScreen(onBack = { nav.popBackStack() }) }
            composable("skins") { SkinScreen(onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onAccount: () -> Unit,
    onSkins: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("More") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElevatedCard(onClick = onAccount, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Microsoft account") },
                    supportingContent = { Text("Sign in and gamertag") },
                    leadingContent = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) }
                )
            }
            ElevatedCard(onClick = onSkins, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Skins") },
                    supportingContent = { Text("Import and select") },
                    leadingContent = { Icon(Icons.Outlined.Face, contentDescription = null) }
                )
            }
            ElevatedCard(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Settings") },
                    supportingContent = { Text("Downloads, theme, storage") },
                    leadingContent = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                )
            }
            Text(
                "Maple Launcher · v0.1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

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
    val scope = rememberCoroutineScope()

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

    fun refreshLocal() {
        accountName = auth.current()?.gamertag
        val active = skins.active()
        skinName = active?.name ?: "Steve"
        head = active?.let { skins.headBitmap(it) }
        val bytes = settings.cacheBytes()
        cacheMb = if (bytes <= 0) "0 MB" else "${bytes / (1024 * 1024)} MB"
        installedVer = installed.systemMinecraftVersion()
        mcInstalled = installedVer != null
        libraryCount = installed.listTracked().size
    }

    LaunchedEffect(Unit) {
        refreshLocal()
        scope.launch {
            val versions = repo.getVersions()
            featured = versions.firstOrNull { it.type == "patch" }
                ?: versions.firstOrNull { it.type == "release" }
                ?: versions.firstOrNull()
        }
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
                title = { Text("Maple Launcher") },
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

            featured?.let { v ->
                ElevatedCard(
                    onClick = onVersions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VersionCover(version = v, height = 128.dp)
                    Column(Modifier.padding(12.dp)) {
                        Text("Featured", style = MaterialTheme.typography.labelLarge)
                        Text(v.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            v.changelog.lineSequence().firstOrNull()?.take(120) ?: v.typeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = onVersions, modifier = Modifier.fillMaxWidth()) {
                            Text("Browse versions")
                        }
                    }
                }
            }

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

            ElevatedCard(onClick = onLibrary, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Library") },
                    supportingContent = {
                        Text(
                            if (libraryCount == 0) "No saved APKs · cache $cacheMb"
                            else "$libraryCount saved · cache $cacheMb"
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                    trailingContent = { TextButton(onClick = onLibrary) { Text("Open") } }
                )
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

            Text(
                "Maple Launcher · v0.1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}
