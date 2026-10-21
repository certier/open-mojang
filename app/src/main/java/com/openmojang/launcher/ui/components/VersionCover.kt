package com.openmojang.launcher.ui.components

import androidx.compose.foundation.Canvas
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.McVersion

@Composable
fun VersionCover(
    version: McVersion,
    height: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val colors = coverPalette(version)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
            .background(Brush.verticalGradient(colors))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCoverPattern(version, colors)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                version.typeLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                version.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun screenshotUrls(version: McVersion): List<Pair<String, String>> {
    val shots = listOf(
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/1_21_80_header.jpg" to "Flat world presets",
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/bedrock_1-20-80_header.jpg" to "Trial chambers",
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/1_21_90_header.jpg" to "Chase the Skies",
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/tricky_trials_header.jpg" to "Tricky Trials"
    )
    val captions = screenshotCaptions(version)
    return shots.mapIndexed { i, (url, fallback) ->
        url to (captions.getOrNull(i) ?: fallback)
    }
}

@Composable
fun VersionScreenshotStrip(
    version: McVersion,
    modifier: Modifier = Modifier
) {
    val shots = remember(version.id) { screenshotUrls(version) }
    Column(modifier.fillMaxWidth()) {
        Text(
            "What's new",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            shots.forEach { (url, caption) ->
                Column(Modifier.width(220.dp)) {
                    AsyncImage(
                        model = url,
                        contentDescription = caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawScene(version: McVersion, palette: List<Color>) {
    // residual pattern helper kept for VersionCover gradient texture
    val seed = version.id.hashCode()
    for (i in 0 until 12) {
        val x = ((seed * (i + 3)) % 100) / 100f * size.width
        val y = ((seed * (i + 7)) % 100) / 100f * size.height
        drawCircle(
            color = palette.getOrElse(i % palette.size) { Color.White }.copy(alpha = 0.08f),
            radius = 18f + (i % 5) * 6f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawCoverPattern(version: McVersion, palette: List<Color>) {
    drawScene(version, palette)
}

private fun coverPalette(version: McVersion): List<Color> {
    val key = (version.patch.ifBlank { version.name } + version.typeLabel).lowercase()
    return when {
        "patch" in key -> listOf(Color(0xFF1B5E20), Color(0xFF43A047), Color(0xFF81C784))
        "preview" in key || "beta" in key -> listOf(Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFFCE93D8))
        else -> listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF42A5F5))
    }
}

private fun screenshotCaptions(version: McVersion): List<String> {
    val notes = version.changelog.orEmpty()
        .lineSequence()
        .map { it.trim().removePrefix("-").trim() }
        .filter { it.length in 8..80 }
        .take(4)
        .toList()
    return if (notes.isNotEmpty()) notes else listOf(
        "New features in ${version.displayName}",
        "Fixes and polish",
        "World and gameplay updates",
        "Performance improvements"
    )
}
