package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
            Text("SETTINGS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextLight)
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Text("Download source", color = TextLight, fontWeight = FontWeight.Bold)
        Text("mcpe-planet / mcpedl (auto)", color = TextLight.copy(alpha = 0.7f), fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))

        Text("Auto-check for updates", color = TextLight, fontWeight = FontWeight.Bold)
        Text("On", color = GrassGreen, fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))

        Text("Keep old versions", color = TextLight, fontWeight = FontWeight.Bold)
        Text("Yes – manage in Versions tab", color = TextLight.copy(alpha = 0.7f), fontSize = 13.sp)

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Open Mojang is not affiliated with Mojang Studios or Microsoft.",
            color = TextLight.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}
