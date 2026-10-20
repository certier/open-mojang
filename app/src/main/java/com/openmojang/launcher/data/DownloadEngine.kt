package com.openmojang.launcher.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.openmojang.launcher.install.InstallResultReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val fraction: Float,
    val bytesRead: Long,
    val bytesTotal: Long
)

/**
 * Pulls the APK inside the app (follows redirects, scrapes a page for the
 * first .apk href when the catalog only has a version page) then commits a
 * PackageInstaller session so Android shows the same install sheet Play uses.
 */
class DownloadEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun downloadApk(
        pageOrApkUrl: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val apkUrl = resolveApkUrl(pageOrApkUrl)
        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty body")
            val total = body.contentLength()
            val dir = File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }
            val outFile = File(dir, fileName)
            body.byteStream().use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val fraction = if (total > 0) downloaded.toFloat() / total else 0f
                        onProgress(DownloadProgress(fraction, downloaded, total))
                    }
                }
            }
            if (outFile.length() < 1_000_000L) {
                outFile.delete()
                throw IllegalStateException("File is not a Minecraft APK (too small). Mirror blocked the direct link.")
            }
            outFile
        }
    }

    private fun resolveApkUrl(pageOrApkUrl: String): String {
        val head = Request.Builder()
            .url(pageOrApkUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/vnd.android.package-archive,*/*")
            .build()
        client.newCall(head).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Could not open download page: HTTP ${response.code}")
            }
            val finalUrl = response.request.url.toString()
            val type = response.header("Content-Type").orEmpty()
            if (finalUrl.contains(".apk", ignoreCase = true) ||
                type.contains("package-archive") ||
                type.contains("octet-stream")
            ) {
                return finalUrl
            }
            val html = response.body?.string().orEmpty()
            val link = APK_HREF.find(html)?.groupValues?.get(1)
                ?: APK_ABS.find(html)?.value
                ?: throw IllegalStateException("No direct APK on that page yet")
            return if (link.startsWith("http")) link else response.request.url.resolve(link).toString()
        }
    }

    fun installApk(apkFile: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName("com.mojang.minecraftpe")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("minecraft.apk", 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { input -> input.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val callback = Intent(context, InstallResultReceiver::class.java).apply {
                action = ACTION_INSTALL
            }
            val pending = PendingIntent.getBroadcast(context, sessionId, callback, flags)
            session.commit(pending.intentSender)
        }
    }

    companion object {
        const val ACTION_INSTALL = "com.openmojang.launcher.INSTALL_STATUS"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        private val APK_HREF = Regex("""href=["']([^"']+\.apk[^"']*)["']""", RegexOption.IGNORE_CASE)
        private val APK_ABS = Regex("""https?://[^"'\s>]+\.apk[^"'\s>]*""", RegexOption.IGNORE_CASE)
    }
}
