package com.openmojang.launcher.data

data class UpdateStatus(
    val installed: String?,
    val installedMatch: McVersion?,
    val latestStable: McVersion?,
    val latestBeta: McVersion?,
    val updateAvailable: Boolean
)

class UpdateChecker(
    private val context: android.content.Context,
    private val repo: VersionRepository = VersionRepository()
) {
    private val installed = InstalledVersions(context)

    suspend fun check(): UpdateStatus {
        val versions = repo.getVersions()
        val stable = versions.firstOrNull { it.type == "release" }
        val beta = versions.firstOrNull { it.type == "beta" }
        val system = installed.systemMinecraftVersion()
        val match = repo.findByInstalled(system)

        val updateAvailable = when {
            system == null && stable != null -> true
            stable == null -> false
            match == null -> true
            match.id != stable.id -> true
            // same drop but older patch (e.g. 1.26.52 vs 1.26.52.3)
            match.patch != stable.patch &&
                system != null &&
                !system.contains(stable.patch.removePrefix("1.")) &&
                system != stable.patch -> true
            else -> false
        }

        return UpdateStatus(
            installed = system,
            installedMatch = match,
            latestStable = stable,
            latestBeta = beta,
            updateAvailable = updateAvailable
        )
    }
}
