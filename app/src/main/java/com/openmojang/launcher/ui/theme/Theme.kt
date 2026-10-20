package com.openmojang.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Official Minecraft Android launcher palette
val DirtBrown = Color(0xFF8B5A2B)
val DirtDark = Color(0xFF5C3A1E)
val GrassGreen = Color(0xFF5D9C3B)
val GrassDark = Color(0xFF3D6B27)
val StoneGray = Color(0xFF7A7A7A)
val ButtonFace = Color(0xFFC6A17A)
val ButtonBorder = Color(0xFF3F2A1A)
val TextLight = Color(0xFFE8E0D0)
val TextShadow = Color(0xFF2A1A0A)
val AccentGold = Color(0xFFE0B020)

private val MinecraftColorScheme = darkColorScheme(
    primary = GrassGreen,
    onPrimary = TextLight,
    secondary = DirtBrown,
    onSecondary = TextLight,
    tertiary = AccentGold,
    background = DirtDark,
    onBackground = TextLight,
    surface = DirtBrown,
    onSurface = TextLight,
    surfaceVariant = StoneGray,
    onSurfaceVariant = TextLight
)

@Composable
fun OpenMojangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinecraftColorScheme,
        content = content
    )
}
