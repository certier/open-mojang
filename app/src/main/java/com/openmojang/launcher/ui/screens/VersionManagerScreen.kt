package com.openmojang.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openmojang.launcher.data.ApkInstaller
import com.openmojang.launcher.data.ChangelogProvider
import com.openmojang.launcher.data.InstalledVersions
import com.openmojang.launcher.data.McVersion
import com.openmojang.launcher.data.VersionRepository
import com.openmojang.launcher.ui.MinecraftButton
import com.openmojang.launcher.ui.theme.*

@Composable
fun VersionManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { VersionRepository() }
    val installed = remember { InstalledVersions(context) }
    val apkInstaller = remember { ApkInstaller(context) }

    var versions by remember { mutableStateOf<List<McVersion>>(emptyList()) }
    var selected by remember { mutableStateOf<McVersion?>(null) }
    var status by remember { mutableStateOf("Loading catalog…") }
    var systemVer by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("all") } // all | release | beta

    LaunchedEffect(Unit) {
        versions = repo.getVersions()
        systemVer = installed.systemMinecraftVersion()
        status = buildString {
            append("Catalog · ${versions.size} builds")
            if (systemVer != null) append(" · Installed $systemVer")
            else append(" · Minecraft not installed")
        }
    }

    val filtered = when (filter) {
        "release" -> versions.filter { it.type == "release" }
        "beta" -> versions.filter { it.type == "beta" }
        else -> versions
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
        Spacer(Modifier.height(8.dp))

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("ALL", filter == "all") { filter = "all" }
            FilterChip("STABLE", filter == "release") { filter = "release" }
            FilterChip("BETA", filter == "beta") { filter = "beta" }
        }

        Spacer(Modifier.height(8.dp))

        if (systemVer != null) {
            MinecraftButton(
                text = "Launch Installed Minecraft",
                onClick = {
                    val ok = installed.launchSystemMinecraft()
                    if (!ok) status = "Could not launch Minecraft"
                },
                primary = true
            )
            Spacer(Modifier.height(8.dp))
        }

        if (!apkInstaller.canRequestPackageInstalls()) {
            Text(
                text = "Allow install from this app →",
                color = AccentGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { apkInstaller.openUnknownSourcesSettings() }
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filtered) { v ->
                VersionCard(
                    version = v,
                    selected = selected?.id == v.id,
                    onClick = { selected = v }
                )
            }
        }

        selected?.let { v ->
            Spacer(Modifier.height(12.dp))
            val rich = ChangelogProvider.forVersion(v.id)
            val body = if (rich.startsWith("No detailed")) v.changelog else rich

            Card(
                colors = CardDefaults.cardColors(containerColor = DirtBrown),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
            ) {
                Column(
                    Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "What ${v.name} fixes / adds",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(text = body, color = TextLight, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            MinecraftButton(
                text = "Open Download Page",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(v.downloadUrl))
                    context.startActivity(intent)
                },
                primary = true
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "After download, open the APK from Files or notifications to install.",
                color = TextLight.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) GrassGreen else DirtBrown)
            .border(1.dp, if (selected) GrassDark else ButtonBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
