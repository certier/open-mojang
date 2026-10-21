package com.openmojang.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class LocalSkin(
    val id: String,
    val name: String,
    val path: String,
    val isSteve: Boolean = false,
    val active: Boolean = false
)

class SkinManager(private val context: Context) {

    private val skinsDir = File(context.filesDir, "skins").apply { mkdirs() }
    private val indexFile = File(skinsDir, "index.json")
    private val activeFile = File(skinsDir, "active.txt")

    fun list(): List<LocalSkin> {
        ensureBuiltinSkins()
        val active = activeId()
        return readAll().map { it.copy(active = it.id == active) }
    }

    private fun ensureBuiltinSkins() {
        val stevePng = File(skinsDir, "steve.png")
        val alexPng = File(skinsDir, "alex.png")
        if (!stevePng.isFile) writeClassicSkin(stevePng, steve = true)
        if (!alexPng.isFile) writeClassicSkin(alexPng, steve = false)
        if (!indexFile.exists() || readAll().none { it.path.isNotEmpty() }) {
            saveIndex(
                listOf(
                    LocalSkin("steve", "Steve", stevePng.absolutePath, isSteve = false),
                    LocalSkin("alex", "Alex", alexPng.absolutePath, isSteve = false)
                )
            )
            if (activeId() == null) activeFile.writeText("steve")
        }
    }

    private fun writeClassicSkin(file: File, steve: Boolean) {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint()
        fun rect(x: Int, y: Int, w: Int, h: Int, color: Int) {
            paint.color = color
            c.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
        }
        val skin = Color.rgb(198, 134, 98)
        val hair = if (steve) Color.rgb(62, 40, 22) else Color.rgb(214, 126, 56)
        val shirt = if (steve) Color.rgb(0, 170, 170) else Color.rgb(214, 100, 160)
        val pants = if (steve) Color.rgb(50, 50, 160) else Color.rgb(90, 70, 150)
        val shoe = Color.rgb(40, 40, 40)
        rect(0, 0, 64, 64, Color.TRANSPARENT)
        rect(8, 8, 8, 8, skin)
        rect(8, 0, 8, 8, hair)
        rect(0, 8, 8, 8, hair)
        rect(16, 8, 8, 8, hair)
        rect(20, 20, 8, 12, shirt)
        rect(20, 32, 8, 12, pants)
        rect(44, 20, 4, 12, shirt)
        rect(4, 20, 4, 12, skin)
        rect(4, 32, 4, 12, pants)
        rect(20, 52, 4, 12, pants)
        rect(4, 44, 4, 4, shoe)
        rect(8, 12, 1, 1, Color.WHITE)
        rect(11, 12, 1, 1, Color.WHITE)
        rect(8, 15, 4, 1, Color.rgb(120, 60, 40))
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }

    fun active(): LocalSkin? = list().firstOrNull { it.active } ?: list().firstOrNull()

    fun setActive(id: String) {
        activeFile.writeText(id)
    }

    fun activeId(): String? =
        if (activeFile.isFile) activeFile.readText().trim().ifBlank { null } else null

    fun importPng(uri: Uri, name: String): LocalSkin? {
        return try {
            val id = UUID.randomUUID().toString().take(8)
            val dest = File(skinsDir, "$id.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { out -> input.copyTo(out) }
            } ?: return null
            if (dest.length() < 50) {
                dest.delete()
                return null
            }
            val skin = LocalSkin(id, name.ifBlank { "Custom" }, dest.absolutePath)
            val all = readAll().toMutableList()
            all += skin
            saveIndex(all)
            setActive(id)
            skin
        } catch (_: Exception) {
            null
        }
    }

    fun delete(id: String) {
        if (id == "steve" || id == "alex") return
        val all = readAll().filter { it.id != id }
        saveIndex(all)
        File(skinsDir, "$id.png").delete()
        if (activeId() == id) activeFile.writeText(all.firstOrNull()?.id ?: "steve")
    }

    fun headBitmap(skin: LocalSkin): Bitmap? {
        val path = skin.path
        if (path.isEmpty()) return null
        val file = File(path)
        if (!file.isFile) return null
        val full = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return try {
            if (full.width < 16 || full.height < 16) full
            else Bitmap.createBitmap(full, 8, 8, 8, 8)
        } catch (_: Exception) {
            full
        }
    }

    fun applyToMinecraft(skin: LocalSkin): String {
        OptionsEditor(context).forceStopMinecraft()
        val src = File(skin.path)
        if (!src.isFile || src.length() < 50) return "Skin file missing (${skin.path})"
        if (!OptionsEditor(context).hasFullStorageAccess()) {
            return "All files access is off — grant it, then apply again"
        }
        val game = Environment.getExternalStorageDirectory()?.let {
            File(it, "games/com.mojang/minecraftpe/custom.png")
        } ?: return "No shared storage"
        return try {
            game.parentFile?.mkdirs()
            src.copyTo(game, overwrite = true)
            if (!game.isFile || game.length() < 50) {
                "Write did not stick at ${game.absolutePath}"
            } else {
                "Wrote ${game.length()} bytes to ${game.absolutePath}. In Minecraft open Dressing Room and pick this custom skin. A Microsoft persona skin overrides it while signed into Xbox."
            }
        } catch (e: Exception) {
            "Skin write failed: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    private fun readAll(): List<LocalSkin> {
        if (!indexFile.isFile) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LocalSkin(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    path = o.optString("path", ""),
                    isSteve = o.optBoolean("isSteve", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveIndex(skins: List<LocalSkin>) {
        val arr = JSONArray()
        skins.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("path", s.path)
                    .put("isSteve", s.isSteve)
            )
        }
        indexFile.writeText(arr.toString())
    }
}
