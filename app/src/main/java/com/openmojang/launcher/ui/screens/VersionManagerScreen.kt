package com.openmojang.launcher.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.data.DownloadEngine
import com.openmojang.launcher.data.McVersion
import com.openmojang.launcher.data.VersionRepository
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun VersionManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { VersionRepository() }
    val engine = remember { DownloadEngine(context) }
    val scope = rememberCoroutineScope()

    var versions by remember { mutableStateOf<List<McVersion>>(emptyList()) }
    var selected by remember { mutableStateOf<McVersion?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("Loading catalog…") }

    LaunchedEffect(Unit) {
        versions = repo.getVersions()
        status = "Ready · ${versions.size} builds"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DirtDark)
            .padding(16.dp)
    ) {
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

        Spacer(Modifier.height(12.dp))
        Text(text = status, color = TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
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
                    Text(text = v.changelog, color = TextLight, fontSize = 13.sp)
                }
            }

            if (downloading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = GrassGreen,
                    trackColor = DirtBrown
                )
            }

            Spacer(Modifier.height(12.dp))
            MinecraftButton(
                text = when {
                    downloading -> "Downloading ${(progress * 100).toInt()}%"
                    else -> "Open Download Page"
                },
                onClick = {
                    // Open the mirror page (direct APK URLs rotate; page is stable)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(v.downloadUrl))
                    context.startActivity(intent)
                },
                primary = true
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tip: after download finishes, open the APK from notifications or Files to install.",
                color = TextLight.copy(alpha = 0.5f),
                fontSize = 11.sp
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
