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
import com.openmojang.launcher.data.MusicPref
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.data.DownloadEngine
import com.openmojang.launcher.data.DownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            job?.cancel()
            stopSelf()
            return START_NOT_STICKY
        }

        val versionId = intent?.getStringExtra(EXTRA_VERSION_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: versionId
        val sizeMb = intent.getIntExtra(EXTRA_SIZE_MB, 0)
        val musicName = intent.getStringExtra(EXTRA_MUSIC) ?: MusicPref.WITH_MUSIC.name
        val musicPref = runCatching { MusicPref.valueOf(musicName) }.getOrDefault(MusicPref.WITH_MUSIC)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "minecraft-$versionId.apk"
        val urls = intent.getStringArrayExtra(EXTRA_URLS)?.toList().orEmpty()

        if (urls.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        ensureChannel()
        val notification = buildNotification(versionName, 0f, -1L, "Starting…")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        running.set(true)
        job = scope.launch {
            val engine = DownloadEngine(applicationContext)
            try {
                SettingsStore(applicationContext).preferredMusic = musicPref
                engine.purgeIncomplete()
                val file = engine.downloadFirst(urls, fileName, sizeMb) { p ->
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(
                        NOTIF_ID,
                        buildNotification(
                            versionName,
                            p.fraction,
                            p.bytesTotal,
                            formatProgress(p)
                        )
                    )
                    sendProgress(versionId, versionName, p)
                }
                sendDone(versionId, file.absolutePath, file.length())
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(
                    NOTIF_ID,
                    buildNotification(versionName, 1f, file.length(), "Done · ${file.length() / (1024 * 1024)} MB")
                )
            } catch (e: CancellationException) {
                sendError(versionId, "Cancelled")
            } catch (e: Exception) {
                sendError(versionId, e.message ?: "Download failed")
            } finally {
                running.set(false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun formatProgress(p: DownloadProgress): String {
        val readMb = p.bytesRead / (1024.0 * 1024.0)
        return if (p.bytesTotal > 0) {
            val totalMb = p.bytesTotal / (1024.0 * 1024.0)
            "%.1f / %.1f MB".format(readMb, totalMb)
        } else {
            "%.1f MB".format(readMb)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(
        title: String,
        fraction: Float,
        total: Long,
        text: String
    ): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(fraction < 1f)
        if (total > 0 && fraction in 0f..1f) {
            b.setProgress(1000, (fraction * 1000).toInt().coerceIn(0, 1000), false)
        } else if (fraction < 1f) {
            b.setProgress(0, 0, true)
        }
        return b.build()
    }

    private fun sendProgress(versionId: String, name: String, p: DownloadProgress) {
        sendBroadcast(
            Intent(ACTION_PROGRESS).apply {
                setPackage(packageName)
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_VERSION_NAME, name)
                putExtra(EXTRA_FRACTION, p.fraction)
                putExtra(EXTRA_BYTES_READ, p.bytesRead)
                putExtra(EXTRA_BYTES_TOTAL, p.bytesTotal)
                putExtra(EXTRA_SPEED_BPS, p.speedBps)
            }
        )
    }

    private fun sendDone(versionId: String, path: String, bytes: Long) {
        sendBroadcast(
            Intent(ACTION_DONE).apply {
                setPackage(packageName)
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_PATH, path)
                putExtra(EXTRA_BYTES_TOTAL, bytes)
            }
        )
    }

    private fun sendError(versionId: String, message: String) {
        sendBroadcast(
            Intent(ACTION_ERROR).apply {
                setPackage(packageName)
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_MESSAGE, message)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "maple_downloads"
        const val NOTIF_ID = 42
        const val ACTION_PROGRESS = "com.openmojang.launcher.DOWNLOAD_PROGRESS"
        const val ACTION_DONE = "com.openmojang.launcher.DOWNLOAD_DONE"
        const val ACTION_ERROR = "com.openmojang.launcher.DOWNLOAD_ERROR"
        const val ACTION_CANCEL = "com.openmojang.launcher.DOWNLOAD_CANCEL"
        const val EXTRA_VERSION_ID = "version_id"
        const val EXTRA_VERSION_NAME = "version_name"
        const val EXTRA_SIZE_MB = "size_mb"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_URLS = "urls"
        const val EXTRA_MUSIC = "music"
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
            urls: List<String>,
            music: MusicPref = MusicPref.WITH_MUSIC
        ) {
            val i = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_VERSION_ID, versionId)
                putExtra(EXTRA_VERSION_NAME, versionName)
                putExtra(EXTRA_SIZE_MB, sizeMb)
                putExtra(EXTRA_FILE_NAME, "minecraft-$versionId.apk")
                putExtra(EXTRA_URLS, urls.toTypedArray())
                putExtra(EXTRA_MUSIC, music.name)
            }
            ContextCompat.startForegroundService(context, i)
        }
    }
}
