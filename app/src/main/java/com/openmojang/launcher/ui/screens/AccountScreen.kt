package com.openmojang.launcher.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.MsAccount
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf<MsAccount?>(auth.current()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DirtDark)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "← BACK",
                color = AccentGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.weight(1f))
            Text("MICROSOFT ACCOUNT", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextLight)
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GrassGreen)
            }
            Spacer(Modifier.height(16.dp))
            Text("Signing in…", color = TextLight.copy(alpha = 0.7f))
        } else if (account == null) {
            Text(
                text = "Sign in with the same Microsoft account that owns Minecraft Bedrock.",
                color = TextLight.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Demo mode active until Azure client ID is configured.",
                color = TextLight.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(24.dp))
            MinecraftButton(
                text = "Sign in with Microsoft",
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
                },
                primary = true
            )
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = AccentGold, fontSize = 13.sp)
            }
        } else {
            Text("Signed in", color = GrassGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Gamertag: ${account!!.gamertag}", color = TextLight, fontSize = 16.sp)
            Text("UUID: ${account!!.uuid}", color = TextLight.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
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
