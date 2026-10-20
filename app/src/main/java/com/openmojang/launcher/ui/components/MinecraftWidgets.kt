package com.openmojang.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.ui.theme.*

@Composable
fun MinecraftButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bg = when {
        !enabled -> StoneGray
        primary -> GrassGreen
        else -> ButtonFace
    }
    val borderColor = when {
        !enabled -> ButtonBorder.copy(alpha = 0.5f)
        primary -> GrassDark
        else -> ButtonBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(3.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) TextLight else TextLight.copy(alpha = 0.5f),
            style = LocalTextStyle.current.copy(
                shadow = Shadow(
                    color = TextShadow,
                    offset = Offset(1.5f, 1.5f),
                    blurRadius = 0f
                )
            )
        )
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "← BACK",
            color = AccentGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onBack() }
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = title.uppercase(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = TextLight
        )
        Spacer(Modifier.weight(1f))
    }
}
