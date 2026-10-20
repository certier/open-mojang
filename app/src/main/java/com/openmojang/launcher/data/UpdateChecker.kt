package com.openmojang.launcher.data

import android.content.Context

data class UpdateStatus(
    val installed: String?,
    val latestStable: McVersion?,
    val latestBeta: McVersion?,
    val updateAvailable: Boolean
)

class UpdateChecker(
    private val context: Context,
    private val repo: VersionRepository = VersionRepository()
) {
    private val installed = InstalledVersions(context)

    suspend fun check(): UpdateStatus {
        val versions = repo.getVersions()
        val stable = versions.firstOrNull { it.type == "release" }
        val beta = versions.firstOrNull { it.type == "beta" }
        val system = installed.systemMinecraftVersion()

        val updateAvailable = stable != null && system != null &&
            !system.contains(stable.name.removePrefix("1.").take(5)) &&
            system != stable.name

        return UpdateStatus(
            installed = system,
            latestStable = stable,
            latestBeta = beta,
            updateAvailable = updateAvailable || (system == null && stable != null)
        )
    }
}
