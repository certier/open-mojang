package com.openmojang.launcher.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import com.openmojang.launcher.data.ApkInstaller

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirm != null) {
                    try {
                        context.startActivity(confirm)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Open install prompt failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS ->
                Toast.makeText(context, "Minecraft installed", Toast.LENGTH_SHORT).show()
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                Toast.makeText(context, "Install cancelled", Toast.LENGTH_SHORT).show()
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                Toast.makeText(
                    context,
                    "Install conflict · uninstall the other Minecraft first",
                    Toast.LENGTH_LONG
                ).show()
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                Toast.makeText(context, "APK incompatible with this device", Toast.LENGTH_LONG).show()
            PackageInstaller.STATUS_FAILURE_INVALID ->
                Toast.makeText(context, "Invalid APK", Toast.LENGTH_LONG).show()
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                Toast.makeText(context, "Not enough storage to install", Toast.LENGTH_LONG).show()
            else ->
                Toast.makeText(
                    context,
                    message ?: "Install failed ($status)",
                    Toast.LENGTH_LONG
                ).show()
        }

        // Optional local broadcast so UI can refresh "Play" state
        context.sendBroadcast(
            Intent(ApkInstaller.ACTION_INSTALL_RESULT).setPackage(context.packageName).apply {
                putExtra(PackageInstaller.EXTRA_STATUS, status)
                if (message != null) putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, message)
            }
        )
    }
}
