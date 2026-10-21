package com.openmojang.launcher.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.DeviceCodeSession
import com.openmojang.launcher.data.LiveProfile
import com.openmojang.launcher.data.MsAccount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf<MsAccount?>(auth.current()) }
    var profile by remember { mutableStateOf<LiveProfile?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<DeviceCodeSession?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    fun fail(t: Throwable) {
        error = listOfNotNull(t.message, t.cause?.message)
            .firstOrNull { !it.isNullOrBlank() }
            ?: t.toString()
        loading = false
        session = null
    }

    fun refreshProfile(acc: MsAccount) {
        scope.launch {
            loading = true
            error = null
            auth.loadLiveProfile(acc)
                .onSuccess {
                    profile = it
                    account = auth.current()
                    status = if (it.totalCount == 0)
                        "Signed in · Gamerscore loaded"
                    else
                        "Signed in · ${it.unlockedCount}/${it.totalCount} Minecraft achievements"
                }
                .onFailure { fail(it) }
            loading = false
        }
    }

    LaunchedEffect(account?.uuid) {
        val acc = account
        if (acc != null && acc.accessToken.isNotBlank() && profile == null) {
            refreshProfile(acc)
        }
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
        if (account != null) {
            val acc = account!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    val av = profile?.avatarUrl.orEmpty()
                    if (av.isNotBlank()) {
                        AsyncImage(
                            model = av,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    Text(acc.gamertag, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "XUID ${acc.uuid}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Xbox profile", style = MaterialTheme.typography.titleSmall)
                            val gs = profile?.gamerscore ?: acc.gamerscore
                            Text(
                                if (gs >= 0) "Gamerscore · $gs" else "Gamerscore · —",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Minecoins are inside Minecraft Marketplace after Xbox sign-in. Maple shows Gamerscore + title achievements from Xbox Live.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            status?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { account?.let { refreshProfile(it) } },
                            enabled = !loading
                        ) { Text(if (loading) "Loading…" else "Refresh") }
                        OutlinedButton(onClick = {
                            auth.signOut()
                            account = null
                            profile = null
                            status = null
                        }) { Text("Sign out") }
                    }
                }
                item {
                    Text("Minecraft achievements", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Icons from Xbox Live media for title Minecraft.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val list = profile?.achievements.orEmpty()
                if (list.isEmpty() && !loading) {
                    item {
                        Text(
                            "No achievements yet. Play on this account then Refresh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(list, key = { it.id }) { a ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (a.iconUrl.isNotBlank()) {
                                AsyncImage(
                                    model = a.iconUrl,
                                    contentDescription = a.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                                )
                            }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(a.name, style = MaterialTheme.typography.titleSmall)
                                if (a.description.isNotBlank()) {
                                    Text(
                                        a.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val state = if (a.progressState == "Achieved" || a.percentComplete >= 100)
                                    "Unlocked" else "In progress · ${a.percentComplete}%"
                                val extra = if (a.rarity.isNotBlank()) " · ${a.rarity}" else ""
                                Text(
                                    state + extra,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        } else {
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
                Text("Not signed in", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Device code — microsoft.com/link",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                session?.let { s ->
                    Text("Code", style = MaterialTheme.typography.labelLarge)
                    Text(
                        s.userCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        (context as? Activity)?.let { auth.openVerification(it, s.verificationUri) }
                    }) { Text("Open microsoft.com/link") }
                    if (loading) {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator()
                    }
                } ?: run {
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                auth.startDeviceCode()
                                    .onSuccess { s ->
                                        session = s
                                        (context as? Activity)?.let {
                                            auth.openVerification(it, s.verificationUri)
                                        }
                                        auth.pollDeviceCode(s)
                                            .onSuccess { acc ->
                                                account = acc
                                                session = null
                                                loading = false
                                                refreshProfile(acc)
                                            }
                                            .onFailure { fail(it) }
                                    }
                                    .onFailure { fail(it) }
                            }
                        },
                        enabled = !loading
                    ) { Text(if (loading) "Starting…" else "Sign in with Microsoft") }
                }
                error?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
