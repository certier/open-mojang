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
        topBar = {
            TopAppBar(
                title = { Text("Microsoft account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))

            when {
                loading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Signing in…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                account == null -> {
                    Text(
                        "Sign in with the Microsoft account that owns Bedrock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Demo profile until an Azure client id is set.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
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
                                result.onSuccess { account = it }.onFailure { error = it.message }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign in") }
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Text(account!!.gamertag, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        account!!.uuid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            auth.signOut()
                            account = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign out") }
                }
            }
        }
    }
}
