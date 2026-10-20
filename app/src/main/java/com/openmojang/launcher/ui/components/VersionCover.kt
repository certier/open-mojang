package com.openmojang.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
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

/** One-line "what's new" tag for the cover. */
fun coverTag(version: McVersion): String = when (version.cover) {
    "ice" -> "Ice Caves"
    "patch" -> "Hotfix"
    "forest" -> "Wilderness"
    "sulfur" -> "Chaos Cubed"
    "grass" -> "Tiny Takeover"
    else -> version.typeLabel
}

@Composable
fun VersionCover(
    version: McVersion,
    height: Dp = 96.dp,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val p = paletteFor(version.cover)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(p.top, p.bottom)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (version.cover) {
                "ice" -> {
                    // frozen peaks
                    val peaks = Path().apply {
                        moveTo(0f, h * 0.85f)
                        lineTo(w * 0.2f, h * 0.4f)
                        lineTo(w * 0.35f, h * 0.6f)
                        lineTo(w * 0.55f, h * 0.25f)
                        lineTo(w * 0.75f, h * 0.55f)
                        lineTo(w, h * 0.35f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(peaks, color = p.mid.copy(alpha = 0.55f))
                    // ice crystals
                    for (i in 0..5) {
                        val x = w * (0.1f + i * 0.15f)
                        drawCircle(p.accent.copy(alpha = 0.5f), radius = h * 0.04f, center = Offset(x, h * 0.2f))
                    }
                }
                "forest" -> {
                    // tree silhouettes
                    for (i in 0..6) {
                        val x = w * (0.08f + i * 0.14f)
                        val trunk = h * 0.15f
                        drawRect(
                            color = Color(0xFF3E2723).copy(alpha = 0.5f),
                            topLeft = Offset(x - 3f, h * 0.55f),
                            size = Size(6f, trunk)
                        )
                        val canopy = Path().apply {
                            moveTo(x, h * 0.25f)
                            lineTo(x + h * 0.18f, h * 0.55f)
                            lineTo(x - h * 0.18f, h * 0.55f)
                            close()
                        }
                        drawPath(canopy, color = p.mid.copy(alpha = 0.7f))
                    }
                }
                "sulfur" -> {
                    // cave mouth + glow
                    drawCircle(p.accent.copy(alpha = 0.35f), radius = h * 0.35f, center = Offset(w * 0.5f, h * 0.7f))
                    drawCircle(p.mid.copy(alpha = 0.5f), radius = h * 0.2f, center = Offset(w * 0.5f, h * 0.65f))
                }
                "patch" -> {
                    // gear / block grid
                    val s = h * 0.18f
                    drawRect(p.accent, topLeft = Offset(w * 0.72f, h * 0.2f), size = Size(s, s))
                    drawRect(p.mid.copy(alpha = 0.6f), topLeft = Offset(w * 0.6f, h * 0.4f), size = Size(s * 0.8f, s * 0.8f))
                    drawRect(p.accent.copy(alpha = 0.5f), topLeft = Offset(w * 0.78f, h * 0.5f), size = Size(s * 0.6f, s * 0.6f))
                }
                else -> {
                    val hills = Path().apply {
                        moveTo(0f, h * 0.7f)
                        lineTo(w * 0.3f, h * 0.45f)
                        lineTo(w * 0.55f, h * 0.6f)
                        lineTo(w * 0.8f, h * 0.35f)
                        lineTo(w, h * 0.5f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(hills, color = p.top.copy(alpha = 0.4f))
                    val s = size.minDimension * 0.14f
                    drawRect(p.accent, topLeft = Offset(w * 0.78f, h * 0.18f), size = Size(s, s))
                }
            }
        }
        if (showLabel) {
            Text(
                text = coverTag(version).uppercase(),
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
