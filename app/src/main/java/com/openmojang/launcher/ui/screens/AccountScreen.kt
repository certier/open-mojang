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
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.DeviceCodeSession
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
    var session by remember { mutableStateOf<DeviceCodeSession?>(null) }
    var editName by remember { mutableStateOf(account?.gamertag.orEmpty()) }
    var editing by remember { mutableStateOf(false) }

    fun fail(t: Throwable) {
        error = listOfNotNull(t.message, t.cause?.message)
            .firstOrNull { !it.isNullOrBlank() }
            ?: t.toString()
        loading = false
        session = null
    }

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
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        session!!.verificationUri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Waiting for you to finish in the browser…",
                        style = MaterialTheme.typography.bodySmall,
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
                        "Device code login — enter the code at microsoft.com/link",
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
                                auth.startDeviceCode()
                                    .onSuccess { s ->
                                        session = s
                                        auth.openVerification(activity, s.verificationUri)
                                        auth.pollDeviceCode(s)
                                            .onSuccess {
                                                account = it
                                                editName = it.gamertag
                                                loading = false
                                                session = null
                                            }
                                            .onFailure { fail(it) }
                                    }
                                    .onFailure { fail(it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign in with Microsoft") }
                }
                else -> {
                    Text(account!!.gamertag, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "ID ${account!!.uuid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    if (editing) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val updated = account!!.copy(gamertag = editName.trim().ifBlank { account!!.gamertag })
                                auth.current()
                                com.openmojang.launcher.data.AccountStore(context).save(updated)
                                account = updated
                                editing = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save") }
                    } else {
                        OutlinedButton(
                            onClick = { editing = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Edit display name") }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            auth.signOut()
                            account = null
                            editName = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign out") }
                }
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
