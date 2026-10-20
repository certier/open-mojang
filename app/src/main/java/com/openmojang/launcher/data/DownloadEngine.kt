package com.openmojang.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val fraction: Float,
    val bytesRead: Long,
    val bytesTotal: Long
)

/**
 * Full in-app Minecraft Bedrock APK download.
 *
 * Order:
 * 1) Known direct file.mcpedl.org URLs (no scrape)
 * 2) Version page → show_file.php form → gate HTML → DOWNLOAD_URL / .apk link
 * 3) Stream to app storage with progress, then system install sheet
 */
class DownloadEngine(private val context: Context) {

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun cached(fileName: String): File? {
        val file = File(downloadDir(), fileName)
        return if (file.exists() && file.length() > 5_000_000L) file else null
    }

    suspend fun downloadFirst(
        urls: List<String>,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): File {
        val errors = mutableListOf<String>()

        // Prefer known direct APK mirrors first (skip HTML scrape)
        val directFirst = urls.filter { it.contains(".apk", ignoreCase = true) } +
            urls.filter { !it.contains(".apk", ignoreCase = true) }

        for (url in directFirst.distinct()) {
            try {
                return downloadApk(url, fileName, onProgress)
            } catch (e: Exception) {
                errors += "${url.take(60)}… → ${e.message}"
                onProgress(DownloadProgress(0f, 0, 0))
            }
        }
        throw IllegalStateException(
            errors.joinToString("\n").ifEmpty { "No download URL worked" }
        )
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
            .header("Accept", "*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("APK HTTP ${response.code} for $apkUrl")
            }
            val body = response.body ?: throw IllegalStateException("Empty APK body")
            val type = response.header("Content-Type").orEmpty()
            // Reject HTML gate pages that slipped through
            if (type.contains("text/html", ignoreCase = true)) {
                throw IllegalStateException("Got HTML instead of APK from $apkUrl")
            }
            val total = body.contentLength()
            val outFile = File(downloadDir(), fileName)
            if (outFile.exists()) outFile.delete()
            body.byteStream().use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(256 * 1024)
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
                throw IllegalStateException(
                    "File too small (${outFile.length()} B) — not a Minecraft APK"
                )
            }
            return outFile
        }
    }

    private fun resolveApkUrl(pageOrApkUrl: String): String {
        // Already a direct APK
        if (looksLikeApkUrl(pageOrApkUrl)) {
            return pageOrApkUrl
        }

        // Known hardcoded mirrors for current builds
        DIRECT_APK[pageOrApkUrl]?.let { return it }
        for ((key, direct) in DIRECT_APK) {
            if (pageOrApkUrl.contains(key)) return direct
        }

        val pageHtml = getHtml(pageOrApkUrl)

        // Absolute .apk already in page HTML
        APK_ABS.find(pageHtml)?.value?.let { return it }
        FILE_MCPEDL.find(pageHtml)?.value?.let { return it }

        // MCPEDL forms → show_file.php
        val forms = FORM_BLOCK.findAll(pageHtml).map { it.value }.toList()
        if (forms.isNotEmpty()) {
            val preferred = forms.firstOrNull { f ->
                val label = f.lowercase()
                (label.contains("arm64") || label.contains("no music")) &&
                    !label.contains("armeabi-v7a")
            } ?: forms.first()

            val fileId = FILE_ID.find(preferred)?.groupValues?.get(1)
                ?: throw IllegalStateException("No file_id in form")
            val postTitle = POST_TITLE.find(preferred)?.groupValues?.get(1)
                ?: "Minecraft APK"
            val postUrl = POST_URL.find(preferred)?.groupValues?.get(1)
                ?: pageOrApkUrl

            val gateHtml = postShowFile(fileId, postTitle, postUrl)

            // JS var DOWNLOAD_URL = "https://file.mcpedl.org/...apk"
            DOWNLOAD_URL_JS.find(gateHtml)?.groupValues?.get(1)?.let { return it }
            APK_ABS.find(gateHtml)?.value?.let { return it }
            FILE_MCPEDL.find(gateHtml)?.value?.let { return it }

            throw IllegalStateException("show_file.php gate had no APK link (file_id=$fileId)")
        }

        APK_HREF.find(pageHtml)?.groupValues?.get(1)?.let { link ->
            return if (link.startsWith("http")) link else "https://mcpedl.org$link"
        }

        throw IllegalStateException("No APK resolver matched: $pageOrApkUrl")
    }

    private fun looksLikeApkUrl(url: String): Boolean {
        return url.contains(".apk", ignoreCase = true) &&
            (url.contains("file.mcpedl.org", ignoreCase = true) ||
                url.endsWith(".apk", ignoreCase = true))
    }

    private fun getHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Page HTTP ${response.code}")
            }
            val type = response.header("Content-Type").orEmpty()
            val finalUrl = response.request.url.toString()
            if (looksLikeApkUrl(finalUrl) ||
                type.contains("package-archive") ||
                type.contains("octet-stream")
            ) {
                // Redirect landed on binary — treat as APK url
                return finalUrl
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
            .header("Accept", "text/html,*/*")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("show_file HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    /** System install sheet — same path users see from Downloads / Play sideload. */
    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
        }
        context.startActivity(intent)
    }

    private fun downloadDir(): File =
        File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

        /** Verified direct APKs — skip scrape when these match. */
        private val DIRECT_APK = mapOf(
            "26.52" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk",
            "minecraft-pe-26-52-apk" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk",
        )

        private val APK_HREF = Regex("""href=["']([^"']+\.apk[^"']*)["']""", RegexOption.IGNORE_CASE)
        private val APK_ABS = Regex("""https?://[^"'\s>]+\.apk[^"'\s>]*""", RegexOption.IGNORE_CASE)
        private val FILE_MCPEDL = Regex("""https?://file\.mcpedl\.org/[^"'\s>]+\.apk""", RegexOption.IGNORE_CASE)
        private val DOWNLOAD_URL_JS = Regex(
            """DOWNLOAD_URL\s*=\s*["'](https?://[^"']+\.apk[^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        private val FORM_BLOCK = Regex(
            """<form[^>]*action=["']/show_file\.php["'][\s\S]*?</form>""",
            RegexOption.IGNORE_CASE
        )
        private val FILE_ID = Regex("""name=["']file_id["'][^>]*value=["'](\d+)["']""", RegexOption.IGNORE_CASE)
        private val POST_TITLE = Regex("""name=["']post_title["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val POST_URL = Regex("""name=["']post_url["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    }
}
