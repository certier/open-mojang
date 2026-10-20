package com.openmojang.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * type: "release" | "beta" | "patch"
 * cover: art key used for the colored banner on version cards
 */
data class McVersion(
    val id: String,
    val name: String,
    val patch: String,
    val type: String,
    val date: String,
    val sizeMb: Int,
    val changelog: String,
    val downloadUrl: String,
    val cover: String = "default"
) {
    val displayName: String get() = if (patch.isNotBlank()) patch else name

    val isPatch: Boolean get() = type == "patch"

    val typeLabel: String
        get() = when (type) {
            "beta" -> "Beta"
            "patch" -> "Minecraft Patch"
            else -> "Release"
        }
}

class VersionRepository {

    private val catalog = listOf(
        McVersion(
            id = "26.70.20",
            name = "1.26.70.20",
            patch = "1.26.70.20",
            type = "beta",
            date = "13 Oct 2026",
            sizeMb = 520,
            changelog = ChangelogProvider.forVersion("26.70.20"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-70-20-apk/",
            cover = "ice"
        ),
        McVersion(
            id = "26.60.30",
            name = "1.26.60.30",
            patch = "1.26.60.30",
            type = "beta",
            date = "6 Oct 2026",
            sizeMb = 515,
            changelog = ChangelogProvider.forVersion("26.60.30"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-30-apk/",
            cover = "ice"
        ),
        McVersion(
            id = "26.60.29",
            name = "1.26.60.29",
            patch = "1.26.60.29",
            type = "beta",
            date = "30 Sep 2026",
            sizeMb = 550,
            changelog = ChangelogProvider.forVersion("26.60.29"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-29-apk/",
            cover = "ice"
        ),
        McVersion(
            id = "26.60.28",
            name = "1.26.60.28",
            patch = "1.26.60.28",
            type = "beta",
            date = "22 Sep 2026",
            sizeMb = 510,
            changelog = ChangelogProvider.forVersion("26.60.28"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-28-apk/",
            cover = "ice"
        ),
        McVersion(
            id = "26.60.24",
            name = "1.26.60.24",
            patch = "1.26.60.24",
            type = "beta",
            date = "Sep 2026",
            sizeMb = 505,
            changelog = ChangelogProvider.forVersion("26.60.24"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-24-apk/",
            cover = "ice"
        ),
        McVersion(
            id = "26.60.23",
            name = "1.26.60.23",
            patch = "1.26.60.23",
            type = "beta",
            date = "Sep 2026",
            sizeMb = 500,
            changelog = ChangelogProvider.forVersion("26.60.24"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-23-apk/",
            cover = "ice"
        ),
        McVersion(
            id = "26.60.21",
            name = "1.26.60.21",
            patch = "1.26.60.21",
            type = "beta",
            date = "Sep 2026",
            sizeMb = 498,
            changelog = ChangelogProvider.forVersion("26.60.24"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-21-apk/",
            cover = "ice"
        ),
        McVersion(
            id = "26.52",
            name = "1.26.52",
            patch = "1.26.52.3",
            type = "patch",
            date = "28 Sep 2026",
            sizeMb = 370,
            changelog = ChangelogProvider.forVersion("26.52"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-52-apk/",
            cover = "patch"
        ),
        McVersion(
            id = "26.51",
            name = "1.26.51",
            patch = "1.26.51.01",
            type = "patch",
            date = "22 Sep 2026",
            sizeMb = 508,
            changelog = ChangelogProvider.forVersion("26.51"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-51-apk/",
            cover = "patch"
        ),
        McVersion(
            id = "26.45",
            name = "1.26.45",
            patch = "1.26.45",
            type = "patch",
            date = "29 Aug 2026",
            sizeMb = 500,
            changelog = ChangelogProvider.forVersion("26.45"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-45-apk/",
            cover = "patch"
        ),
        McVersion(
            id = "26.50",
            name = "1.26.50",
            patch = "1.26.50",
            type = "release",
            date = "15 Sep 2026",
            sizeMb = 505,
            changelog = ChangelogProvider.forVersion("26.50"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-50-apk/",
            cover = "forest"
        ),
        McVersion(
            id = "26.30",
            name = "1.26.30",
            patch = "1.26.30",
            type = "release",
            date = "2026",
            sizeMb = 490,
            changelog = ChangelogProvider.forVersion("26.30"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-30-apk/",
            cover = "sulfur"
        ),
        McVersion(
            id = "26.10",
            name = "1.26.10",
            patch = "1.26.10",
            type = "release",
            date = "Mar 2026",
            sizeMb = 480,
            changelog = ChangelogProvider.forVersion("26.10"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-10-apk/",
            cover = "grass"
        )
    )

    private val directApk = mapOf(
        "26.52" to "https://file.mcpedl.org/uploads_files/28-09-2026/minecraft-26-52-arm64-v8a.apk"
    )

    suspend fun getVersions(): List<McVersion> = withContext(Dispatchers.IO) { catalog }

    fun findByInstalled(versionName: String?): McVersion? {
        if (versionName.isNullOrBlank()) return null
        val cleaned = versionName.trim()
        return catalog.firstOrNull { it.patch == cleaned }
            ?: catalog.firstOrNull { cleaned.startsWith(it.patch) || it.patch.startsWith(cleaned) }
            ?: catalog.firstOrNull { cleaned.contains(it.id) || it.name in cleaned }
    }

    fun urlsFor(version: McVersion): List<String> {
        val slug = version.id.replace('.', '-')
        val list = mutableListOf<String>()
        directApk[version.id]?.let { list += it }
        list += version.downloadUrl
        list += "https://mcpedl.org/minecraft-pe-$slug-apk/"
        list += "https://mcpe-planet.com/minecraft-pe-$slug-apk/"
        return list.distinct()
    }

    fun latestStable(): McVersion? =
        catalog.firstOrNull { it.type == "patch" }
            ?: catalog.firstOrNull { it.type == "release" }

    fun latestBeta(): McVersion? = catalog.firstOrNull { it.type == "beta" }

    fun latestPatch(): McVersion? = catalog.firstOrNull { it.type == "patch" }
}
