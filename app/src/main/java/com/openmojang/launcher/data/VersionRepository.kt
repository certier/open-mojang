package com.openmojang.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class McVersion(
    val id: String,
    val name: String,
    val patch: String = "",
    val type: String,
    val date: String,
    val sizeMb: Int,
    val downloadUrl: String,
    val changelog: String = "",
    val cover: String = "grass"
) {
    val displayName: String
        get() = name.ifBlank { id }

    val typeLabel: String
        get() = when (type) {
            "release" -> "Release"
            "beta" -> "Beta"
            "patch" -> "Patch"
            else -> type.replaceFirstChar { it.uppercase() }
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
            sizeMb = 210,
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
            sizeMb = 205,
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
            sizeMb = 204,
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
            sizeMb = 203,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-28-apk/",
            changelog = ChangelogProvider.forVersion("26.60.28"),
            cover = "ice"
        ),
        McVersion(
            id = "26.52",
            name = "1.26.52",
            patch = "1.26.52.3",
            type = "patch",
            date = "2026-09",
            sizeMb = 198,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-52-apk/",
            changelog = ChangelogProvider.forVersion("26.52"),
            cover = "patch"
        ),
        McVersion(
            id = "26.50",
            name = "1.26.50",
            patch = "1.26.50",
            type = "release",
            date = "2026-08",
            sizeMb = 196,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-50-apk/",
            changelog = ChangelogProvider.forVersion("26.50"),
            cover = "sulfur"
        ),
        McVersion(
            id = "26.30",
            name = "1.26.30",
            patch = "1.26.30",
            type = "release",
            date = "2026-07",
            sizeMb = 190,
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-30-apk/",
            changelog = ChangelogProvider.forVersion("26.30"),
            cover = "forest"
        ),
        McVersion(
            id = "21.0",
            name = "1.21",
            patch = "1.21.0",
            type = "release",
            date = "2024",
            sizeMb = 180,
            downloadUrl = "https://mcpedl.org/minecraft-pe-1-21-apk/",
            changelog = ChangelogProvider.forVersion("21.0"),
            cover = "grass"
        )
    )

    suspend fun getVersions(): List<McVersion> = withContext(Dispatchers.IO) {
        catalog.sortedByDescending {
            UpdateChecker.versionKey(it.patch.ifBlank { it.name })
        }
    }

    fun findByInstalled(systemVer: String?): McVersion? {
        if (systemVer.isNullOrBlank()) return null
        val cleaned = systemVer.trim()
        catalog.firstOrNull { it.patch.equals(cleaned, true) || it.name.equals(cleaned, true) }
            ?.let { return it }
        catalog.firstOrNull { v ->
            cleaned.contains(v.patch) || v.patch.contains(cleaned) ||
                cleaned.contains(v.name) || cleaned.contains(v.id) ||
                cleaned.replace("1.", "").contains(v.id)
        }?.let { return it }
        return catalog.firstOrNull {
            UpdateChecker.compareVersionStrings(cleaned, it.patch.ifBlank { it.name }) == 0
        }
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
        catalog.filter { it.type == "release" || it.type == "patch" }
            .maxWithOrNull(compareBy { UpdateChecker.versionKey(it.patch.ifBlank { it.name }) })

    fun latestBeta(): McVersion? =
        catalog.filter { it.type == "beta" }
            .maxWithOrNull(compareBy { UpdateChecker.versionKey(it.patch.ifBlank { it.name }) })

    fun latestPatch(): McVersion? =
        catalog.filter { it.type == "patch" }
            .maxWithOrNull(compareBy { UpdateChecker.versionKey(it.patch.ifBlank { it.name }) })
}
