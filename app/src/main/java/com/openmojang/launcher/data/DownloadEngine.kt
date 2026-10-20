package com.openmojang.launcher.data

import android.content.Context
import android.content.Intent
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val fraction: Float,
    val bytesRead: Long,
    val bytesTotal: Long
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
        .build()

    init {
        purgeIncomplete()
    }

    /** Only return a finished, valid APK. Incomplete / bad files are deleted. */
    fun cached(fileName: String): File? {
        val dir = downloadDir()
        // kill leftover partials for this name
        File(dir, "$fileName.part").delete()
        val file = File(dir, fileName)
        if (!file.exists()) return null
        if (!isGoodApk(file)) {
            file.delete()
            return null
        }
        return file
    }

    /** Remove every .part and any final file that fails APK checks. */
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

    suspend fun downloadFirst(
        urls: List<String>,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): File {
        // Never keep a half-written final name from a previous crash
        File(downloadDir(), "$fileName.part").delete()
        val existing = File(downloadDir(), fileName)
        if (existing.exists() && !isGoodApk(existing)) existing.delete()

        val errors = mutableListOf<String>()
        val directFirst = urls.filter { it.contains(".apk", ignoreCase = true) } +
            urls.filter { !it.contains(".apk", ignoreCase = true) }

        for (url in directFirst.distinct()) {
            try {
                return downloadApk(url, fileName, onProgress)
            } catch (e: Exception) {
                // ensure no partial survives a failed mirror
                File(downloadDir(), "$fileName.part").delete()
                errors += "${url.take(64)} → ${e.message}"
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
                throw IllegalStateException("APK HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty APK body")
            val type = response.header("Content-Type").orEmpty()
            if (type.contains("text/html", ignoreCase = true)) {
                throw IllegalStateException("Got HTML instead of APK")
            }
            val total = body.contentLength()
            val dir = downloadDir()
            val partFile = File(dir, "$fileName.part")
            val finalFile = File(dir, fileName)
            if (partFile.exists()) partFile.delete()

            try {
                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
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
                        output.flush()
                    }
                }

                if (!isGoodApk(partFile)) {
                    partFile.delete()
                    throw IllegalStateException(
                        "Incomplete or invalid APK (${partFile.length()} B)"
                    )
                }

                // Promote only after validation
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
                // never leave a bad final from this attempt
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
                n == 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
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

        APK_ABS.find(pageHtml)?.value?.let { return it }
        FILE_MCPEDL.find(pageHtml)?.value?.let { return it }

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

        throw IllegalStateException("No APK resolver matched")
    }

    private fun pickForm(forms: List<String>): String {
        val abi = settings.preferredAbi
        return when (abi) {
            AbiPref.ARM64 -> forms.firstOrNull { f ->
                val l = f.lowercase()
                (l.contains("arm64") || (l.contains("no music") && !l.contains("armeabi"))) &&
                    !l.contains("armeabi-v7a")
            } ?: forms.first()
            AbiPref.ARM32 -> forms.firstOrNull { f ->
                f.lowercase().contains("armeabi")
            } ?: forms.first()
            AbiPref.AUTO -> forms.firstOrNull { f ->
                val l = f.lowercase()
                (l.contains("arm64") || l.contains("no music")) && !l.contains("armeabi-v7a")
            } ?: forms.first()
        }
    }

    private fun looksLikeApkUrl(url: String): Boolean {
        return url.contains(".apk", ignoreCase = true) &&
            url.startsWith("http") && !url.contains('<')
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
            if (looksLikeApkUrl(finalUrl) ||
                type.contains("package-archive") ||
                type.contains("octet-stream")
            ) return finalUrl
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
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
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
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

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
