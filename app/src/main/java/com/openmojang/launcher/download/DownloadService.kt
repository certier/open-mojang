package com.openmojang.launcher.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.openmojang.launcher.MainActivity
import com.openmojang.launcher.data.DownloadEngine
import com.openmojang.launcher.data.DownloadProgress
import com.openmojang.launcher.data.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var activeFileName: String? = null
    private var activeVersionId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            job?.cancel()
            deletePartials(activeFileName)
            DownloadEngine(applicationContext).purgeIncomplete()
            running.set(false)
            activeVersionId = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val versionId = intent?.getStringExtra(EXTRA_VERSION_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: versionId
        val sizeMb = intent.getIntExtra(EXTRA_SIZE_MB, 0)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "minecraft-$versionId.apk"
        val urls = intent.getStringArrayExtra(EXTRA_URLS)?.toList().orEmpty()

        // User double-tapped Get while another download runs
        if (running.get() && activeVersionId != null && activeVersionId != versionId) {
            broadcastError(versionId, "Another download is already running. Cancel it first.")
            return START_NOT_STICKY
        }
        if (running.get() && activeVersionId == versionId) {
            // same version — ignore duplicate start
            return START_NOT_STICKY
        }

        activeFileName = fileName
        activeVersionId = versionId

        if (urls.isEmpty()) {
            broadcastError(versionId, "No download URLs")
            stopSelf()
            return START_NOT_STICKY
        }

        ensureChannel()
        val notification = buildNotification(versionName, 0, 0, 0, 0, indeterminate = true)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        job?.cancel()
        running.set(true)
        job = scope.launch {
            val engine = DownloadEngine(applicationContext)
            val settings = SettingsStore(applicationContext)
            engine.purgeIncomplete()
            try {
                engine.preflightError(sizeMb)?.let { throw IllegalStateException(it) }

                // Use existing good file if present
                engine.cached(fileName)?.let { file ->
                    completeNotification(versionName, file.length(), success = true)
                    broadcastComplete(versionId, file.absolutePath, file.length())
                    if (settings.installAfterDownload) engine.installApk(file)
                    return@launch
                }

                broadcastProgress(versionId, DownloadProgress(0f, 0, 0), versionName)
                val file = engine.downloadFirst(urls, fileName, sizeMb) { p ->
                    updateNotification(versionName, p)
                    broadcastProgress(versionId, p, versionName)
                }

                completeNotification(versionName, file.length(), success = true)
                broadcastComplete(versionId, file.absolutePath, file.length())

                if (settings.installAfterDownload) {
                    try {
                        engine.installApk(file)
                    } catch (ie: Exception) {
                        // download ok; install is separate failure
                        completeNotification(
                            versionName,
                            file.length(),
                            success = true,
                            message = "Downloaded · install failed: ${ie.message}"
                        )
                    }
                }
            } catch (e: CancellationException) {
                deletePartials(fileName)
                engine.purgeIncomplete()
                completeNotification(versionName, 0, success = false, message = "Cancelled")
                broadcastError(versionId, "Download cancelled")
            } catch (e: Exception) {
                deletePartials(fileName)
                engine.purgeIncomplete()
                val msg = e.message ?: "Download failed"
                completeNotification(versionName, 0, success = false, message = msg)
                broadcastError(versionId, msg)
            } finally {
                running.set(false)
                activeVersionId = null
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun deletePartials(fileName: String?) {
        if (fileName.isNullOrBlank()) return
        val dir = File(applicationContext.getExternalFilesDir(null), "Download")
        File(dir, "$fileName.part").delete()
        val final = File(dir, fileName)
        if (final.exists() && final.length() < 5_000_000L) final.delete()
    }

    override fun onDestroy() {
        job?.cancel()
        running.set(false)
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Minecraft APK download progress"
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun cancelIntent(): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply { action = ACTION_CANCEL }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getService(this, 1, intent, flags)
    }

    private fun buildNotification(
        versionName: String,
        percent: Int,
        read: Long,
        total: Long,
        speedBps: Long,
        indeterminate: Boolean
    ): Notification {
        val speedText = if (speedBps > 0) " · ${formatSpeed(speedBps)}" else ""
        val text = when {
            indeterminate -> "Resolving APK…"
            total > 0 -> "$percent% · ${mb(read)} / ${mb(total)} MB$speedText"
            else -> "${mb(read)} MB$speedText"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading Minecraft $versionName")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .addAction(0, "Cancel", cancelIntent())
            .setProgress(
                if (indeterminate || total <= 0) 0 else 100,
                if (indeterminate) 0 else percent.coerceIn(0, 100),
                indeterminate || total <= 0
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification(versionName: String, p: DownloadProgress) {
        val percent = if (p.bytesTotal > 0) (p.fraction * 100).roundToInt() else 0
        val n = buildNotification(
            versionName,
            percent,
            p.bytesRead,
            p.bytesTotal,
            p.speedBps,
            indeterminate = p.bytesRead == 0L
        )
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, n)
    }

    private fun completeNotification(
        versionName: String,
        bytes: Long,
        success: Boolean,
        message: String? = null
    ) {
        val title = if (success) "Download complete" else "Download failed"
        val text = when {
            message != null && success -> message
            success -> "Minecraft $versionName · ${mb(bytes)} MB"
            else -> message ?: "Failed"
        }
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openAppIntent())
            .setProgress(0, 0, false)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID + 1, n)
    }

    private fun broadcastProgress(versionId: String, p: DownloadProgress, name: String) {
        sendBroadcast(
            Intent(ACTION_PROGRESS).setPackage(packageName).apply {
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_VERSION_NAME, name)
                putExtra(EXTRA_FRACTION, p.fraction)
                putExtra(EXTRA_BYTES_READ, p.bytesRead)
                putExtra(EXTRA_BYTES_TOTAL, p.bytesTotal)
                putExtra(EXTRA_SPEED_BPS, p.speedBps)
            }
        )
    }

    private fun broadcastComplete(versionId: String, path: String, bytes: Long) {
        sendBroadcast(
            Intent(ACTION_COMPLETE).setPackage(packageName).apply {
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_PATH, path)
                putExtra(EXTRA_BYTES_TOTAL, bytes)
            }
        )
    }

    private fun broadcastError(versionId: String, message: String) {
        sendBroadcast(
            Intent(ACTION_ERROR).setPackage(packageName).apply {
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_MESSAGE, message)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "open_mojang_downloads"
        const val NOTIF_ID = 42001

        const val ACTION_PROGRESS = "com.openmojang.launcher.DOWNLOAD_PROGRESS"
        const val ACTION_COMPLETE = "com.openmojang.launcher.DOWNLOAD_COMPLETE"
        const val ACTION_ERROR = "com.openmojang.launcher.DOWNLOAD_ERROR"
        const val ACTION_CANCEL = "com.openmojang.launcher.DOWNLOAD_CANCEL"

        const val EXTRA_VERSION_ID = "version_id"
        const val EXTRA_VERSION_NAME = "version_name"
        const val EXTRA_SIZE_MB = "size_mb"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_URLS = "urls"
        const val EXTRA_FRACTION = "fraction"
        const val EXTRA_BYTES_READ = "bytes_read"
        const val EXTRA_BYTES_TOTAL = "bytes_total"
        const val EXTRA_SPEED_BPS = "speed_bps"
        const val EXTRA_PATH = "path"
        const val EXTRA_MESSAGE = "message"

        private val running = AtomicBoolean(false)

        fun isRunning(): Boolean = running.get()

        fun start(
            context: Context,
            versionId: String,
            versionName: String,
            sizeMb: Int,
            urls: List<String>
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_VERSION_NAME, versionName)
                putExtra(EXTRA_SIZE_MB, sizeMb)
                putExtra(EXTRA_FILE_NAME, "minecraft-$versionId.apk")
                putExtra(EXTRA_URLS, urls.toTypedArray())
            }
            ContextCompat.startForegroundService(context, intent)
        }

        private fun mb(bytes: Long): String =
            if (bytes <= 0L) "0.0" else "%.1f".format(bytes / (1024.0 * 1024.0))

        private fun formatSpeed(bps: Long): String {
            if (bps <= 0) return ""
            val kb = bps / 1024.0
            return if (kb >= 1024) "%.1f MB/s".format(kb / 1024.0)
            else "%.0f KB/s".format(kb)
        }
    }
}
