package com.openmojang.launcher.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openmojang.launcher.data.OptionsEditor

@Composable
fun SetupScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val editor = remember { OptionsEditor(context) }
    var notes by remember {
        mutableStateOf("Allow these once. Maple needs them to download, install, and write options.txt.")
    }
    var filesOn by remember { mutableStateOf(editor.hasFullStorageAccess()) }

    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notes = if (granted) "Notifications allowed"
        else "Notifications denied — downloads still run, the shade just stays quiet"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Maple setup", style = MaterialTheme.typography.headlineMedium)
        Text(
            notes,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notif.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notes = "Notifications are already allowed on this Android version"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Allow download notifications") }

        OutlinedButton(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                    .onFailure { notes = "Open Settings → Apps → Maple → Install unknown apps" }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Allow install from this app") }

        OutlinedButton(
            onClick = {
                editor.openAllFilesSettings()
                notes = "Turn on All files access, come back, then tap Check storage"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (filesOn) "All files access is on" else "Allow all files access") }

        TextButton(
            onClick = {
                filesOn = editor.hasFullStorageAccess()
                notes = if (filesOn) editor.createGameOptions() else "All files access still off"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Check storage and create options.txt") }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        Text(
            "This screen shows once. You can still change things later in Modify and Settings.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
