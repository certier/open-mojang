package com.openmojang.launcher.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.MsAccount
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf<MsAccount?>(auth.current()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = DirtDark,
        topBar = {
            TopAppBar(
                title = { Text("Microsoft Account") },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Surface(
                color = SurfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = if (account != null) GrassLight else TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            when {
                loading -> {
                    CircularProgressIndicator(color = GrassGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("Signing in…", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                account == null -> {
                    Text(
                        "Sign in with the Microsoft account that owns Bedrock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Demo mode until Azure client ID is set.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(28.dp))
                    MinecraftButton(
                        text = "Sign in with Microsoft",
                        primary = true,
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                val activity = context as? Activity
                                if (activity == null) {
                                    error = "No activity"
                                    loading = false
                                    return@launch
                                }
                                val result = auth.signIn(activity)
                                result.onSuccess { account = it }
                                    .onFailure { error = it.message }
                                loading = false
                            }
                        }
                    )
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Signed in") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GrassDark,
                            labelColor = GrassLight
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        account!!.gamertag,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextLight
                    )
                    Text(
                        account!!.uuid,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(28.dp))
                    MinecraftButton(
                        text = "Sign Out",
                        onClick = {
                            auth.signOut()
                            account = null
                        }
                    )
                }
            }
        }
    }
}
