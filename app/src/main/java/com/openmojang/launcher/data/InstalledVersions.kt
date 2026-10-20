package com.openmojang.launcher.data

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class InstalledBuild(
    val id: String,
    val name: String,
    val packageName: String,
    val apkPath: String,
    val installedAt: Long
)

/**
 * Tracks Minecraft APKs the user has downloaded through Open Mojang
 * and detects the system-installed com.mojang.minecraftpe package.
 */
class InstalledVersions(private val context: Context) {

    private val storeFile = File(context.filesDir, "installed_versions.json")

    fun listTracked(): List<InstalledBuild> {
        if (!storeFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(storeFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        InstalledBuild(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            packageName = o.optString("packageName", "com.mojang.minecraftpe"),
                            apkPath = o.optString("apkPath", ""),
                            installedAt = o.optLong("installedAt", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun track(build: InstalledBuild) {
        val current = listTracked().filter { it.id != build.id }.toMutableList()
        current.add(0, build)
        val arr = JSONArray()
        current.forEach { b ->
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("name", b.name)
                    .put("packageName", b.packageName)
                    .put("apkPath", b.apkPath)
                    .put("installedAt", b.installedAt)
            )
        }
        storeFile.writeText(arr.toString())
    }

    fun untrack(id: String) {
        val current = listTracked().filter { it.id != id }
        val arr = JSONArray()
        current.forEach { b ->
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("name", b.name)
                    .put("packageName", b.packageName)
                    .put("apkPath", b.apkPath)
                    .put("installedAt", b.installedAt)
            )
        }
        storeFile.writeText(arr.toString())
    }

    /** Returns versionName of system Minecraft if installed, else null. */
    fun systemMinecraftVersion(): String? {
        return try {
            val info = context.packageManager.getPackageInfo("com.mojang.minecraftpe", 0)
            info.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isSystemMinecraftInstalled(): Boolean = systemMinecraftVersion() != null

    fun launchSystemMinecraft(): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
            ?: return false
        context.startActivity(launch)
        return true
    }
}
