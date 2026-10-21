package com.openmojang.launcher.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reads and writes Minecraft Bedrock options.txt.
 * Auto-finds the file when All files access is granted — no picker required.
 * Creates options.txt with defaults when none exists.
 */
class OptionsEditor(private val context: Context) {

    private val prefs = context.getSharedPreferences("options_editor", Context.MODE_PRIVATE)

    var pickedUri: Uri?
        get() = prefs.getString(KEY_URI, null)?.let { Uri.parse(it) }
        set(value) {
            prefs.edit().putString(KEY_URI, value?.toString()).apply()
        }

    fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun openAllFilesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun candidateFiles(): List<File> {
        val out = mutableListOf<File>()
        val roots = mutableListOf<File>()
        Environment.getExternalStorageDirectory()?.let { roots += it }
        context.getExternalFilesDir(null)?.parentFile?.parentFile?.let { roots += it }

        val relative = listOf(
            "games/com.mojang/minecraftpe/options.txt",
            "Android/data/com.mojang.minecraftpe/files/games/com.mojang/minecraftpe/options.txt",
            "Android/data/com.mojang.minecraftpe/files/minecraftpe/options.txt"
        )
        for (root in roots.distinct()) {
            for (rel in relative) {
                val f = File(root, rel)
                if (f.isFile) out += f
            }
            if (hasFullStorageAccess()) {
                try {
                    val games = File(root, "games/com.mojang")
                    if (games.isDirectory) {
                        games.walkTopDown().maxDepth(5).forEach { f ->
                            if (f.isFile && f.name.equals("options.txt", true)) out += f
                        }
                    }
                    val adata = File(root, "Android/data/com.mojang.minecraftpe")
                    if (adata.isDirectory) {
                        adata.walkTopDown().maxDepth(7).forEach { f ->
                            if (f.isFile && f.name.equals("options.txt", true)) out += f
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        val mirror = File(context.getExternalFilesDir(null), "minecraftpe/options.txt")
        if (mirror.isFile) out += mirror
        return out.distinctBy { it.absolutePath }
    }

    fun loadFromFile(file: File): Map<String, String>? {
        if (!file.isFile || !file.canRead()) return null
        return parse(file.readText())
    }

    fun loadFromUri(uri: Uri): Map<String, String>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                parse(input.bufferedReader().readText())
            }
        } catch (_: Exception) {
            null
        }
    }

    fun preferredGameOptionsFile(): File {
        val root = Environment.getExternalStorageDirectory()
            ?: File("/storage/emulated/0")
        return File(root, "games/com.mojang/minecraftpe/options.txt")
    }

    fun defaultOptions(): Map<String, String> = linkedMapOf(
        "gfx_viewdistance" to "192",
        "gfx_field_of_view" to "60",
        "gfx_gamma" to "0.50",
        "gfx_fancygraphics" to "1",
        "gfx_smoothlighting" to "1",
        "gfx_fancyskies" to "1",
        "gfx_viewbobbing" to "1",
        "gfx_async_texture_loads" to "1",
        "gfx_msaa" to "2",
        "gfx_max_framerate" to "0",
        "gfx_vsync" to "0",
        "gfx_multithreaded_renderer" to "1",
        "gfx_particleviewdistance" to "100",
        "gfx_graphics_api" to "0",
        "graphics_api" to "default",
        "preferred_graphics_backend" to "default",
        "gfx_prefer_vulkan" to "0"
    )

    fun ensureOptionsFile(): Pair<String, Map<String, String>>? {
        val defaults = defaultOptions()
        val targets = mutableListOf<File>()
        if (hasFullStorageAccess()) {
            targets += preferredGameOptionsFile()
            targets += File(
                Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0"),
                "Android/data/com.mojang.minecraftpe/files/games/com.mojang/minecraftpe/options.txt"
            )
        }
        targets += File(context.getExternalFilesDir(null), "minecraftpe/options.txt")

        for (f in targets) {
            try {
                if (f.isFile && f.canRead()) {
                    loadFromFile(f)?.let { return f.absolutePath to it }
                }
                f.parentFile?.mkdirs()
                if (saveToFile(f, defaults)) {
                    return f.absolutePath to defaults
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun loadBest(): Pair<String, Map<String, String>>? {
        pickedUri?.let { uri ->
            loadFromUri(uri)?.let { return "SAF" to it }
        }
        for (f in candidateFiles()) {
            loadFromFile(f)?.let { return f.absolutePath to it }
        }
        return ensureOptionsFile()
    }

    fun saveToFile(file: File, options: Map<String, String>): Boolean {
        return try {
            val text = serialize(options)
            file.parentFile?.mkdirs()
            file.writeText(text)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun saveToUri(uri: Uri, options: Map<String, String>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(serialize(options).toByteArray())
            } != null
        } catch (_: Exception) {
            false
        }
    }

    fun saveBest(options: Map<String, String>): String? {
        pickedUri?.let { uri ->
            if (saveToUri(uri, options)) return "Saved via file access"
        }
        for (f in candidateFiles()) {
            if ((f.canWrite() || !f.exists()) && saveToFile(f, options)) {
                return "Saved to ${f.absolutePath}"
            }
        }
        if (hasFullStorageAccess()) {
            val game = preferredGameOptionsFile()
            if (saveToFile(game, options)) {
                return "Created and saved ${game.absolutePath}"
            }
        }
        val mirror = File(context.getExternalFilesDir(null), "minecraftpe/options.txt")
        if (saveToFile(mirror, options)) {
            return "Saved mirror: ${mirror.absolutePath} (grant All files access to write into Minecraft folder)"
        }
        return null
    }

    fun backup(options: Map<String, String>): String? {
        return try {
            val dir = File(context.getExternalFilesDir(null), "minecraftpe/backups").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "options_$stamp.txt")
            file.writeText(serialize(options))
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun listBackups(): List<File> {
        val dir = File(context.getExternalFilesDir(null), "minecraftpe/backups")
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    fun forceStopMinecraft(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(MC_PACKAGE)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deviceReportsVulkan(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature("android.hardware.vulkan.level") ||
            pm.hasSystemFeature("android.hardware.vulkan.version") ||
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) ||
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    }

    fun vulkanLevelLabel(): String {
        if (!deviceReportsVulkan()) return "No Vulkan feature flag"
        return try {
            if (Build.VERSION.SDK_INT >= 24) {
                val level = context.packageManager.systemAvailableFeatures
                    .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }
                    ?.version
                val ver = context.packageManager.systemAvailableFeatures
                    .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                    ?.version
                buildString {
                    append("Vulkan available")
                    if (level != null) append(" · level $level")
                    if (ver != null) append(" · 0x${Integer.toHexString(ver)}")
                }
            } else "Vulkan feature present"
        } catch (_: Exception) {
            "Vulkan available"
        }
    }

    fun applyPreset(base: Map<String, String>, preset: Preset): Map<String, String> {
        val m = base.toMutableMap()
        when (preset) {
            Preset.PERFORMANCE -> {
                m["gfx_fancygraphics"] = "0"
                m["gfx_smoothlighting"] = "0"
                m["gfx_fancyskies"] = "0"
                m["gfx_msaa"] = "1"
                m["gfx_viewdistance"] = "96"
                m["gfx_particleviewdistance"] = "50"
                m["gfx_max_framerate"] = "60"
                m["gfx_vsync"] = "0"
                m["gfx_multithreaded_renderer"] = "1"
                m["gfx_async_texture_loads"] = "1"
                m["gfx_viewbobbing"] = "0"
                setGraphicsApi(m, GraphicsApi.OPENGLES)
            }
            Preset.BALANCED -> {
                m["gfx_fancygraphics"] = "1"
                m["gfx_smoothlighting"] = "1"
                m["gfx_fancyskies"] = "1"
                m["gfx_msaa"] = "2"
                m["gfx_viewdistance"] = "192"
                m["gfx_particleviewdistance"] = "100"
                m["gfx_max_framerate"] = "0"
                m["gfx_vsync"] = "0"
                m["gfx_multithreaded_renderer"] = "1"
                m["gfx_async_texture_loads"] = "1"
                m["gfx_viewbobbing"] = "1"
                setGraphicsApi(m, GraphicsApi.DEFAULT)
            }
            Preset.QUALITY -> {
                m["gfx_fancygraphics"] = "1"
                m["gfx_smoothlighting"] = "1"
                m["gfx_fancyskies"] = "1"
                m["gfx_msaa"] = "4"
                m["gfx_viewdistance"] = "256"
                m["gfx_particleviewdistance"] = "100"
                m["gfx_max_framerate"] = "0"
                m["gfx_vsync"] = "0"
                m["gfx_multithreaded_renderer"] = "1"
                m["gfx_async_texture_loads"] = "1"
                m["gfx_viewbobbing"] = "1"
                setGraphicsApi(m, GraphicsApi.VULKAN)
            }
            Preset.FULLBRIGHT -> {
                m["gfx_gamma"] = "1"
                m["gfx_smoothlighting"] = "0"
            }
        }
        return m
    }

    fun setGraphicsApi(m: MutableMap<String, String>, api: GraphicsApi) {
        when (api) {
            GraphicsApi.DEFAULT -> {
                m["gfx_graphics_api"] = "0"
                m["graphics_api"] = "default"
                m["preferred_graphics_backend"] = "default"
                m["gfx_prefer_vulkan"] = "0"
            }
            GraphicsApi.OPENGLES -> {
                m["gfx_graphics_api"] = "1"
                m["graphics_api"] = "opengles"
                m["preferred_graphics_backend"] = "opengl"
                m["gfx_prefer_vulkan"] = "0"
            }
            GraphicsApi.VULKAN -> {
                m["gfx_graphics_api"] = "2"
                m["graphics_api"] = "vulkan"
                m["preferred_graphics_backend"] = "vulkan"
                m["gfx_prefer_vulkan"] = "1"
            }
        }
    }

    fun graphicsApiOf(options: Map<String, String>): GraphicsApi {
        val v = (options["preferred_graphics_backend"]
            ?: options["graphics_api"]
            ?: options["gfx_graphics_api"]
            ?: "").lowercase()
        return when {
            v.contains("vulkan") || v == "2" || options["gfx_prefer_vulkan"] == "1" -> GraphicsApi.VULKAN
            v.contains("opengl") || v.contains("gles") || v == "1" -> GraphicsApi.OPENGLES
            else -> GraphicsApi.DEFAULT
        }
    }

    private fun parse(text: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        text.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) return@forEach
            val idx = t.indexOf(':')
            if (idx <= 0) return@forEach
            val key = t.substring(0, idx).trim()
            val value = t.substring(idx + 1).trim()
            map[key] = value
        }
        return map
    }

    private fun serialize(options: Map<String, String>): String {
        return options.entries.joinToString("\n") { (k, v) -> "$k:$v" } + "\n"
    }

    enum class Preset { PERFORMANCE, BALANCED, QUALITY, FULLBRIGHT }
    enum class GraphicsApi { DEFAULT, OPENGLES, VULKAN }

    companion object {
        private const val KEY_URI = "options_uri"
        const val MC_PACKAGE = "com.mojang.minecraftpe"

        fun optionsMimeTypes(): Array<String> = arrayOf("text/plain", "*/*")
    }
}
