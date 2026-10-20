package com.openmojang.launcher.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Tab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun OpenMojangApp() {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "Home", Icons.Outlined.Home),
        Tab("versions", "Versions", Icons.Outlined.Download),
        Tab("modify", "Modify", Icons.Outlined.Tune),
        Tab("library", "Library", Icons.Outlined.Inventory2),
        Tab("more", "More", Icons.Outlined.MoreHoriz)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "home"
    val showBar = current in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
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
            composable("modify") { ModifyScreen() }
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
        featured = versions.firstOrNull { it.type == "patch" }
            ?: versions.firstOrNull { it.type == "release" }
            ?: versions.firstOrNull()

        if (settings.autoCheckUpdates) {
            val u = withContext(Dispatchers.IO) { checker.check() }
            updateAvailable = u.updateAvailable
            latestName = u.latestStable?.displayName
            status = when {
                u.installed == null && u.latestStable != null ->
                    "Minecraft not installed · latest ${u.latestStable.displayName}"
                u.updateAvailable && u.latestStable != null ->
                    "Update available · ${u.latestStable.displayName}"
                u.installed != null ->
                    "Ready · ${u.installed}"
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
                                v.changelog.lineSequence().firstOrNull()?.take(100) ?: v.typeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            FilledTonalButton(
                                onClick = onVersions,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Browse versions")
                            }
                        }
                    }
                }
            }

            Text(
                "Quick access",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(onClick = onLibrary, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Library") },
                    supportingContent = {
                        Text(
                            if (libraryCount == 0) "No saved APKs · $cacheMb cache"
                            else "$libraryCount saved · $cacheMb cache"
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.Inventory2, contentDescription = null) }
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
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 28.dp)
            )
        }
    }
}
