package com.openmojang.launcher.data

/**
 * Rich patch-note blocks shown inside the launcher UI.
 * Keyed by version id (e.g. "26.52").
 */
object ChangelogProvider {

    fun forVersion(id: String): String {
        return notes[id]
            ?: notes[id.substringBeforeLast('.')]
            ?: "No detailed notes for this build yet."
    }

    private val notes = mapOf(
        "26.70.20" to """
            |Minecraft 1.26.70.20 (Beta) — Ice Caves
            |
            |World
            |• Brittle Ice over ice-cave crevasses — cracks underfoot then breaks
            |• Ice Cave generation refinements
            |
            |Gameplay
            |• Chest above a Jukebox can feed Music Discs as a playlist
            |• Frostbites can pick up Spears
            |
            |Experimental
            |• Nether spawn density / fortress placement closer to Java
            |
            |Fixes
            |• Less lag opening Abandoned Camp chests
            |• Crash and missing-sound fixes
            """.trimMargin(),

        "26.60.30" to """
            |Minecraft 1.26.60.30 (Beta)
            |
            |• Touch control stutter reduced
            |• Achievements restored where they were disabled
            |• Faster Realms world uploads
            """.trimMargin(),

        "26.60.29" to """
            |Minecraft 1.26.60.29 (Beta) — Drop 4 preview
            |
            |Experimental
            |• Ice Cave biome with icicles and ice crystals
            |• Frostbite zombie (Freezing + Ice Balls)
            |
            |Fixes
            |• Walking into dropped stacks no longer duplicates items
            |• Generating World screen no longer hangs between dimensions
            |• Realms world downloads much faster
            |• Main menu layout updates (gradual UI rebuild)
            """.trimMargin(),

        "26.60.28" to """
            |Minecraft 1.26.60.28 (Preview / Beta)
            |
            |• Blocks now require central block support (stability)
            |• Improved cursor movement while scrolling
            |• Cubemap array texture detection fixed on older Windows GPUs
            """.trimMargin(),

        "26.60.24" to """
            |Minecraft 1.26.60.24 (Beta)
            |
            |• Early Drop 4 / Ice Caves pipeline
            |• Stability and networking fixes ahead of 26.60.28+
            """.trimMargin(),

        "26.52" to """
            |What Minecraft 1.26.52.3 fixes
            |
            |Hotfix on Chaos Cubed / Wilderness Bound line
            |
            |Combat / Spectator
            |• Spectator mode projectile issues
            |
            |Items
            |• Cushion reload behavior
            |• Invisible items edge cases
            |
            |Marketplace
            |• Achievement tracking for Marketplace content
            |
            |Stability
            |• Crash rate down after 26.50
            |• Realms session reconnect hardened
            |
            |UI
            |• Realms interface polish
            |• Screen reader labels
            |• Text scale clipping on small phones
            |
            |Performance
            |• Chunk mesh cost reduced on 4–6 GB devices
            |• Less jank opening inventory in combat
            """.trimMargin(),

        "26.51" to """
            |Minecraft 1.26.51.01
            |
            |Hotfix on Wilderness Bound (26.50).
            |Stability and multiplayer reconnect fixes.
            """.trimMargin(),

        "26.50" to """
            |Minecraft 1.26.50 — Wilderness Bound
            |
            |World
            |• Dappled Forest biome
            |• Abandoned camps with loot
            |• Sit pads
            |
            |Mobs
            |• Baby mob visual + AI pass
            |
            |Other
            |• Golden dandelion interactions
            |• Craftable name tags path adjusted
            """.trimMargin(),

        "26.45" to """
            |Minecraft 1.26.45
            |
            |Maintenance hotfix. Stability only.
            """.trimMargin(),

        "26.30" to """
            |Minecraft 1.26.30 — Chaos Cubed
            |
            |• Sulfur caves
            |• Cinnabar blocks
            |• Sulfur cube mob
            """.trimMargin(),

        "26.10" to """
            |Minecraft 1.26.10 — Tiny Takeover
            |
            |• Baby mob redesign
            |• Golden dandelions
            |• Craftable name tags
            |• QoL and performance
            """.trimMargin()
    )
}
