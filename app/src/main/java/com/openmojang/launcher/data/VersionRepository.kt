package com.openmojang.launcher.data

data class McVersion(
    val id: String,
    val name: String,
    val patch: String = "",
    val type: String,
    val date: String,
    val sizeMbSlim: Int,
    val sizeMbFull: Int,
    val downloadUrl: String,
    val changelog: String = "",
    val cover: String = "grass",
    val tags: List<String> = emptyList()
) {
    val sizeMb: Int get() = sizeMbFull
    val displayName: String get() = name.ifBlank { id }
    val typeLabel: String
        get() = when (type) {
            "release" -> "Release"
            "beta" -> "Beta"
            "patch" -> "Patch"
            "hotfix" -> "Hotfix"
            "community" -> "Community"
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
        MusicPref.AUTO -> "~${sizeMbFull} MB full / ~${sizeMbSlim} MB slim"
    }
}

class VersionRepository {

    private val directApk = mapOf(
        "26.52" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk",
        "26.52.02" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk"
    )

    private fun v(
        id: String,
        name: String,
        type: String,
        date: String,
        slim: Int,
        full: Int,
        slug: String,
        cover: String = "grass",
        tags: List<String> = emptyList()
    ) = McVersion(
        id = id,
        name = name,
        patch = name.removePrefix("1."),
        type = type,
        date = date,
        sizeMbSlim = slim,
        sizeMbFull = full,
        downloadUrl = "https://mcpedl.org/$slug/",
        changelog = ChangelogProvider.forVersion(id),
        cover = cover,
        tags = tags
    )

    private val catalog: List<McVersion> = listOf(
        v("26.70.20", "1.26.70.20", "beta", "2026-10", 210, 560, "minecraft-pe-26-70-20-apk", "ice", listOf("preview", "latest")),
        v("26.70.10", "1.26.70.10", "beta", "2026-10", 208, 558, "minecraft-pe-26-70-10-apk", "ice", listOf("preview")),
        v("26.60.30", "1.26.60.30", "beta", "2026-09", 205, 555, "minecraft-pe-26-60-30-apk", "ice"),
        v("26.60.29", "1.26.60.29", "beta", "2026-09", 204, 554, "minecraft-pe-26-60-29-apk", "ice"),
        v("26.60.28", "1.26.60.28", "beta", "2026-09", 203, 553, "minecraft-pe-26-60-28-apk", "ice"),
        v("26.60.02", "1.26.60.02", "hotfix", "2026-09", 202, 552, "minecraft-pe-26-60-02-apk", "ice", listOf("hotfix")),
        v("26.52", "1.26.52", "release", "2026-09", 198, 548, "minecraft-pe-26-52-apk", "grass", listOf("stable")),
        v("26.52.02", "1.26.52.02", "patch", "2026-09", 198, 548, "minecraft-pe-26-52-apk", "grass", listOf("patch")),
        v("26.50", "1.26.50", "release", "2026-08", 196, 545, "minecraft-pe-26-50-apk", "grass"),
        v("26.40", "1.26.40", "release", "2026-08", 194, 540, "minecraft-pe-26-40-apk", "grass"),
        v("26.30", "1.26.30", "release", "2026-07", 190, 530, "minecraft-pe-26-30-apk", "grass"),
        v("26.20", "1.26.20", "release", "2026-07", 188, 528, "minecraft-pe-26-20-apk", "grass"),
        v("26.10", "1.26.10", "release", "2026-06", 185, 525, "minecraft-pe-26-10-apk", "grass"),
        v("26.0", "1.26.0", "release", "2026-06", 180, 520, "minecraft-pe-26-0-apk", "grass"),
        v("21.100", "1.21.100", "release", "2026-05", 175, 510, "minecraft-pe-21-100-apk", "nether"),
        v("21.90", "1.21.90", "release", "2026-04", 172, 505, "minecraft-pe-21-90-apk", "nether"),
        v("21.80", "1.21.80", "release", "2026-03", 170, 500, "minecraft-pe-21-80-apk", "nether"),
        v("21.70", "1.21.70", "release", "2026-02", 168, 495, "minecraft-pe-21-70-apk", "nether"),
        v("21.60", "1.21.60", "release", "2026-01", 165, 490, "minecraft-pe-21-60-apk", "nether"),
        v("21.50", "1.21.50", "release", "2025-12", 160, 480, "minecraft-pe-21-50-apk", "nether"),
        v("21.44", "1.21.44", "hotfix", "2025-11", 158, 475, "minecraft-pe-21-44-apk", "nether", listOf("hotfix")),
        v("21.31", "1.21.31", "release", "2025-10", 155, 470, "minecraft-pe-21-31-apk", "nether"),
        v("21.23", "1.21.23", "patch", "2025-09", 152, 465, "minecraft-pe-21-23-apk", "nether", listOf("patch")),
        v("21.20", "1.21.20", "release", "2025-08", 150, 460, "minecraft-pe-21-20-apk", "nether"),
        v("21.0", "1.21.0", "release", "2025-06", 145, 450, "minecraft-pe-21-0-apk", "nether"),
        v("26.52-community", "1.26.52 Community", "community", "2026-09", 200, 550, "minecraft-pe-26-52-apk", "end", listOf("community", "mirror")),
        v("26.50-slim-focus", "1.26.50 Slim Focus", "community", "2026-08", 190, 540, "minecraft-pe-26-50-apk", "end", listOf("community")),
        v("21.100-community", "1.21.100 Community", "community", "2026-05", 170, 500, "minecraft-pe-21-100-apk", "end", listOf("community"))
    )

    fun getVersions(): List<McVersion> = catalog

    fun byType(type: String): List<McVersion> =
        if (type == "all") catalog else catalog.filter { it.type == type }

    fun search(q: String): List<McVersion> {
        if (q.isBlank()) return catalog
        val s = q.trim().lowercase()
        return catalog.filter {
            it.displayName.lowercase().contains(s) ||
                it.id.lowercase().contains(s) ||
                it.type.lowercase().contains(s) ||
                it.tags.any { t -> t.contains(s) }
        }
    }

    fun findByInstalled(systemVer: String?): McVersion? {
        if (systemVer.isNullOrBlank()) return null
        val key = systemVer.trim()
        return catalog.firstOrNull {
            it.patch == key || it.name == key || it.id == key ||
                key.startsWith(it.patch) || it.patch.startsWith(key) ||
                key.contains(it.patch)
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
        catalog.firstOrNull { it.type == "patch" || it.type == "hotfix" }

    fun typesPresent(): List<String> =
        catalog.map { it.type }.distinct()
}
