package com.openmojang.launcher.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Handles downloading Minecraft Bedrock APKs from the fast public mirrors
 * and launching the system package installer.
 */
class DownloadEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    suspend fun downloadApk(
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

        val body = response.body ?: throw Exception("Empty body")
        val total = body.contentLength().coerceAtLeast(1)

        val dir = File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }
        val outFile = File(dir, fileName)

        body.byteStream().use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                var downloaded = 0L
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded.toFloat() / total)
                }
            }
        }
        outFile
    }

    fun installApk(apkFile: File) {
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
}
