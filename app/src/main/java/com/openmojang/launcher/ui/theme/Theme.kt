package com.openmojang.launcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MinecraftDarkScheme = darkColorScheme(
    primary = GrassGreen,
    onPrimary = TextLight,
    primaryContainer = GrassDark,
    onPrimaryContainer = TextLight,
    secondary = DirtBrown,
    onSecondary = TextLight,
    secondaryContainer = SurfaceContainer,
    onSecondaryContainer = TextLight,
    tertiary = AccentGold,
    onTertiary = TextShadow,
    tertiaryContainer = Color(0xFF5A4A18),
    onTertiaryContainer = AccentGold,
    background = DirtDark,
    onBackground = TextLight,
    surface = NightSurface,
    onSurface = TextLight,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = SurfaceContainerLow,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = DirtBrown,
    outline = OutlineSoft,
    outlineVariant = ButtonBorder,
    error = ErrorRed,
    onError = TextLight
)

// local helper — Color already imported via other theme files in same package
private fun Color(hex: Long) = androidx.compose.ui.graphics.Color(hex)

@Composable
fun OpenMojangTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = MinecraftDarkScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DirtDark.toArgb()
            window.navigationBarColor = DirtDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OpenMojangTypography,
        shapes = OpenMojangShapes,
        content = content
    )
}
