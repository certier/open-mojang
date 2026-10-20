package com.openmojang.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openmojang.launcher.data.UpdateChecker
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
    val context = LocalContext.current
    val checker = remember { UpdateChecker(context) }
    var status by remember { mutableStateOf("Checking…") }
    var statusTone by remember { mutableStateOf(StatusTone.Neutral) }

    LaunchedEffect(Unit) {
        val u = checker.check()
        when {
            u.installed == null && u.latestStable != null -> {
                status = "Not installed · Latest ${u.latestStable.name}"
                statusTone = StatusTone.Warn
            }
            u.updateAvailable && u.latestStable != null -> {
                status = "Update · ${u.latestStable.name}"
                statusTone = StatusTone.Warn
            }
            u.installed != null -> {
                status = "Ready · ${u.installed}"
                statusTone = StatusTone.Good
            }
            else -> {
                status = "Catalog ready"
                statusTone = StatusTone.Neutral
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SkyTop, SkyMid, DirtMid, DirtDark)
                )
            )
    ) {
        // Soft vignette layer
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            DirtDark.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "MINECRAFT",
                    style = MaterialTheme.typography.displayLarge.copy(
                        shadow = Shadow(
                            color = TextShadow,
                            offset = Offset(3f, 3f),
                            blurRadius = 0f
                        )
                    ),
                    color = TextLight
                )
                Text(
                    text = "BEDROCK EDITION",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Open Mojang  ·  v0.1.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuCard(
                    title = "Play",
                    subtitle = "Versions, download, launch",
                    icon = Icons.Filled.PlayArrow,
                    primary = true,
                    onClick = onPlay
                )
                MenuCard(
                    title = "Microsoft Account",
                    subtitle = "Sign in & profile",
                    icon = Icons.Filled.AccountCircle,
                    onClick = onAccount
                )
                MenuCard(
                    title = "Skins",
                    subtitle = "Browse and apply",
                    icon = Icons.Filled.Face,
                    onClick = onSkins
                )
                MenuCard(
                    title = "Settings",
                    subtitle = "Launcher preferences",
                    icon = Icons.Filled.Settings,
                    onClick = onSettings
                )
            }

            StatusChip(text = status, tone = statusTone)
        }
    }
}

enum class StatusTone { Neutral, Good, Warn }

@Composable
private fun StatusChip(text: String, tone: StatusTone) {
    val bg = when (tone) {
        StatusTone.Good -> GrassDark.copy(alpha = 0.85f)
        StatusTone.Warn -> Color(0xFF6A4A12).copy(alpha = 0.9f)
        StatusTone.Neutral -> SurfaceContainer.copy(alpha = 0.9f)
    }
    val border = when (tone) {
        StatusTone.Good -> GrassLight
        StatusTone.Warn -> AccentGold
        StatusTone.Neutral -> OutlineSoft
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "cardScale")

    val container = if (primary) GrassGreen else SurfaceContainerHigh
    val borderColor = if (primary) GrassDark else ButtonBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
            .border(2.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        color = container,
        shadowElevation = if (primary) 6.dp else 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (primary) GrassDark else DirtMid,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextLight,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btnScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(scale)
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .border(3.dp, borderColor, MaterialTheme.shapes.small)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                shadow = Shadow(
                    color = TextShadow,
                    offset = Offset(1.5f, 1.5f),
                    blurRadius = 0f
                )
            ),
            color = TextLight
        )
    }
}

// local Color alias for StatusChip warn tone
private fun Color(hex: Long) = androidx.compose.ui.graphics.Color(hex)
