package com.openmojang.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class McVersion(
    val id: String,
    val name: String,
    val patch: String = "",
    val type: String,
    val date: String,
    /** Approximate APK size without music pack (slim). */
    val sizeMbSlim: Int,
    /** Approximate APK size with music (full). Real downloads often land near this. */
    val sizeMbFull: Int,
    val downloadUrl: String,
    val changelog: String = "",
    val cover: String = "grass"
) {
    /** Legacy single field — prefer [estimatedMb]. */
    val sizeMb: Int get() = sizeMbFull

    val displayName: String
        get() = name.ifBlank { id }

    val typeLabel: String
        get() = when (type) {
            "release" -> "Release"
            "beta" -> "Beta"
            "patch" -> "Patch"
            else -> type.replaceFirstChar { it.uppercase() }
        }

    fun estimatedMb(music: MusicPref): Int = when (music) {
        MusicPref.WITH_MUSIC -> sizeMbFull
        MusicPref.NO_MUSIC -> sizeMbSlim
        MusicPref.AUTO -> sizeMbFull
    }

    fun sizeLabel(music: MusicPref): String = when (music) {
        MusicPref.WITH_MUSIC -> "~${sizeMbFull} MB with music"
        MusicPref.NO_MUSIC -> "~${sizeMbSlim} MB no music"
        MusicPref.AUTO -> "~${sizeMbFull} MB (full) / ~${sizeMbSlim} MB slim"
    }
}

class VersionRepository {

    private val directApk = mapOf(
        "26.52" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk",
        "26.52.02" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk"
    )

    private val catalog: List<McVersion> = listOf(
        McVersion(
            id = "26.70.20",
            name = "1.26.70.20",
            patch = "1.26.70.20",
            type = "beta",
            date = "2026-10",
            sizeMbSlim = 210,
            sizeMbFull = 560,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-70-20-apk/",
            changelog = ChangelogProvider.forVersion("26.70.20"),
            cover = "ice"
        ),
        McVersion(
            id = "26.60.30",
            name = "1.26.60.30",
            patch = "1.26.60.30",
            type = "beta",
            date = "2026-09",
            sizeMbSlim = 205,
            sizeMbFull = 555,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-30-apk/",
            changelog = ChangelogProvider.forVersion("26.60.30"),
            cover = "ice"
        ),
        McVersion(
            id = "26.60.29",
            name = "1.26.60.29",
            patch = "1.26.60.29",
            type = "beta",
            date = "2026-09",
            sizeMbSlim = 204,
            sizeMbFull = 554,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-29-apk/",
            changelog = ChangelogProvider.forVersion("26.60.29"),
            cover = "ice"
        ),
        McVersion(
            id = "26.60.28",
            name = "1.26.60.28",
            patch = "1.26.60.28",
            type = "beta",
            date = "2026-09",
            sizeMbSlim = 203,
            sizeMbFull = 553,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-28-apk/",
            changelog = ChangelogProvider.forVersion("26.60.28"),
            cover = "ice"
        ),
        McVersion(
            id = "26.52",
            name = "1.26.52",
            patch = "1.26.52",
            type = "release",
            date = "2026-09",
            sizeMbSlim = 198,
            sizeMbFull = 548,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-52-apk/",
            changelog = ChangelogProvider.forVersion("26.52"),
            cover = "grass"
        ),
        McVersion(
            id = "26.50",
            name = "1.26.50",
            patch = "1.26.50",
            type = "release",
            date = "2026-08",
            sizeMbSlim = 196,
            sizeMbFull = 545,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-50-apk/",
            changelog = ChangelogProvider.forVersion("26.50"),
            cover = "grass"
        ),
        McVersion(
            id = "26.30",
            name = "1.26.30",
            patch = "1.26.30",
            type = "release",
            date = "2026-07",
            sizeMbSlim = 190,
            sizeMbFull = 530,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-30-apk/",
            changelog = ChangelogProvider.forVersion("26.30"),
            cover = "grass"
        ),
        McVersion(
            id = "26.0",
            name = "1.26.0",
            patch = "1.26.0",
            type = "release",
            date = "2026-06",
            sizeMbSlim = 180,
            sizeMbFull = 520,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-0-apk/",
            changelog = ChangelogProvider.forVersion("26.0"),
            cover = "grass"
        )
    )

    suspend fun getVersions(): List<McVersion> = withContext(Dispatchers.IO) {
        catalog
    }

    fun findByInstalled(systemVer: String?): McVersion? {
        if (systemVer.isNullOrBlank()) return null
        val key = systemVer.trim()
        return catalog.firstOrNull {
            it.patch == key || it.name == key || it.id == key ||
                key.startsWith(it.patch) || it.patch.startsWith(key)
        }
    }

    fun urlsFor(version: McVersion): List<String> {
        val out = mutableListOf<String>()
        directApk[version.id]?.let { out += it }
        directApk[version.patch]?.let { out += it }
        if (version.downloadUrl.isNotBlank()) out += version.downloadUrl
        return out.distinct()
    }

    fun latestStable(): McVersion? =
        catalog.firstOrNull { it.type == "release" }

    fun latestBeta(): McVersion? =
        catalog.firstOrNull { it.type == "beta" }

    fun latestPatch(): McVersion? =
        catalog.firstOrNull { it.type == "patch" }
}
