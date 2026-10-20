package com.openmojang.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*

data class McVersion(
    val id: String,
    val name: String,
    val type: String, // "release" or "beta"
    val date: String,
    val sizeMb: Int,
    val changelog: String,
    val downloadUrl: String = ""
)

@Composable
fun VersionManagerScreen(onBack: () -> Unit) {
    // Sample data – real engine will pull from mcpe-planet / mcpedl sources
    val versions = remember {
        listOf(
            McVersion("26.70.20", "1.26.70.20", "beta", "13 Oct 2026", 520,
                "Ice Caves Drop beta – Realms permission fixes, faster uploads, touch control stutter fixed."),
            McVersion("26.52", "1.26.52", "release", "28 Sep 2026", 510,
                "What Minecraft 26.52 fixes:\n• Stability improvements after Wilderness Bound\n• Realms interface polish\n• Accessibility and UI fixes\n• Performance on mid-range Android devices"),
            McVersion("26.60.30", "1.26.60.30", "beta", "6 Oct 2026", 515,
                "Touch control stutter fixed, achievements restored, Realms upload speed improved."),
            McVersion("26.50", "1.26.50", "release", "15 Sep 2026", 505,
                "Wilderness Bound drop – Dappled Forest, abandoned camps, sit pads, baby mob overhaul.")
        )
    }

    var selected by remember { mutableStateOf<McVersion?>(null) }
    var downloading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DirtDark)
            .padding(16.dp)
    ) {
        // Header
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
                text = "VERSIONS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextLight
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Download any Mojang build. Latest first.",
            color = TextLight.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(versions) { v ->
                VersionCard(
                    version = v,
                    selected = selected?.id == v.id,
                    onClick = { selected = v }
                )
            }
        }

        selected?.let { v ->
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DirtBrown),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = "What ${v.name} fixes / adds",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = v.changelog,
                        color = TextLight,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            MinecraftButton(
                text = if (downloading) "Downloading…" else "Download & Install",
                onClick = {
                    // Real download engine goes here – OkHttp + PackageInstaller
                    downloading = true
                },
                primary = true
            )
        }
    }
}

@Composable
fun VersionCard(version: McVersion, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) AccentGold else ButtonBorder
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) DirtBrown else DirtDark.copy(alpha = 0.8f))
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = version.name,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    fontSize = 16.sp
                )
                Text(
                    text = "${version.type.uppercase()} · ${version.date} · ${version.sizeMb} MB",
                    color = TextLight.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            if (version.type == "release") {
                Text("STABLE", color = GrassGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("BETA", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
