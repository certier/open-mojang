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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.AccountStore
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.DeviceCodeSession
import com.openmojang.launcher.data.MsAccount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val store = remember { AccountStore(context) }
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf<MsAccount?>(auth.current()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<DeviceCodeSession?>(null) }
    var editName by remember { mutableStateOf(account?.gamertag.orEmpty()) }
    var editing by remember { mutableStateOf(false) }

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
                loading && session != null -> {
                    Text("Enter this code on Microsoft", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        session!!.userCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        session!!.verificationUri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Waiting for you to finish in the browser…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            (context as? Activity)?.let {
                                auth.openVerification(it, session!!.verificationUri)
                            }
                        }
                    ) { Text("Open Microsoft link again") }
                }
                loading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Starting Microsoft sign-in…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                account == null -> {
                    Text(
                        "Sign in with the Microsoft account that owns Bedrock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Uses device code login (same pattern as Xbox / Minecraft launchers).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            loading = true
                            error = null
                            session = null
                            scope.launch {
                                val activity = context as? Activity
                                if (activity == null) {
                                    error = "No activity"
                                    loading = false
                                    return@launch
                                }
                                val started = auth.startDeviceCode()
                                started.onSuccess { s ->
                                    session = s
                                    auth.openVerification(activity, s.verificationUri)
                                    val result = auth.pollDeviceCode(s)
                                    result.onSuccess {
                                        account = it
                                        editName = it.gamertag
                                        session = null
                                    }.onFailure { error = it.message }
                                }.onFailure { error = it.message }
                                loading = false
                                session = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign in with Microsoft") }
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    if (editing) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val trimmed = editName.trim()
                                    if (trimmed.isNotEmpty()) {
                                        val updated = account!!.copy(gamertag = trimmed)
                                        store.save(updated)
                                        account = updated
                                        editing = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Save") }
                            OutlinedButton(
                                onClick = {
                                    editName = account!!.gamertag
                                    editing = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }
                    } else {
                        Text(account!!.gamertag, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            account!!.uuid,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Xbox Live signed in",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                editName = account!!.gamertag
                                editing = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Edit display name") }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            auth.signOut()
                            account = null
                            editing = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign out") }
                }
            }
        }
    }
}
