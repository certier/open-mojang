package com.openmojang.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openmojang.launcher.ui.screens.*
import com.openmojang.launcher.ui.theme.*

@Composable
fun OpenMojangApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPlay = { navController.navigate("versions") },
                onAccount = { navController.navigate("account") },
                onSkins = { navController.navigate("skins") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable("versions") {
            VersionManagerScreen(onBack = { navController.popBackStack() })
        }
        composable("account") {
            AccountScreen(onBack = { navController.popBackStack() })
        }
        composable("skins") {
            SkinScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onAccount: () -> Unit,
    onSkins: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF87CEEB), DirtDark) // sky to dirt like official
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top logo area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "MINECRAFT",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = TextLight,
                    style = LocalTextStyle.current.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = TextShadow,
                            offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                            blurRadius = 0f
                        )
                    )
                )
                Text(
                    text = "BEDROCK EDITION",
                    fontSize = 16.sp,
                    color = AccentGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Open Mojang Launcher",
                    fontSize = 12.sp,
                    color = TextLight.copy(alpha = 0.7f)
                )
            }

            // Main buttons – classic Minecraft style
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                MinecraftButton(text = "Play", onClick = onPlay, primary = true)
                MinecraftButton(text = "Microsoft Account", onClick = onAccount)
                MinecraftButton(text = "Skins", onClick = onSkins)
                MinecraftButton(text = "Settings", onClick = onSettings)
            }

            // Bottom status
            Text(
                text = "Ready · Latest: checking…",
                fontSize = 12.sp,
                color = TextLight.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun MinecraftButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = if (primary) GrassGreen else ButtonFace
    val borderColor = if (primary) GrassDark else ButtonBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(3.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = TextShadow,
                    offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                    blurRadius = 0f
                )
            )
        )
    }
}
