package com.openmojang.launcher.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val fraction: Float,
    val bytesRead: Long,
    val bytesTotal: Long,
    val speedBps: Long = 0L
)

class DownloadEngine(private val context: Context) {

    private val settings = SettingsStore(context)
    private val memoryCookies = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val list = memoryCookies.getOrPut(url.host) { mutableListOf() }
                synchronized(list) {
                    for (c in cookies) {
                        list.removeAll { it.name == c.name }
                        list.add(c)
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val list = memoryCookies[url.host] ?: return emptyList()
                synchronized(list) {
                    return list.filter { it.matches(url) }
                }
            }
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    init {
        purgeIncomplete()
    }

    fun cached(fileName: String): File? {
        val dir = downloadDir()
        File(dir, "$fileName.part").delete()
        val file = File(dir, fileName)
        if (!file.exists()) return null
        if (!isGoodApk(file)) {
            file.delete()
            return null
        }
        return file
    }

    fun purgeIncomplete(): Int {
        val dir = downloadDir()
        var n = 0
        dir.listFiles()?.forEach { f ->
            when {
                f.name.endsWith(".part") -> {
                    if (f.delete()) n++
                }
                f.name.endsWith(".apk", ignoreCase = true) && !isGoodApk(f) -> {
                    if (f.delete()) n++
                }
            }
        }
        return n
    }

    fun freeSpaceBytes(): Long {
        return try {
            val path = downloadDir().absolutePath
            val stat = StatFs(path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    fun hasNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun preflightError(sizeMb: Int): String? {
        if (!hasNetwork()) return "No internet connection"
        if (settings.wifiOnlyDownloads && !isOnWifi()) {
            return "Wi‑Fi only is on. Connect to Wi‑Fi or turn it off in Settings."
        }
        val need = (sizeMb.coerceAtLeast(400) + 50) * 1024L * 1024L
        val free = freeSpaceBytes()
        if (free < need) {
            val freeMb = free / (1024 * 1024)
            return "Not enough storage · ~${freeMb} MB free, need ~${sizeMb + 50} MB"
        }
        return null
    }

    suspend fun downloadFirst(
        urls: List<String>,
        fileName: String,
        expectedSizeMb: Int = 0,
        onProgress: (DownloadProgress) -> Unit = {}
    ): File {
        cached(fileName)?.let { return it }
        preflightError(expectedSizeMb)?.let { throw IllegalStateException(it) }
        File(downloadDir(), "$fileName.part").delete()
        val existing = File(downloadDir(), fileName)
        if (existing.exists() && !isGoodApk(existing)) existing.delete()
        val errors = mutableListOf<String>()
        val ordered = urls.filter { it.contains(".apk", ignoreCase = true) } +
            urls.filter { !it.contains(".apk", ignoreCase = true) }
        for (url in ordered.distinct()) {
            try {
                return downloadApk(url, fileName, expectedSizeMb, onProgress)
            } catch (e: Exception) {
                File(downloadDir(), "$fileName.part").delete()
                errors += friendlyError(e)
                onProgress(DownloadProgress(0f, 0, 0))
            }
        }
        val summary = when {
            errors.isEmpty() -> "No download URL worked"
            errors.size == 1 -> errors.first()
            else -> "All mirrors failed:\n" + errors.take(3).joinToString("\n")
        }
        throw IllegalStateException(summary)
    }

    suspend fun downloadApk(
        pageOrApkUrl: String,
        fileName: String,
        expectedSizeMb: Int = 0,
        onProgress: (DownloadProgress) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        var last: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val apkUrl = resolveApkUrl(pageOrApkUrl)
                return@withContext streamToFile(apkUrl, fileName, expectedSizeMb, onProgress)
            } catch (e: Exception) {
                last = e
                File(downloadDir(), "$fileName.part").delete()
                if (!isRetryable(e) || attempt == MAX_ATTEMPTS - 1) throw e
                delay(1000L * (attempt + 1))
            }
        }
        throw last ?: IllegalStateException("Download failed")
    }

    private fun isRetryable(e: Exception): Boolean {
        return e is SocketTimeoutException ||
            e is UnknownHostException ||
            e is IOException &&
            (e.message?.contains("reset", true) == true ||
                e.message?.contains("timeout", true) == true ||
                e.message?.contains("Unavailable", true) == true)
    }

    private fun friendlyError(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            e is UnknownHostException -> "DNS failed · check connection"
            e is SocketTimeoutException -> "Timed out · try again"
            m.contains("HTML", true) -> "Mirror returned a web page, not an APK"
            m.contains("HTTP 403") -> "Mirror blocked the download (403)"
            m.contains("HTTP 404") -> "APK not found on mirror (404)"
            m.contains("HTTP 5") -> "Mirror server error"
            m.contains("storage", true) || m.contains("space", true) -> m
            m.contains("too small", true) || m.contains("invalid", true) -> m
            m.length > 120 -> m.take(120) + "…"
            else -> m.ifBlank { e.javaClass.simpleName }
        }
    }

    private fun streamToFile(
        apkUrl: String,
        fileName: String,
        expectedSizeMb: Int,
        onProgress: (DownloadProgress) -> Unit
    ): File {
        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://mcpedl.org/")
            .header("Accept", "*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("APK HTTP ${response.code}")
            val body = response.body ?: throw IllegalStateException("Empty APK body")
            val type = response.header("Content-Type").orEmpty()
            if (type.contains("text/html", ignoreCase = true)) {
                throw IllegalStateException("Got HTML instead of APK")
            }
            var total = body.contentLength()
            if (total <= 0 && expectedSizeMb > 0) total = expectedSizeMb * 1024L * 1024L
            if (total > 0) {
                val free = freeSpaceBytes()
                if (free < total + 40L * 1024 * 1024) {
                    throw IllegalStateException("Not enough storage · need ~${total / (1024 * 1024)} MB free")
                }
            }
            val dir = downloadDir()
            val partFile = File(dir, "$fileName.part")
            val finalFile = File(dir, fileName)
            if (partFile.exists()) partFile.delete()
            try {
                val startNs = System.nanoTime()
                var lastEmitNs = 0L
                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.nanoTime()
                            if (now - lastEmitNs > 250_000_000L || downloaded == total) {
                                lastEmitNs = now
                                val elapsed = ((now - startNs) / 1_000_000_000.0).coerceAtLeast(0.001)
                                val speed = (downloaded / elapsed).toLong()
                                val fraction = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                                onProgress(DownloadProgress(fraction, downloaded, total, speed))
                            }
                        }
                        output.flush()
                    }
                }
                if (!isGoodApk(partFile)) {
                    val len = partFile.length()
                    partFile.delete()
                    throw IllegalStateException("Incomplete or invalid APK (${len / (1024 * 1024)} MB)")
                }
                if (expectedSizeMb > 0) {
                    val expected = expectedSizeMb * 1024L * 1024L
                    val actual = partFile.length()
                    val minOk = (expected * 0.55).toLong()
                    if (actual < minOk) {
                        partFile.delete()
                        throw IllegalStateException("File too small: ${actual / (1024 * 1024)} MB (expected ~$expectedSizeMb MB)")
                    }
                }
                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    partFile.copyTo(finalFile, overwrite = true)
                    partFile.delete()
                }
                if (!isGoodApk(finalFile)) {
                    finalFile.delete()
                    throw IllegalStateException("Final APK failed validation")
                }
                return finalFile
            } catch (e: Exception) {
                partFile.delete()
                if (finalFile.exists() && !isGoodApk(finalFile)) finalFile.delete()
                throw e
            }
        }
    }

    private fun isGoodApk(file: File): Boolean {
        if (!file.exists() || file.length() < MIN_APK_BYTES) return false
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val n = input.read(header)
                n == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveApkUrl(pageOrApkUrl: String): String {
        if (looksLikeApkUrl(pageOrApkUrl)) return pageOrApkUrl
        DIRECT_APK[pageOrApkUrl]?.let { return it }
        for ((key, direct) in DIRECT_APK) {
            if (pageOrApkUrl.contains(key)) return direct
        }
        val pageHtml = getHtml(pageOrApkUrl)
        if (looksLikeApkUrl(pageHtml) && pageHtml.length < 500) return pageHtml.trim()
        val absLinks = APK_ABS.findAll(pageHtml).map { it.value }.distinct().toList()
        if (absLinks.isNotEmpty()) {
            return FormPicker.pickApkLink(absLinks, settings.preferredAbi, settings.preferredMusic)
        }
        val fileLinks = FILE_MCPEDL.findAll(pageHtml).map { it.value }.distinct().toList()
        if (fileLinks.isNotEmpty()) {
            return FormPicker.pickApkLink(fileLinks, settings.preferredAbi, settings.preferredMusic)
        }
        val forms = FORM_BLOCK.findAll(pageHtml).map { it.value }.toList()
        if (forms.isNotEmpty()) {
            val preferred = pickForm(forms)
            val fileId = FILE_ID.find(preferred)?.groupValues?.get(1)
                ?: throw IllegalStateException("No file_id in form")
            val postTitle = POST_TITLE.find(preferred)?.groupValues?.get(1) ?: "Minecraft APK"
            val postUrl = POST_URL.find(preferred)?.groupValues?.get(1) ?: pageOrApkUrl
            val gateHtml = postShowFile(fileId, postTitle, postUrl)
            DOWNLOAD_URL_JS.find(gateHtml)?.groupValues?.get(1)?.let { return it }
            APK_ABS.find(gateHtml)?.value?.let { return it }
            FILE_MCPEDL.find(gateHtml)?.value?.let { return it }
            throw IllegalStateException("show_file gate had no APK (file_id=$fileId)")
        }
        APK_HREF.find(pageHtml)?.groupValues?.get(1)?.let { link ->
            return if (link.startsWith("http")) link else "https://mcpedl.org$link"
        }
        throw IllegalStateException("No APK link found on page")
    }

    private fun pickForm(forms: List<String>): String {
        return FormPicker.pickForm(forms, settings.preferredAbi, settings.preferredMusic)
    }

    private fun looksLikeApkUrl(url: String): Boolean {
        return url.contains(".apk", ignoreCase = true) && url.startsWith("http") && !url.contains('<')
    }

    private fun getHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Page HTTP ${response.code}")
            val type = response.header("Content-Type").orEmpty()
            val finalUrl = response.request.url.toString()
            if (looksLikeApkUrl(finalUrl) || type.contains("package-archive") || type.contains("octet-stream")) return finalUrl
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
            if (!response.isSuccessful) throw IllegalStateException("show_file HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    fun installApk(apkFile: File) {
        if (!isGoodApk(apkFile)) {
            apkFile.delete()
            throw IllegalStateException("APK is incomplete or corrupt")
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    private fun downloadDir(): File =
        File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }

    companion object {
        private const val MIN_APK_BYTES = 5_000_000L
        private const val MAX_ATTEMPTS = 3
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        private val DIRECT_APK = mapOf(
            "26.52" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk",
            "minecraft-pe-26-52-apk" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk",
        )
        private val APK_HREF = Regex("""href=["']([^"']+\.apk[^"']*)["']""", RegexOption.IGNORE_CASE)
        private val APK_ABS = Regex("""https?://[^"'\s>]+\.apk[^"'\s>]*""", RegexOption.IGNORE_CASE)
        private val FILE_MCPEDL = Regex("""https?://file\.mcpedl\.org/[^"'\s>]+\.apk""", RegexOption.IGNORE_CASE)
        private val DOWNLOAD_URL_JS = Regex("""DOWNLOAD_URL\s*=\s*["'](https?://[^"']+\.apk[^"']*)["']""", RegexOption.IGNORE_CASE)
        private val FORM_BLOCK = Regex("""<form[^>]*action=["']/show_file\.php["'][\s\S]*?</form>""", RegexOption.IGNORE_CASE)
        private val FILE_ID = Regex("""name=["']file_id["'][^>]*value=["'](\d+)["']""", RegexOption.IGNORE_CASE)
        private val POST_TITLE = Regex("""name=["']post_title["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val POST_URL = Regex("""name=["']post_url["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    }
}
