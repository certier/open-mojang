package com.openmojang.launcher.data

/**
 * Rich patch-note blocks shown inside the launcher UI.
 * Keyed by version id (e.g. "26.52").
 */
object ChangelogProvider {

    fun forVersion(id: String): String {
        return notes[id] ?: notes[id.substringBeforeLast('.')] ?: "No detailed notes for this build yet."
    }

    private val notes = mapOf(
        "26.70.20" to """
            |Ice Caves Drop — Beta
            |
            |Gameplay
            |• New ice cave generation passes
            |• Realms permission edge cases fixed
            |• Faster Realms world uploads
            |
            |Controls
            |• Touch control stutter reduced on mid-range devices
            |
            |Progress
            |• Achievements that were incorrectly disabled are restored
            """.trimMargin(),

        "26.52" to """
            |What Minecraft 26.52 fixes
            |
            |Stability
            |• Crash rate down after Wilderness Bound (26.50)
            |• Realms session reconnect hardened
            |
            |UI / Accessibility
            |• Realms interface polish
            |• Screen reader labels corrected on several menus
            |• Text scale no longer clips on small phones
            |
            |Performance
            |• Chunk mesh build cost reduced on 4–6 GB devices
            |• Lower jank when opening inventory during combat
            """.trimMargin(),

        "26.50" to """
            |Wilderness Bound
            |
            |World
            |• Dappled Forest biome
            |• Abandoned camps with loot tables
            |• Sit pads (player can sit)
            |
            |Mobs
            |• Baby mob visual + AI overhaul
            |• Golden dandelion interactions expanded
            |
            |Other
            |• Craftable name tags recipe path adjusted
            """.trimMargin(),

        "26.10" to """
            |Tiny Takeover
            |
            |• Baby mob redesign pass
            |• Golden dandelions
            |• Craftable name tags
            |• Broad quality-of-life and performance notes
            """.trimMargin()
    )
}
