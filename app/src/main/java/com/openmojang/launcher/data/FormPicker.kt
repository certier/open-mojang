package com.openmojang.launcher.data

object FormPicker {

    fun pickForm(forms: List<String>, abi: AbiPref, music: MusicPref): String {
        if (forms.isEmpty()) throw IllegalStateException("No download forms")
        val pool = filterByMusic(forms, music)
        return pool.maxByOrNull { score(it, abi, music) } ?: pool.first()
    }

    fun pickApkLink(links: List<String>, abi: AbiPref, music: MusicPref): String {
        if (links.isEmpty()) throw IllegalStateException("No APK links")
        if (links.size == 1) return links.first()
        val pool = filterByMusic(links, music)
        return pool.maxByOrNull { score(it, abi, music) } ?: pool.first()
    }

    /**
     * When the user picks With music, drop every "no music" candidate first.
     * When they pick No music, prefer only those. Label-free links stay if needed.
     */
    private fun filterByMusic(candidates: List<String>, music: MusicPref): List<String> {
        return when (music) {
            MusicPref.WITH_MUSIC -> {
                val withLabel = candidates.filter { isWithMusic(it) }
                if (withLabel.isNotEmpty()) return withLabel
                val notSlim = candidates.filter { !isNoMusic(it) }
                if (notSlim.isNotEmpty()) notSlim else candidates
            }
            MusicPref.NO_MUSIC -> {
                val slim = candidates.filter { isNoMusic(it) }
                if (slim.isNotEmpty()) slim else candidates
            }
            MusicPref.AUTO -> candidates
        }
    }

    private fun isNoMusic(text: String): Boolean {
        val l = text.lowercase()
        return l.contains("no music") ||
            l.contains("without music") ||
            l.contains("no-music") ||
            l.contains("nomusic") ||
            l.contains("no_music") ||
            l.contains("w/o music") ||
            l.contains("wo music") ||
            l.contains("without-music") ||
            l.contains("slim") && l.contains("music") ||
            // common mirror filenames
            l.contains("-nm-") ||
            l.contains("_nm_") ||
            l.contains("-nomusic") ||
            l.contains("_nomusic")
    }

    private fun isWithMusic(text: String): Boolean {
        if (isNoMusic(text)) return false
        val l = text.lowercase()
        return l.contains("with music") ||
            l.contains("with-music") ||
            l.contains("with_music") ||
            l.contains("music included") ||
            l.contains("+ music") ||
            l.contains("+music") ||
            l.contains("full music") ||
            l.contains("withmusic")
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
        when (music) {
            MusicPref.NO_MUSIC -> {
                if (isNoMusic(text)) s += 40
                if (isWithMusic(text)) s -= 50
            }
            MusicPref.WITH_MUSIC -> {
                if (isWithMusic(text)) s += 40
                if (isNoMusic(text)) s -= 50
            }
            MusicPref.AUTO -> {
                // Prefer smaller unless labeled with-music
                if (isNoMusic(text)) s += 10
            }
        }
        return s
    }
}
