package com.openmojang.launcher.data

data class UpdateStatus(
    val installed: String?,
    val installedMatch: McVersion?,
    val latestStable: McVersion?,
    val latestBeta: McVersion?,
    val newestInCatalog: McVersion?,
    val updateAvailable: Boolean,
    /** Human line when an update exists, else null. */
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

        // Only "update" if catalog has something strictly newer than installed
        val candidate = newest
        val updateAvailable = when {
            system == null && candidate != null -> true
            system != null && candidate != null ->
                compareVersionStrings(system, candidate.patch.ifBlank { candidate.name }) < 0
            else -> false
        }

        val targetName = if (updateAvailable) candidate?.displayName else null

        return UpdateStatus(
            installed = system,
            installedMatch = match,
            latestStable = stable,
            latestBeta = beta,
            newestInCatalog = newest,
            updateAvailable = updateAvailable,
            updateTargetName = targetName
        )
    }

    companion object {
        /** Numeric key for ordering 1.26.70.20 > 1.26.50 */
        fun versionKey(raw: String): List<Int> =
            Regex("\\d+").findAll(raw).map { it.value.toIntOrNull() ?: 0 }.toList()

        fun compareVersionStrings(a: String, b: String): Int {
            val pa = versionKey(a)
            val pb = versionKey(b)
            val n = maxOf(pa.size, pb.size)
            for (i in 0 until n) {
                val x = pa.getOrElse(i) { 0 }
                val y = pb.getOrElse(i) { 0 }
                if (x != y) return x.compareTo(y)
            }
            return 0
        }
    }
}
