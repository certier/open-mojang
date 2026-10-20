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
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*

@Composable
fun SkinScreen(onBack: () -> Unit) {
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
            Text("SKINS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextLight)
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Browse, upload, and apply skins to your Microsoft account.",
            color = TextLight.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        // Placeholder skin preview box
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(StoneGray)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text("SKIN", color = TextLight)
        }

        Spacer(Modifier.height(24.dp))

        MinecraftButton(text = "Change Skin", onClick = { /* open gallery / file picker */ }, primary = true)
        Spacer(Modifier.height(12.dp))
        MinecraftButton(text = "Upload Custom Skin", onClick = { })
        Spacer(Modifier.height(12.dp))
        MinecraftButton(text = "Reset to Steve", onClick = { })
    }
}
