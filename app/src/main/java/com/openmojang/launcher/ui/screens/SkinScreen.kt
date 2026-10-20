package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.data.AuthManager
import com.openmojang.launcher.data.LocalSkin
import com.openmojang.launcher.data.SkinManager
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SkinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val skins = remember { SkinManager(context) }
    val auth = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf(skins.list()) }
    var selected by remember { mutableStateOf<LocalSkin?>(list.firstOrNull()) }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DirtDark)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "← BACK",
                color = AccentGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.weight(1f))
            Text("SKINS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextLight)
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Local skins. Apply needs a real Microsoft token.",
            color = TextLight.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(StoneGray)
                .border(2.dp, ButtonBorder, RoundedCornerShape(4.dp))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selected?.name ?: "—",
                color = TextLight,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(list) { skin ->
                val sel = selected?.id == skin.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sel) DirtBrown else DirtDark.copy(alpha = 0.8f))
                        .border(2.dp, if (sel) AccentGold else ButtonBorder, RoundedCornerShape(4.dp))
                        .clickable { selected = skin }
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (skin.isSteve) "${skin.name} (default)" else skin.name,
                        color = TextLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (status.isNotEmpty()) {
            Text(status, color = AccentGold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        MinecraftButton(
            text = "Apply to Account",
            onClick = {
                val skin = selected ?: return@MinecraftButton
                val account = auth.current()
                if (account == null) {
                    status = "Sign in first"
                    return@MinecraftButton
                }
                scope.launch {
                    val ok = skins.applyToAccount(skin, account)
                    status = if (ok) "Skin applied" else "Need real MSAL token to apply online"
                }
            },
            primary = true
        )
        Spacer(Modifier.height(8.dp))
        MinecraftButton(
            text = "Reset to Steve",
            onClick = {
                selected = list.firstOrNull { it.isSteve }
                status = "Steve selected"
            }
        )
    }
}
