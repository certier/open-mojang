package com.openmojang.launcher.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class DownloadProgress(
    val fraction: Float,
    val bytesRead: Long,
    val bytesTotal: Long,
    val speedBps: Long = 0L
)

class DownloadEngine(private val context: Context) {

    private val settings = SettingsStore(context)
    private val memoryCookies = mutableMapOf<String, MutableList<Cookie>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
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
        .build()

    private val active = AtomicBoolean(false)

    fun cached(fileName: String): File? {
        val f = File(downloadDir(), fileName)
        return if (isGoodApk(f)) f else null
    }

    fun purgeIncomplete() {
        val dir = downloadDir()
        dir.listFiles()?.forEach { f ->
            if (f.name.endsWith(".part") || (f.name.endsWith(".apk") && !isGoodApk(f))) {
                f.delete()
            }
        }
    }

    fun isGoodApk(file: File): Boolean {
        if (!file.isFile) return false
        if (file.length() < MIN_APK_BYTES) return false
        return try {
            file.inputStream().use { ins ->
                val magic = ByteArray(4)
                if (ins.read(magic) != 4) return false
                magic[0] == 0x50.toByte() && magic[1] == 0x4b.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun preflightError(expectedSizeMb: Int): String? {
        if (settings.wifiOnly && !isOnWifi()) {
            return "Wi‑Fi only is on · connect to Wi‑Fi or turn it off in Settings"
        }
        val free = context.getExternalFilesDir(null)?.usableSpace ?: Long.MAX_VALUE
        val need = expectedSizeMb.toLong() * 1024L * 1024L + 20L * 1024L * 1024L
        if (free < need) {
            return "Not enough free space for this download"
        }
        return null
    }

    suspend fun downloadFirst(
        urls: List<String>,
        fileName: String,
        expectedSizeMb: Int,
        onProgress: (DownloadProgress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!active.compareAndSet(false, true)) {
            throw IllegalStateException("Another download is already running")
        }
        try {
            preflightError(expectedSizeMb)?.let { throw IllegalStateException(it) }
            cached(fileName)?.let { return@withContext it }

            val ordered = urls.filter { it.contains(".apk", ignoreCase = true) } +
                urls.filter { !it.contains(".apk", ignoreCase = true) }
            var lastError: Exception? = null
            for (url in ordered.distinct()) {
                try {
                    return@withContext downloadApk(url, fileName, expectedSizeMb, onProgress)
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: IllegalStateException("No working download URL")
        } finally {
            active.set(false)
        }
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun downloadApk(
        pageOrApkUrl: String,
        fileName: String,
        expectedSizeMb: Int,
        onProgress: (DownloadProgress) -> Unit
    ): File {
        val apkUrl = resolveApkUrl(pageOrApkUrl)
        val dest = File(downloadDir(), fileName)
        val part = File(downloadDir(), "$fileName.part")
        if (part.exists()) part.delete()

        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} for APK")
            }
            val body = response.body ?: throw IllegalStateException("Empty body")
            val rawLen = body.contentLength()
            val total = if (rawLen >= 8L * 1024L * 1024L) rawLen else -1L
            var read = 0L
            var lastUi = 0L
            var lastBytes = 0L
            val t0 = System.nanoTime()

            body.byteStream().use { input ->
                FileOutputStream(part).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        val now = System.nanoTime()
                        if (now - lastUi > 200_000_000L) {
                            val dt = (now - t0).coerceAtLeast(1L) / 1_000_000_000.0
                            val speed = ((read - lastBytes) / ((now - lastUi).coerceAtLeast(1L) / 1_000_000_000.0)).toLong()
                            lastBytes = read
                            lastUi = now
                            val frac = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
                            onProgress(DownloadProgress(frac, read, total, speed))
                        }
                    }
                }
            }

            if (!isGoodApk(part)) {
                part.delete()
                throw IllegalStateException("Downloaded file is not a valid APK")
            }
            if (dest.exists()) dest.delete()
            if (!part.renameTo(dest)) {
                part.copyTo(dest, overwrite = true)
                part.delete()
            }
            onProgress(DownloadProgress(1f, dest.length(), dest.length(), 0L))
            return dest
        }
    }

    private fun resolveApkUrl(pageOrApkUrl: String): String {
        if (looksLikeApkUrl(pageOrApkUrl)) return pageOrApkUrl

        DIRECT_APK[pageOrApkUrl.substringAfterLast('/')]?.let { return it }

        val html = getHtml(pageOrApkUrl)
        val absLinks = APK_ABS.findAll(html).map { it.value }.distinct().toList()
        if (absLinks.isNotEmpty()) {
            return FormPicker.pickApkLink(absLinks, settings.preferredAbi, settings.preferredMusic)
        }
        val fileLinks = FILE_MCPEDL.findAll(html).map { it.value }.distinct().toList()
        if (fileLinks.isNotEmpty()) {
            return FormPicker.pickApkLink(fileLinks, settings.preferredAbi, settings.preferredMusic)
        }
        val js = DOWNLOAD_URL_JS.find(html)?.groupValues?.get(1)
        if (js != null) return js

        val forms = FORM_BLOCK.findAll(html).map { it.value }.toList()
        if (forms.isNotEmpty()) {
            val preferred = pickForm(forms)
            val fileId = FILE_ID.find(preferred)?.groupValues?.get(1)
                ?: throw IllegalStateException("No file_id in form")
            val postTitle = POST_TITLE.find(preferred)?.groupValues?.get(1) ?: "Minecraft APK"
            val postUrl = POST_URL.find(preferred)?.groupValues?.get(1) ?: pageOrApkUrl
            return postShowFile(fileId, postTitle, postUrl)
        }

        val href = APK_HREF.find(html)?.groupValues?.get(1)
        if (href != null) {
            return if (href.startsWith("http")) href else "https://mcpedl.org$href"
        }
        throw IllegalStateException("Could not resolve APK from page")
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
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            val finalUrl = response.request.url.toString()
            val body = response.body?.string().orEmpty()
            if (looksLikeApkUrl(finalUrl)) return finalUrl
            return body
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
            .header("Accept", "text/html,*/*")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("show_file HTTP ${response.code}")
            val text = response.body?.string().orEmpty()
            val loc = response.header("Location")
            if (loc != null && looksLikeApkUrl(loc)) return loc
            val abs = APK_ABS.find(text)?.value
            if (abs != null) return abs
            val file = FILE_MCPEDL.find(text)?.value
            if (file != null) return file
            throw IllegalStateException("show_file did not return APK URL")
        }
    }

    fun installApk(apkFile: File) {
        if (!isGoodApk(apkFile)) {
            apkFile.delete()
            throw IllegalStateException("APK is incomplete or corrupt")
        }
        val ok = ApkInstaller(context).install(apkFile)
        if (!ok) throw IllegalStateException("Could not start install")
    }

    private fun downloadDir(): File =
        File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }

    companion object {
        private const val MIN_APK_BYTES = 5_000_000L
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
