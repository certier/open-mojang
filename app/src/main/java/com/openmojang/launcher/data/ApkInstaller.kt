package com.openmojang.launcher.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.openmojang.launcher.install.InstallResultReceiver
import java.io.File
import java.io.FileInputStream

/**
 * Installs a local APK.
 * Primary path: PackageInstaller session (streamed commit + status callback).
 * Fallback: ACTION_VIEW via FileProvider.
 */
class ApkInstaller(private val context: Context) {

    private val installed = InstalledVersions(context)

    /**
     * @return true if install UI was shown.
     */
    fun install(apk: File, expectedPackage: String = "com.mojang.minecraftpe"): Boolean {
        if (!apk.exists() || apk.length() < 1_000_000L) return false
        return try {
            installWithSession(apk)
        } catch (_: Exception) {
            installWithViewIntent(apk)
        }
    }

    fun isMinecraftInstalled(): Boolean = installed.isSystemMinecraftInstalled()

    fun minecraftVersionName(): String? = installed.systemMinecraftVersion()

    private fun installWithSession(apk: File): Boolean {
        val pm = context.packageManager
        val installer = pm.packageInstaller

        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }
            // Third-party apps must allow the system confirm sheet
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
            setSize(apk.length())
        }

        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                FileInputStream(apk).use { input ->
                    val buf = ByteArray(256 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                    }
                    session.fsync(out)
                }
            }

            val callback = Intent(context, InstallResultReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(context, sessionId, callback, flags)
            session.commit(pi.intentSender)
            return true
        } catch (e: Exception) {
            try {
                session.abandon()
            } catch (_: Exception) {
            }
            throw e
        } finally {
            try {
                session.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun installWithViewIntent(apk: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    companion object {
        const val ACTION_INSTALL_RESULT = "com.openmojang.launcher.INSTALL_RESULT"
    }
}
