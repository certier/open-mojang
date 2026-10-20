package com.openmojang.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.McVersion

private data class CoverPalette(
    val top: Color,
    val bottom: Color,
    val accent: Color,
    val mid: Color
)

private fun paletteFor(cover: String): CoverPalette = when (cover) {
    "ice" -> CoverPalette(
        Color(0xFF0D47A1), Color(0xFF81D4FA), Color(0xFFE1F5FE), Color(0xFF4FC3F7)
    )
    "patch" -> CoverPalette(
        Color(0xFF1A237E), Color(0xFF3D5AFE), Color(0xFFFFC107), Color(0xFF7986CB)
    )
    "forest" -> CoverPalette(
        Color(0xFF1B5E20), Color(0xFF81C784), Color(0xFFFFF176), Color(0xFF66BB6A)
    )
    "sulfur" -> CoverPalette(
        Color(0xFF4A148C), Color(0xFFFF6F00), Color(0xFFFFD740), Color(0xFFFF8A65)
    )
    "grass" -> CoverPalette(
        Color(0xFF33691E), Color(0xFFAED581), Color(0xFFFFF59D), Color(0xFF8BC34A)
    )
    else -> CoverPalette(
        Color(0xFF1B0000), Color(0xFFBF360C), Color(0xFFFFAB40), Color(0xFFE64A19)
    )
}

fun coverTag(version: McVersion): String = when (version.cover) {
    "ice" -> "Ice Caves"
    "patch" -> "Hotfix"
    "forest" -> "Wilderness"
    "sulfur" -> "Chaos Cubed"
    "grass" -> "Tiny Takeover"
    else -> version.typeLabel
}

/**
 * Build screenshot captions from real changelog bullets so each card shows
 * what this update actually added — not generic art labels.
 */
fun screenshotCaptions(version: McVersion): List<String> {
    val sectionHeaders = setOf(
        "world", "gameplay", "experimental", "fixes", "combat", "items",
        "hotfix", "stability", "realms", "preview", "beta"
    )
    val fromLog = version.changelog
        .lineSequence()
        .map { it.trim() }
        .map { line ->
            line.removePrefix("•")
                .removePrefix("-")
                .removePrefix("*")
                .removePrefix("|")
                .trim()
        }
        .filter { line ->
            val lower = line.lowercase()
            line.length in 10..64 &&
                !lower.startsWith("minecraft ") &&
                !line.endsWith(":") &&
                lower !in sectionHeaders &&
                !lower.startsWith("what minecraft")
        }
        .distinct()
        .take(4)
        .toList()

    if (fromLog.isNotEmpty()) return fromLog

    return when (version.cover) {
        "ice" -> listOf("Ice Caves", "Frostbite mobs", "Brittle ice floors")
        "patch" -> listOf("Stability fixes", "Realms improvements", "Combat fixes")
        "forest" -> listOf("Dappled Forest", "Camp structures", "Sit pads")
        "sulfur" -> listOf("Sulfur caves", "Cinnabar ore", "Sulfur cube")
        "grass" -> listOf("Baby mobs", "Dandelions", "Name tags")
        else -> listOf("World changes", "Gameplay", "Bug fixes")
    }
}

@Composable
fun VersionCover(
    version: McVersion,
    height: Dp = 96.dp,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val p = remember(version.cover) { paletteFor(version.cover) }
    val tag = remember(version.cover, version.typeLabel) { coverTag(version) }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(p.top, p.bottom)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension * 0.16f
            drawRect(
                color = p.accent.copy(alpha = 0.85f),
                topLeft = Offset(size.width * 0.78f, size.height * 0.18f),
                size = Size(s, s)
            )
            drawRect(
                color = p.mid.copy(alpha = 0.35f),
                topLeft = Offset(0f, size.height * 0.72f),
                size = Size(size.width, size.height * 0.28f)
            )
        }
        if (showLabel) {
            Text(
                text = tag.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
            Text(
                text = version.typeLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun VersionScreenshotStrip(
    version: McVersion,
    modifier: Modifier = Modifier
) {
    val captions = remember(version.id, version.cover, version.changelog) {
        screenshotCaptions(version)
    }
    val p = remember(version.cover) { paletteFor(version.cover) }
    val borderColor = MaterialTheme.colorScheme.outline

    Column(modifier.fillMaxWidth()) {
        Text(
            "What's new in this update",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Each card is a real highlight from the changelog",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            captions.forEachIndexed { index, caption ->
                ScreenshotCard(
                    cover = version.cover,
                    palette = p,
                    caption = caption,
                    indexLabel = "${index + 1}/${captions.size}",
                    variant = index,
                    borderColor = borderColor
                )
            }
        }
    }
}

@Composable
private fun ScreenshotCard(
    cover: String,
    palette: CoverPalette,
    caption: String,
    indexLabel: String,
    variant: Int,
    borderColor: Color
) {
    Column(Modifier.width(200.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(palette.top, palette.bottom)))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawScene(cover, palette, variant)
            }
            Text(
                indexLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    "NEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    caption,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun DrawScope.drawScene(cover: String, p: CoverPalette, variant: Int) {
    val w = size.width
    val h = size.height
    when (cover) {
        "ice" -> {
            val peaks = Path().apply {
                moveTo(0f, h)
                lineTo(w * 0.2f, h * (0.45f - variant * 0.05f))
                lineTo(w * 0.4f, h * 0.65f)
                lineTo(w * 0.65f, h * (0.3f + variant * 0.04f))
                lineTo(w, h * 0.5f)
                lineTo(w, h)
                close()
            }
            drawPath(peaks, p.mid.copy(alpha = 0.55f))
            drawCircle(p.accent.copy(alpha = 0.45f), h * 0.06f, Offset(w * 0.3f, h * 0.25f))
            drawCircle(p.accent.copy(alpha = 0.35f), h * 0.04f, Offset(w * 0.7f, h * 0.2f))
        }
        "forest" -> {
            for (i in 0..4) {
                val x = w * (0.12f + i * 0.18f)
                drawRect(Color(0xFF3E2723).copy(alpha = 0.45f), Offset(x - 2f, h * 0.55f), Size(4f, h * 0.2f))
                val canopy = Path().apply {
                    moveTo(x, h * (0.28f + (i % 2) * 0.05f))
                    lineTo(x + h * 0.14f, h * 0.55f)
                    lineTo(x - h * 0.14f, h * 0.55f)
                    close()
                }
                drawPath(canopy, p.mid.copy(alpha = 0.75f))
            }
        }
        "sulfur" -> {
            drawCircle(p.accent.copy(alpha = 0.4f), h * 0.35f, Offset(w * 0.5f, h * 0.7f))
            drawCircle(p.mid.copy(alpha = 0.55f), h * 0.18f, Offset(w * 0.5f, h * 0.62f))
            if (variant > 0) {
                drawRect(p.accent.copy(alpha = 0.5f), Offset(w * 0.15f, h * 0.3f), Size(h * 0.12f, h * 0.12f))
            }
        }
        "patch" -> {
            val s = h * 0.16f
            drawRect(p.accent, Offset(w * 0.2f + variant * 8f, h * 0.25f), Size(s, s))
            drawRect(p.mid.copy(alpha = 0.7f), Offset(w * 0.45f, h * 0.4f), Size(s * 0.9f, s * 0.9f))
            drawRect(p.accent.copy(alpha = 0.55f), Offset(w * 0.65f, h * 0.28f), Size(s * 0.7f, s * 0.7f))
        }
        else -> {
            val hills = Path().apply {
                moveTo(0f, h)
                lineTo(w * 0.3f, h * 0.45f)
                lineTo(w * 0.55f, h * 0.6f)
                lineTo(w * 0.85f, h * 0.35f)
                lineTo(w, h * 0.5f)
                lineTo(w, h)
                close()
            }
            drawPath(hills, p.top.copy(alpha = 0.45f))
            drawRect(p.accent, Offset(w * 0.7f, h * 0.2f), Size(h * 0.14f, h * 0.14f))
        }
    }
}

@Composable
fun BoxScope.CoverBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
