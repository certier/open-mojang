package com.openmojang.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class McVersion(
    val id: String,
    val name: String,
    val type: String,
    val date: String,
    val sizeMb: Int,
    val changelog: String,
    val downloadUrl: String
)

class VersionRepository {

    private val catalog = listOf(
        McVersion("26.70.20", "1.26.70.20", "beta", "13 Oct 2026", 520,
            "Ice Caves Drop beta. Realms permissions, faster uploads, touch stutter, achievements.",
            "https://mcpedl.org/minecraft-pe-26-70-20-apk/"),
        McVersion("26.60.30", "1.26.60.30", "beta", "6 Oct 2026", 515,
            "Touch stutter fixed, achievements restored, Realms upload speed.",
            "https://mcpedl.org/minecraft-pe-26-60-30-apk/"),
        McVersion("26.52", "1.26.52", "release", "28 Sep 2026", 510,
            "Stability after Wilderness Bound, Realms UI, accessibility, mid-range performance.",
            "https://mcpedl.org/minecraft-pe-26-52-apk/"),
        McVersion("26.51", "1.26.51", "release", "22 Sep 2026", 508,
            "Hotfix on Wilderness Bound.",
            "https://mcpedl.org/minecraft-pe-26-51-apk/"),
        McVersion("26.50", "1.26.50", "release", "15 Sep 2026", 505,
            "Wilderness Bound. Dappled Forest, camps, sit pads, baby mobs.",
            "https://mcpedl.org/minecraft-pe-26-50-apk/"),
        McVersion("26.45", "1.26.45", "release", "29 Aug 2026", 500,
            "Maintenance hotfix.",
            "https://mcpedl.org/minecraft-pe-26-45-apk/"),
        McVersion("26.10", "1.26.10", "release", "Mar 2026", 480,
            "Tiny Takeover. Baby mobs, golden dandelions, craftable name tags.",
            "https://mcpedl.org/minecraft-pe-26-10-apk/")
    )

    suspend fun getVersions(): List<McVersion> = withContext(Dispatchers.IO) { catalog }

    fun urlsFor(version: McVersion): List<String> {
        val slug = version.id.replace('.', '-')
        return listOf(
            version.downloadUrl,
            "https://mcpedl.org/minecraft-pe-$slug-apk/",
            "https://mcpe-planet.com/minecraft-pe-$slug-apk/",
            "https://mcpe-planet.com/downloads/"
        ).distinct()
    }

    fun latestStable(): McVersion? = catalog.firstOrNull { it.type == "release" }
    fun latestBeta(): McVersion? = catalog.firstOrNull { it.type == "beta" }
}
