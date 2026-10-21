package com.openmojang.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
