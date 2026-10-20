package com.openmojang.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class McVersion(
    val id: String,
    val name: String,
    /** Full Android version string when known, e.g. 1.26.52.3 */
    val patch: String,
    val type: String,
    val date: String,
    val sizeMb: Int,
    val changelog: String,
    val downloadUrl: String
) {
    /** Short label for chips / lists */
    val displayName: String get() = if (patch.isNotBlank()) patch else name

    /** True if this build is a patch on a major drop (x.y.z.w with w > 0 or trailing hotfix). */
    val isPatch: Boolean
        get() {
            val parts = patch.removePrefix("1.").split('.')
            return parts.size >= 4 || (parts.size == 3 && parts.getOrNull(2)?.toIntOrNull()?.let { it in 1..49 } == true)
        }
}

class VersionRepository {

    private val catalog = listOf(
        // --- Beta / Preview line ---
        McVersion(
            id = "26.70.20",
            name = "1.26.70.20",
            patch = "1.26.70.20",
            type = "beta",
            date = "13 Oct 2026",
            sizeMb = 520,
            changelog = ChangelogProvider.forVersion("26.70.20"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-70-20-apk/"
        ),
        McVersion(
            id = "26.60.30",
            name = "1.26.60.30",
            patch = "1.26.60.30",
            type = "beta",
            date = "6 Oct 2026",
            sizeMb = 515,
            changelog = ChangelogProvider.forVersion("26.60.30"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-30-apk/"
        ),
        McVersion(
            id = "26.60.29",
            name = "1.26.60.29",
            patch = "1.26.60.29",
            type = "beta",
            date = "30 Sep 2026",
            sizeMb = 550,
            changelog = ChangelogProvider.forVersion("26.60.29"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-29-apk/"
        ),
        McVersion(
            id = "26.60.28",
            name = "1.26.60.28",
            patch = "1.26.60.28",
            type = "beta",
            date = "22 Sep 2026",
            sizeMb = 510,
            changelog = ChangelogProvider.forVersion("26.60.28"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-28-apk/"
        ),
        McVersion(
            id = "26.60.24",
            name = "1.26.60.24",
            patch = "1.26.60.24",
            type = "beta",
            date = "Sep 2026",
            sizeMb = 505,
            changelog = ChangelogProvider.forVersion("26.60.24"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-60-24-apk/"
        ),
        // --- Stable release line ---
        McVersion(
            id = "26.52",
            name = "1.26.52",
            patch = "1.26.52.3",
            type = "release",
            date = "28 Sep 2026",
            sizeMb = 370,
            changelog = ChangelogProvider.forVersion("26.52"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-52-apk/"
        ),
        McVersion(
            id = "26.51",
            name = "1.26.51",
            patch = "1.26.51.01",
            type = "release",
            date = "22 Sep 2026",
            sizeMb = 508,
            changelog = ChangelogProvider.forVersion("26.51"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-51-apk/"
        ),
        McVersion(
            id = "26.50",
            name = "1.26.50",
            patch = "1.26.50",
            type = "release",
            date = "15 Sep 2026",
            sizeMb = 505,
            changelog = ChangelogProvider.forVersion("26.50"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-50-apk/"
        ),
        McVersion(
            id = "26.45",
            name = "1.26.45",
            patch = "1.26.45",
            type = "release",
            date = "29 Aug 2026",
            sizeMb = 500,
            changelog = ChangelogProvider.forVersion("26.45"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-45-apk/"
        ),
        McVersion(
            id = "26.30",
            name = "1.26.30",
            patch = "1.26.30",
            type = "release",
            date = "2026",
            sizeMb = 490,
            changelog = ChangelogProvider.forVersion("26.30"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-30-apk/"
        ),
        McVersion(
            id = "26.10",
            name = "1.26.10",
            patch = "1.26.10",
            type = "release",
            date = "Mar 2026",
            sizeMb = 480,
            changelog = ChangelogProvider.forVersion("26.10"),
            downloadUrl = "https://mcpedl.org/minecraft-pe-26-10-apk/"
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

    fun latestStable(): McVersion? = catalog.firstOrNull { it.type == "release" }
    fun latestBeta(): McVersion? = catalog.firstOrNull { it.type == "beta" }
}
