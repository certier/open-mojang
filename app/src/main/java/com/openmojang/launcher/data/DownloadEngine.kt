package com.openmojang.launcher.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.openmojang.launcher.install.InstallResultReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
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
 * MCPEDL flow:
 * 1) GET version page
 * 2) find <form action="/show_file.php"> with file_id (prefer arm64 / no music)
 * 3) POST show_file.php
 * 4) scrape https://file.mcpedl.org/.../*.apk
 * 5) stream APK to app storage, then PackageInstaller
 */
class DownloadEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    fun cached(fileName: String): File? {
        val file = File(downloadDir(), fileName)
        return if (file.exists() && file.length() > 1_000_000L) file else null
    }

    suspend fun downloadFirst(
        urls: List<String>,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): File {
        var last: Exception? = null
        for (url in urls.distinct()) {
            try {
                return downloadApk(url, fileName, onProgress)
            } catch (e: Exception) {
                last = e
                onProgress(DownloadProgress(0f, 0, 0))
            }
        }
        throw last ?: IllegalStateException("No download URL")
    }

    suspend fun downloadApk(
        pageOrApkUrl: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val apkUrl = resolveApkUrl(pageOrApkUrl)
        streamToFile(apkUrl, fileName, onProgress)
    }

    private fun streamToFile(
        apkUrl: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit
    ): File {
        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://mcpedl.org/")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("APK HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty APK body")
            val total = body.contentLength()
            val outFile = File(downloadDir(), fileName)
            body.byteStream().use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(128 * 1024)
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
            if (outFile.length() < 5_000_000L) {
                outFile.delete()
                throw IllegalStateException("Downloaded file too small to be Minecraft")
            }
            return outFile
        }
    }

    private fun resolveApkUrl(pageOrApkUrl: String): String {
        if (pageOrApkUrl.contains("file.mcpedl.org", ignoreCase = true) &&
            pageOrApkUrl.contains(".apk", ignoreCase = true)
        ) {
            return pageOrApkUrl
        }

        val pageHtml = getHtml(pageOrApkUrl)

        // Direct link already in HTML
        APK_ABS.find(pageHtml)?.value?.let { return it }

        // MCPEDL form: file_id + show_file.php
        val forms = FORM_BLOCK.findAll(pageHtml).map { it.value }.toList()
        if (forms.isNotEmpty()) {
            val preferred = forms.firstOrNull { f ->
                f.contains("arm64", ignoreCase = true) ||
                    (f.contains("no music", ignoreCase = true) && !f.contains("armeabi-v7a", ignoreCase = true))
            } ?: forms.first()

            val fileId = FILE_ID.find(preferred)?.groupValues?.get(1)
                ?: throw IllegalStateException("No file_id on page")
            val postTitle = POST_TITLE.find(preferred)?.groupValues?.get(1)
                ?: "Minecraft APK"
            val postUrl = POST_URL.find(preferred)?.groupValues?.get(1)
                ?: pageOrApkUrl

            val gateHtml = postShowFile(fileId, postTitle, postUrl)
            APK_ABS.find(gateHtml)?.value?.let { return it }
            FILE_MCPEDL.find(gateHtml)?.value?.let { return it }
            throw IllegalStateException("show_file.php returned no APK link")
        }

        APK_HREF.find(pageHtml)?.groupValues?.get(1)?.let { link ->
            return if (link.startsWith("http")) link else "https://mcpedl.org$link"
        }

        throw IllegalStateException("No APK resolver matched this page")
    }

    private fun getHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Page HTTP ${response.code}")
            }
            val type = response.header("Content-Type").orEmpty()
            val finalUrl = response.request.url.toString()
            if (finalUrl.contains(".apk", ignoreCase = true) ||
                type.contains("package-archive") ||
                type.contains("octet-stream")
            ) {
                return finalUrl // caller treats pure apk url via APK_ABS path; rare
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun postShowFile(fileId: String, postTitle: String, postUrl: String): String {
        val body = FormBody.Builder()
            .add("file_id", fileId)
            .add("post_title", postTitle)
            .add("post_url", postUrl)
            .build()
        val request = Request.Builder()
            .url("https://mcpedl.org/show_file.php")
            .header("User-Agent", USER_AGENT)
            .header("Referer", postUrl)
            .header("Origin", "https://mcpedl.org")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("show_file HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
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

    private fun downloadDir(): File =
        File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }

    companion object {
        const val ACTION_INSTALL = "com.openmojang.launcher.INSTALL_STATUS"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        private val APK_HREF = Regex("""href=["']([^"']+\.apk[^"']*)["']""", RegexOption.IGNORE_CASE)
        private val APK_ABS = Regex("""https?://[^"'\s>]+\.apk[^"'\s>]*""", RegexOption.IGNORE_CASE)
        private val FILE_MCPEDL = Regex("""https?://file\.mcpedl\.org/[^"'\s>]+\.apk""", RegexOption.IGNORE_CASE)
        private val FORM_BLOCK = Regex(
            """<form[^>]*action=["']/show_file\.php["'][\s\S]*?</form>""",
            RegexOption.IGNORE_CASE
        )
        private val FILE_ID = Regex("""name=["']file_id["'][^>]*value=["'](\d+)["']""", RegexOption.IGNORE_CASE)
        private val POST_TITLE = Regex("""name=["']post_title["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val POST_URL = Regex("""name=["']post_url["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    }
}
