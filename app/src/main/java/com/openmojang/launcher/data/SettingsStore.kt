package com.openmojang.launcher.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("open_mojang_settings", Context.MODE_PRIVATE)

    var autoCheckUpdates: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    var keepOldVersions: Boolean
        get() = prefs.getBoolean(KEY_KEEP_OLD, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_OLD, value).apply()

    var preferredMirror: String
        get() = prefs.getString(KEY_MIRROR, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_MIRROR, value).apply()

    companion object {
        private const val KEY_AUTO_UPDATE = "auto_update"
        private const val KEY_KEEP_OLD = "keep_old"
        private const val KEY_MIRROR = "mirror"
    }
}
