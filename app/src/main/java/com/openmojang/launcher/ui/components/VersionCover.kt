package com.openmojang.launcher.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.openmojang.launcher.data.McVersion

/**
 * Official Mojang CDN images that return HTTP 200.
 * Verified against minecraft.net content/dam assets.
 */
private object MojangShots {
    const val WINTER_DROP =
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/MC_WinterDrop26_BPS_Oct13_FeatureCombo_Main_1170x500.jpg"
    const val KEY_ART =
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/NewKeyArt_Header.jpg"
    const val HEADER_1_21_80 =
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/1_21_80_header.jpg"
    const val BEDROCK_1_20_80 =
        "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/screenshots/bedrock_1-20-80_header.jpg"
    const val KEY_ART_ALT =
        "https://www.minecraft.net/content/dam/minecraftnet/archive/6a01155c9d646735449a3587d29bae0c-screenshotguide_header.png"

    val all = listOf(WINTER_DROP, KEY_ART, HEADER_1_21_80, BEDROCK_1_20_80, KEY_ART_ALT)
}

/** Stable per-version pick so each build looks different in the list / detail sheet. */
fun coverImageFor(version: McVersion): String {
    val idx = (version.id.hashCode().and(0x7fffffff)) % MojangShots.all.size
    return MojangShots.all[idx]
}

fun screenshotUrls(version: McVersion): List<Pair<String, String>> {
    val captions = screenshotCaptions(version)
    val seed = version.id.hashCode().and(0x7fffffff)
    val pool = MojangShots.all
    val ordered = (0 until pool.size).map { pool[(seed + it) % pool.size] }
    return ordered.take(4).mapIndexed { i, url ->
        url to (captions.getOrNull(i) ?: "Screenshot ${i + 1}")
    }
}

private fun screenshotCaptions(version: McVersion): List<String> {
    val bits = version.changelog
        .lineSequence()
        .map { it.trim().removePrefix("-").trim() }
        .filter { it.length in 4..48 }
        .take(4)
        .toList()
    if (bits.size >= 2) return bits
    return when {
        version.type == "beta" -> listOf("Preview features", "Experimental packs", "Bug fixes", "Performance")
        version.type == "patch" -> listOf("Hotfix", "Stability", "Crash fixes", "Balance")
        else -> listOf("New content", "World generation", "Mobs & blocks", "Quality of life")
    }
}

private fun coverPalette(version: McVersion): List<Color> {
    return when {
        version.type == "beta" || version.cover == "ice" -> listOf(
            Color(0xFF1B3A4B), Color(0xFF2E6B8A), Color(0xFF7EB6C9)
        )
        version.type == "patch" -> listOf(
            Color(0xFF3E2723), Color(0xFF6D4C41), Color(0xFFA1887F)
        )
        else -> listOf(
            Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF81C784)
        )
    }
}

@Composable
fun VersionCover(
    version: McVersion,
    height: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val colors = coverPalette(version)
    val image = remember(version.id) { coverImageFor(version) }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(0.dp))
            .background(Brush.verticalGradient(colors))
    ) {
        SubcomposeAsyncImage(
            model = image,
            contentDescription = version.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors))
                )
            },
            error = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors))
                )
            }
        )
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

@Composable
fun VersionScreenshotStrip(
    version: McVersion,
    modifier: Modifier = Modifier
) {
    val shots = remember(version.id) { screenshotUrls(version) }
    val palette = coverPalette(version)
    Column(modifier.fillMaxWidth()) {
        Text(
            "Screenshots",
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
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(palette))
                    ) {
                        SubcomposeAsyncImage(
                            model = url,
                            contentDescription = caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(Modifier.fillMaxSize()) {
                                    Text(
                                        "…",
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            },
                            error = {
                                Box(Modifier.fillMaxSize()) {
                                    Text(
                                        caption,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(8.dp)
                                    )
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
