package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var autoUpdate by remember { mutableStateOf(store.autoCheckUpdates) }
    var keepOld by remember { mutableStateOf(store.keepOldVersions) }

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
            Text("SETTINGS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextLight)
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Text("Download source", color = TextLight, fontWeight = FontWeight.Bold)
        Text("mcpe-planet / mcpedl (auto)", color = TextLight.copy(alpha = 0.7f), fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        SettingToggle(
            title = "Auto-check for updates",
            subtitle = if (autoUpdate) "On" else "Off",
            checked = autoUpdate,
            onCheckedChange = {
                autoUpdate = it
                store.autoCheckUpdates = it
            }
        )

        Spacer(Modifier.height(12.dp))

        SettingToggle(
            title = "Keep old versions",
            subtitle = if (keepOld) "Yes – manage in Versions" else "No",
            checked = keepOld,
            onCheckedChange = {
                keepOld = it
                store.keepOldVersions = it
            }
        )

        Spacer(Modifier.height(32.dp))

        Text("Open Mojang v0.1.0", color = TextLight.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Not affiliated with Mojang Studios or Microsoft.",
            color = TextLight.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextLight, fontWeight = FontWeight.Bold)
            Text(subtitle, color = if (checked) GrassGreen else TextLight.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextLight,
                checkedTrackColor = GrassGreen,
                uncheckedThumbColor = TextLight,
                uncheckedTrackColor = StoneGray
            )
        )
    }
}
