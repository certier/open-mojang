package com.openmojang.launcher.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/**
 * Reads and writes Minecraft Bedrock options.txt.
 * Keys match what the game stores (gfx_*, etc.).
 * Access: known paths first, then SAF Uri the user picked.
 */
class OptionsEditor(private val context: Context) {

    private val prefs = context.getSharedPreferences("options_editor", Context.MODE_PRIVATE)

    var pickedUri: Uri?
        get() = prefs.getString(KEY_URI, null)?.let { Uri.parse(it) }
        set(value) {
            prefs.edit().putString(KEY_URI, value?.toString()).apply()
        }

    fun candidateFiles(): List<File> {
        val out = mutableListOf<File>()
        val roots = mutableListOf<File>()
        Environment.getExternalStorageDirectory()?.let { roots += it }
        context.getExternalFilesDir(null)?.parentFile?.parentFile?.let { roots += it }
        // Common Bedrock locations
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
        }
        // Scoped: our own external mirror if user copied the file
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

    fun loadBest(): Pair<String, Map<String, String>>? {
        pickedUri?.let { uri ->
            loadFromUri(uri)?.let { return "SAF" to it }
        }
        for (f in candidateFiles()) {
            loadFromFile(f)?.let { return f.absolutePath to it }
        }
        return null
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
            if (f.canWrite() && saveToFile(f, options)) {
                return "Saved to ${f.absolutePath}"
            }
        }
        // Write a mirror the user can copy
        val mirror = File(context.getExternalFilesDir(null), "minecraftpe/options.txt")
        if (saveToFile(mirror, options)) {
            return "Saved mirror: ${mirror.absolutePath} (copy into Minecraft folder if needed)"
        }
        return null
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
                m["gfx_max_framerate"] = "60"
                m["gfx_vsync"] = "0"
                m["gfx_multithreaded_renderer"] = "1"
                setGraphicsApi(m, GraphicsApi.OPENGLES)
            }
            Preset.BALANCED -> {
                m["gfx_fancygraphics"] = "1"
                m["gfx_smoothlighting"] = "1"
                m["gfx_fancyskies"] = "1"
                m["gfx_msaa"] = "2"
                m["gfx_viewdistance"] = "192"
                m["gfx_max_framerate"] = "0"
                m["gfx_vsync"] = "0"
                m["gfx_multithreaded_renderer"] = "1"
                setGraphicsApi(m, GraphicsApi.DEFAULT)
            }
            Preset.QUALITY -> {
                m["gfx_fancygraphics"] = "1"
                m["gfx_smoothlighting"] = "1"
                m["gfx_fancyskies"] = "1"
                m["gfx_msaa"] = "4"
                m["gfx_viewdistance"] = "256"
                m["gfx_max_framerate"] = "0"
                m["gfx_vsync"] = "0"
                m["gfx_multithreaded_renderer"] = "1"
                setGraphicsApi(m, GraphicsApi.VULKAN)
            }
        }
        return m
    }

    fun setGraphicsApi(m: MutableMap<String, String>, api: GraphicsApi) {
        // Bedrock / RenderDragon builds vary; write known + experimental keys
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

    enum class Preset { PERFORMANCE, BALANCED, QUALITY }
    enum class GraphicsApi { DEFAULT, OPENGLES, VULKAN }

    companion object {
        private const val KEY_URI = "options_uri"

        /** Document tree under Android/data is blocked; user picks the file. */
        fun optionsMimeTypes(): Array<String> = arrayOf("text/plain", "*/*")
    }
}
