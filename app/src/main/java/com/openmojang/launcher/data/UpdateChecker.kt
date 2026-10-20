package com.openmojang.launcher.data

data class UpdateStatus(
    val installed: String?,
    val installedMatch: McVersion?,
    val latestStable: McVersion?,
    val latestBeta: McVersion?,
    val newestInCatalog: McVersion?,
    val updateAvailable: Boolean,
    val updateTargetName: String?
)

class UpdateChecker(
    private val context: android.content.Context,
    private val repo: VersionRepository = VersionRepository()
) {
    private val installed = InstalledVersions(context)

    suspend fun check(): UpdateStatus {
        val versions = repo.getVersions()
        val stable = versions
            .filter { it.type == "release" || it.type == "patch" }
            .maxWithOrNull(compareBy { versionKey(it.patch.ifBlank { it.name }) })
        val beta = versions
            .filter { it.type == "beta" }
            .maxWithOrNull(compareBy { versionKey(it.patch.ifBlank { it.name }) })
        val newest = versions.maxWithOrNull(
            compareBy { versionKey(it.patch.ifBlank { it.name }) }
        )
        val system = installed.systemMinecraftVersion()
        val match = repo.findByInstalled(system)

        val candidate = newest
        val updateAvailable = when {
            system == null && candidate != null -> true
            system != null && candidate != null ->
                compareVersionStrings(system, candidate.patch.ifBlank { candidate.name }) < 0
            else -> false
        }

        return UpdateStatus(
            installed = system,
            installedMatch = match,
            latestStable = stable,
            latestBeta = beta,
            newestInCatalog = newest,
            updateAvailable = updateAvailable,
            updateTargetName = if (updateAvailable) candidate?.displayName else null
        )
    }

    companion object {
        /**
         * Pack version digits into a Long so compareBy works.
         * 1.26.70.20 -> higher than 1.26.50
         */
        fun versionKey(raw: String?): Long {
            if (raw.isNullOrBlank()) return 0L
            val parts = Regex("\\d+").findAll(raw).map { it.value.toLongOrNull() ?: 0L }.toList()
            var key = 0L
            // major.minor.patch.revision — 4 slots of 1000
            for (i in 0 until 4) {
                val n = parts.getOrElse(i) { 0L }.coerceIn(0L, 999L)
                key = key * 1000L + n
            }
            return key
        }

        fun compareVersionStrings(a: String?, b: String?): Int =
            versionKey(a).compareTo(versionKey(b))
    }
}
