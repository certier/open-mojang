package com.openmojang.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

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
        val active = activeId()
        if (!indexFile.exists()) {
            val steve = LocalSkin("steve", "Steve", "", isSteve = true, active = active == null || active == "steve")
            saveIndex(listOf(steve))
            if (active == null) setActive("steve")
            return listOf(steve.copy(active = activeId() == "steve"))
        }
        return readAll().map { it.copy(active = it.id == active) }
    }

    fun active(): LocalSkin? = list().firstOrNull { it.active } ?: list().firstOrNull()

    fun setActive(id: String) {
        activeFile.writeText(id)
    }

    fun importFromUri(uri: Uri, name: String): LocalSkin? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            if (bitmap.width < 64 || bitmap.height < 32) return null
            val id = "skin_${System.currentTimeMillis()}"
            val out = File(skinsDir, "$id.png")
            FileOutputStream(out).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            val skin = LocalSkin(id, name.ifBlank { "Custom skin" }, out.absolutePath)
            saveIndex(readAll().filter { it.id != id } + skin)
            setActive(id)
            skin.copy(active = true)
        } catch (_: Exception) {
            null
        }
    }

    fun delete(id: String) {
        if (id == "steve") return
        readAll().find { it.id == id }?.path?.let { path ->
            if (path.isNotEmpty()) File(path).delete()
        }
        saveIndex(readAll().filter { it.id != id })
        if (activeId() == id) setActive("steve")
    }

    fun headBitmap(skin: LocalSkin): Bitmap? {
        if (skin.path.isEmpty() || !File(skin.path).exists()) return null
        val full = BitmapFactory.decodeFile(skin.path) ?: return null
        val scale = full.width / 64f
        val size = (8 * scale).toInt().coerceAtLeast(8)
        val x = (8 * scale).toInt()
        val y = (8 * scale).toInt()
        if (x + size > full.width || y + size > full.height) return full
        return Bitmap.createBitmap(full, x, y, size, size)
    }

    private fun activeId(): String? =
        if (activeFile.exists()) activeFile.readText().trim().ifEmpty { null } else null

    private fun readAll(): List<LocalSkin> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        LocalSkin(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            path = o.optString("path", ""),
                            isSteve = o.optBoolean("isSteve", false)
                        )
                    )
                }
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
