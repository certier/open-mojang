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
    val accent: Color
)

private fun paletteFor(cover: String): CoverPalette = when (cover) {
    "ice" -> CoverPalette(Color(0xFF0D47A1), Color(0xFF81D4FA), Color(0xFFE1F5FE))
    "patch" -> CoverPalette(Color(0xFF1A237E), Color(0xFF3D5AFE), Color(0xFFFFC107))
    "forest" -> CoverPalette(Color(0xFF1B5E20), Color(0xFF81C784), Color(0xFFFFF176))
    "sulfur" -> CoverPalette(Color(0xFF4A148C), Color(0xFFFF6F00), Color(0xFFFFD740))
    "grass" -> CoverPalette(Color(0xFF33691E), Color(0xFFAED581), Color(0xFFFFF59D))
    else -> CoverPalette(Color(0xFF0B1220), Color(0xFF3D5AFE), Color(0xFFFFC107))
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
            // mountains / hills silhouette
            val path = Path().apply {
                moveTo(0f, size.height * 0.75f)
                lineTo(size.width * 0.25f, size.height * 0.45f)
                lineTo(size.width * 0.45f, size.height * 0.65f)
                lineTo(size.width * 0.7f, size.height * 0.35f)
                lineTo(size.width, size.height * 0.55f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, color = p.top.copy(alpha = 0.35f))
            // accent block
            val s = size.minDimension * 0.14f
            drawRect(
                color = p.accent,
                topLeft = Offset(size.width * 0.78f, size.height * 0.18f),
                size = androidx.compose.ui.geometry.Size(s, s)
            )
        }
        if (showLabel) {
            Text(
                text = version.typeLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
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
