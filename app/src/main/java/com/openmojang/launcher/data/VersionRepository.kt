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

/**
 * Curated Bedrock catalog. Tracks the same mirrors that push same-day Mojang builds.
 * Direct APK URLs rotate; downloadUrl points at the stable version page.
 */
class VersionRepository {

    private val catalog = listOf(
        McVersion(
            "26.70.20", "1.26.70.20", "beta", "13 Oct 2026", 520,
            "Ice Caves Drop beta.\n• Realms permission fixes\n• Faster Realms uploads\n• Touch control stutter fixed\n• Achievement restoration",
            "https://mcpe-planet.com/downloads/"
        ),
        McVersion(
            "26.60.30", "1.26.60.30", "beta", "6 Oct 2026", 515,
            "Touch control stutter fixed, achievements restored, Realms upload speed improved.",
            "https://mcpedl.org/minecraft-pe-26-60-30-apk/"
        ),
        McVersion(
            "26.60.29", "1.26.60.29", "beta", "30 Sep 2026", 514,
            "Faster Realms world operations and minor beta stability.",
            "https://mcpe-planet.com/downloads/"
        ),
        McVersion(
            "26.52", "1.26.52", "release", "28 Sep 2026", 510,
            "What Minecraft 26.52 fixes:\n• Stability after Wilderness Bound\n• Realms interface polish\n• Accessibility + UI fixes\n• Better performance on mid-range Android",
            "https://mcpedl.org/downloading/"
        ),
        McVersion(
            "26.51", "1.26.51", "release", "22 Sep 2026", 508,
            "Hotfix on Wilderness Bound — crash fixes and Realms edge cases.",
            "https://mcpe-planet.com/downloads/"
        ),
        McVersion(
            "26.50", "1.26.50", "release", "15 Sep 2026", 505,
            "Wilderness Bound drop – Dappled Forest, abandoned camps, sit pads, baby mob overhaul.",
            "https://mcpe-planet.com/downloads/"
        ),
        McVersion(
            "26.45", "1.26.45", "release", "29 Aug 2026", 500,
            "Maintenance hotfix. Platform rollout staggered; APK bypasses store wait.",
            "https://mcpedl.org/minecraft-pe-26-45-apk/"
        ),
        McVersion(
            "26.40.22", "1.26.40.22", "beta", "Jun 2026", 495,
            "Sulfur Cube behavior, Vibrant Visuals underwater, PBR pack fixes, multiplayer pack management.",
            "https://mcpedl.org/downloading/"
        ),
        McVersion(
            "26.10", "1.26.10", "release", "Mar 2026", 480,
            "Tiny Takeover — baby mob overhaul, golden dandelions, craftable name tags, QoL.",
            "https://mcpe-planet.com/downloads/"
        )
    )

    suspend fun getVersions(): List<McVersion> = withContext(Dispatchers.IO) { catalog }

    fun latestStable(): McVersion? = catalog.firstOrNull { it.type == "release" }
    fun latestBeta(): McVersion? = catalog.firstOrNull { it.type == "beta" }
    fun byId(id: String): McVersion? = catalog.find { it.id == id }
}
