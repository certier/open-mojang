package com.openmojang.launcher.data

import android.content.Context
import java.io.File

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AbiPref { AUTO, ARM64, ARM32 }
enum class MirrorPref { AUTO, MCPEDL, MCPE_PLANET }
/** Mirrors often ship two APKs: full (with music) and slim (no music). */
enum class MusicPref { AUTO, WITH_MUSIC, NO_MUSIC }

class SettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("open_mojang_settings", Context.MODE_PRIVATE)

    var autoCheckUpdates: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    var keepOldVersions: Boolean
        get() = prefs.getBoolean(KEY_KEEP_OLD, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_OLD, value).apply()

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

    /** Prefer no-music by default — smaller download on mobile. */
    var preferredMusic: MusicPref
        get() = runCatching {
            MusicPref.valueOf(prefs.getString(KEY_MUSIC, MusicPref.NO_MUSIC.name) ?: MusicPref.NO_MUSIC.name)
        }.getOrDefault(MusicPref.NO_MUSIC)
        set(value) = prefs.edit().putString(KEY_MUSIC, value.name).apply()

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var installAfterDownload: Boolean
        get() = prefs.getBoolean(KEY_INSTALL_AFTER, true)
        set(value) = prefs.edit().putBoolean(KEY_INSTALL_AFTER, value).apply()

    var wifiOnlyDownloads: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    fun downloadDir(): File =
        File(appContext.getExternalFilesDir(null), "Download").apply { mkdirs() }

    fun cacheBytes(): Long {
        val dir = downloadDir()
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
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
