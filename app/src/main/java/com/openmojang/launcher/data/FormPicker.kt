package com.openmojang.launcher.data

object FormPicker {

    fun pickForm(forms: List<String>, abi: AbiPref, music: MusicPref): String {
        if (forms.isEmpty()) throw IllegalStateException("No download forms")
        return forms.maxByOrNull { score(it, abi, music) } ?: forms.first()
    }

    fun pickApkLink(links: List<String>, abi: AbiPref, music: MusicPref): String {
        if (links.isEmpty()) throw IllegalStateException("No APK links")
        if (links.size == 1) return links.first()
        return links.maxByOrNull { score(it, abi, music) } ?: links.first()
    }

    private fun score(text: String, abi: AbiPref, music: MusicPref): Int {
        val l = text.lowercase()
        var s = 0
        when (abi) {
            AbiPref.ARM64 -> {
                if (l.contains("arm64") || l.contains("arm64-v8a") || l.contains("v8a") ||
                    l.contains("64-bit") || l.contains("64bit")
                ) s += 50
                if (l.contains("armeabi") || l.contains("v7a") || l.contains("32-bit") || l.contains("32bit")) s -= 40
            }
            AbiPref.ARM32 -> {
                if (l.contains("armeabi") || l.contains("v7a") || l.contains("32-bit") || l.contains("32bit")) s += 50
                if (l.contains("arm64") || l.contains("v8a") || l.contains("64-bit") || l.contains("64bit")) s -= 40
            }
            AbiPref.AUTO -> {
                if (l.contains("arm64") || l.contains("v8a") || l.contains("64-bit")) s += 30
                if (l.contains("armeabi") || l.contains("v7a")) s -= 10
            }
        }
        val noMusic = l.contains("no music") || l.contains("without music") ||
            l.contains("no-music") || l.contains("nomusic") || l.contains("no_music") ||
            l.contains("w/o music")
        val withMusic = (l.contains("with music") || l.contains("with-music") ||
            l.contains("with_music") || l.contains("music included") || l.contains("+ music")) && !noMusic
        when (music) {
            MusicPref.NO_MUSIC -> {
                if (noMusic) s += 40
                if (withMusic) s -= 30
            }
            MusicPref.WITH_MUSIC -> {
                if (withMusic) s += 40
                if (noMusic) s -= 30
            }
            MusicPref.AUTO -> if (noMusic) s += 15
        }
        return s
    }
}
