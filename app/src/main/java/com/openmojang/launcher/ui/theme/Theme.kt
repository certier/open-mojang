package com.openmojang.launcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.ThemeMode

private val LightScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = GrassDark,
    secondary = SecondaryGray,
    onSecondary = OnPrimary,
    tertiary = TertiaryAmber,
    background = SurfaceLight,
    onBackground = Ink,
    surface = SurfaceCard,
    onSurface = Ink,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = InkMuted,
    outline = OutlineGoogle,
    error = ErrorRed,
    onError = OnPrimary
)

private val DarkScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFA8C7FA),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF062E6F),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF0842A0),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD3E3FD),
    secondary = androidx.compose.ui.graphics.Color(0xFFC4C7C5),
    background = androidx.compose.ui.graphics.Color(0xFF1F1F1F),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE3E3E3),
    surface = androidx.compose.ui.graphics.Color(0xFF1F1F1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE3E3E3),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF303030),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC4C7C5),
    outline = androidx.compose.ui.graphics.Color(0xFF444746),
    error = androidx.compose.ui.graphics.Color(0xFFF2B8B5)
)

@Composable
fun OpenMojangTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val store = SettingsStore(context)
    val systemDark = isSystemInDarkTheme()
    val dark = when (store.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.surface.toArgb()
            window.navigationBarColor = scheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = OpenMojangTypography,
        shapes = OpenMojangShapes,
        content = content
    )
}
