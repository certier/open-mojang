package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.LocalSkin
import com.openmojang.launcher.data.SkinManager
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val skins = remember { SkinManager(context) }
    val auth = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf(skins.list()) }
    var selected by remember { mutableStateOf<LocalSkin?>(list.firstOrNull()) }
    var status by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DirtDark,
        topBar = {
            TopAppBar(
                title = { Text("Skins") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceContainer,
                    titleContentColor = TextLight,
                    navigationIconContentColor = AccentGold
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Local skins. Online apply needs a real Microsoft token.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                color = SurfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(2.dp, ButtonBorder),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Face, null, tint = TextLight, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            selected?.name ?: "—",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextLight
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(list) { skin ->
                    val sel = selected?.id == skin.id
                    Card(
                        onClick = { selected = skin },
                        colors = CardDefaults.cardColors(
                            containerColor = if (sel) SurfaceContainerHighest else SurfaceContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (sel) AccentGold else ButtonBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (skin.isSteve) "${skin.name} (default)" else skin.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextLight,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (status.isNotEmpty()) {
                Text(status, color = AccentGold, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            MinecraftButton(
                text = "Apply to Account",
                primary = true,
                onClick = {
                    val skin = selected ?: return@MinecraftButton
                    val account = auth.current()
                    if (account == null) {
                        status = "Sign in first"
                        return@MinecraftButton
                    }
                    scope.launch {
                        val ok = skins.applyToAccount(skin, account)
                        status = if (ok) "Skin applied" else "Need real MSAL token to apply online"
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            MinecraftButton(
                text = "Reset to Steve",
                onClick = {
                    selected = list.firstOrNull { it.isSteve }
                    status = "Steve selected"
                }
            )
        }
    }
}
