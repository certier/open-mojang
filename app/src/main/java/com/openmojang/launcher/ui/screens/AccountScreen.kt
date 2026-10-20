package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*

@Composable
fun AccountScreen(onBack: () -> Unit) {
    var signedIn by remember { mutableStateOf(false) }

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

        if (!signedIn) {
            Text(
                text = "Sign in with the same Microsoft account that owns Minecraft Bedrock.",
                color = TextLight.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
            MinecraftButton(
                text = "Sign in with Microsoft",
                onClick = {
                    // MSAL interactive login goes here
                    signedIn = true
                },
                primary = true
            )
        } else {
            Text("Signed in", color = GrassGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Player: Steve", color = TextLight, fontSize = 16.sp)
            Text("UUID: …", color = TextLight.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
            MinecraftButton(text = "Sign Out", onClick = { signedIn = false })
        }
    }
}
