package com.openmojang.launcher.data

import android.content.Context
import java.io.File

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AbiPref { AUTO, ARM64, ARM32 }
enum class MirrorPref { MCPEDL, MCPE_PLANET, AUTO }

/** Mirrors often ship two APKs: full (with music) and slim (no music). */
enum class MusicPref { AUTO, WITH_MUSIC, NO_MUSIC }

class SettingsStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("open_mojang_settings", Context.MODE_PRIVATE)

    var preferredMirror: MirrorPref
        get() = runCatching {
            MirrorPref.valueOf(prefs.getString(KEY_MIRROR, MirrorPref.AUTO.name) ?: MirrorPref.AUTO.name)
        }.getOrDefault(MirrorPref.AUTO)
        set(value) = prefs.edit().putString(KEY_MIRROR, value.name).apply()

    var preferredAbi: AbiPref
        get() = runCatching {
            AbiPref.valueOf(prefs.getString(KEY_ABI, AbiPref.AUTO.name) ?: AbiPref.AUTO.name)
        }.getOrDefault(AbiPref.AUTO)
        set(value) = prefs.edit().putString(KEY_ABI, value.name).apply()

    var preferredMusic: MusicPref
        get() = runCatching {
            MusicPref.valueOf(prefs.getString(KEY_MUSIC, MusicPref.WITH_MUSIC.name) ?: MusicPref.WITH_MUSIC.name)
        }.getOrDefault(MusicPref.WITH_MUSIC)
        set(value) = prefs.edit().putString(KEY_MUSIC, value.name).apply()

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var autoCheckUpdates: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    var keepOldApks: Boolean
        get() = prefs.getBoolean(KEY_KEEP_OLD, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_OLD, value).apply()

    var installAfterDownload: Boolean
        get() = prefs.getBoolean(KEY_INSTALL_AFTER, true)
        set(value) = prefs.edit().putBoolean(KEY_INSTALL_AFTER, value).apply()

    var wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    /** Alias used by older DownloadEngine call sites */
    var wifiOnlyDownloads: Boolean
        get() = wifiOnly
        set(value) { wifiOnly = value }

    fun cacheBytes(): Long {
        val dir = downloadDir()
        if (!dir.exists()) return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearCache(): Int {
        val dir = downloadDir()
        if (!dir.exists()) return 0
        var n = 0
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.delete()) n++
        }
        return n
    }

    private fun downloadDir(): File =
        File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }

    companion object {
        private const val KEY_AUTO_UPDATE = "auto_update"
        private const val KEY_KEEP_OLD = "keep_old"
        private const val KEY_MIRROR = "mirror"
        private const val KEY_ABI = "abi"
        private const val KEY_MUSIC = "music"
        private const val KEY_THEME = "theme"
        private const val KEY_INSTALL_AFTER = "install_after"
        private const val KEY_WIFI_ONLY = "wifi_only"
    }
}
