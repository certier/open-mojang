package com.openmojang.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openmojang.launcher.data.UpdateChecker
import com.openmojang.launcher.ui.screens.*

@Composable
fun OpenMojangApp() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPlay = { nav.navigate("versions") },
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
    onPlay: () -> Unit,
    onAccount: () -> Unit,
    onSkins: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val checker = remember { UpdateChecker(context) }
    var status by remember { mutableStateOf("Checking versions…") }

    LaunchedEffect(Unit) {
        val u = checker.check()
        status = when {
            u.installed == null && u.latestStable != null ->
                "Minecraft is not installed. Latest stable is ${u.latestStable.name}."
            u.updateAvailable && u.latestStable != null ->
                "Update available: ${u.latestStable.name}. Installed: ${u.installed}."
            u.installed != null ->
                "Installed ${u.installed}. Beta ${u.latestBeta?.name ?: "—"}."
            else -> "Catalog ready."
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Open Mojang") },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                "Bedrock launcher",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Versions")
            }
            OutlinedButton(onClick = onAccount, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Microsoft account")
            }
            OutlinedButton(onClick = onSkins, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.Face, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Skins")
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Settings")
            }
            Text(
                "v0.1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}
