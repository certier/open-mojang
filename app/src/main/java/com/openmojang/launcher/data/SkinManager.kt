package com.openmojang.launcher.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class LocalSkin(
    val id: String,
    val name: String,
    val path: String,
    val isSteve: Boolean = false
)

/**
 * Manages local skin PNGs under app filesDir/skins.
 * Apply-to-account requires a live Xbox token from AuthManager.
 */
class SkinManager(private val context: Context) {

    private val skinsDir = File(context.filesDir, "skins").apply { mkdirs() }
    private val indexFile = File(skinsDir, "index.json")

    fun list(): List<LocalSkin> {
        if (!indexFile.exists()) {
            // Seed Steve default
            val steve = LocalSkin("steve", "Steve", "", isSteve = true)
            saveIndex(listOf(steve))
            return listOf(steve)
        }
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

    fun importFromUri(uri: Uri, name: String): LocalSkin? {
        return try {
            val id = "skin_${System.currentTimeMillis()}"
            val out = File(skinsDir, "$id.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            } ?: return null
            val skin = LocalSkin(id, name, out.absolutePath)
            val updated = list().filter { !it.isSteve || true } + skin
            saveIndex(updated)
            skin
        } catch (_: Exception) {
            null
        }
    }

    fun delete(id: String) {
        val current = list().filter { it.id != id }
        list().find { it.id == id }?.path?.let { path ->
            if (path.isNotEmpty()) File(path).delete()
        }
        saveIndex(current)
    }

    /**
     * Apply skin to the signed-in Microsoft account.
     * Requires a real access token; returns false in demo mode.
     */
    suspend fun applyToAccount(skin: LocalSkin, account: MsAccount): Boolean {
        if (account.accessToken.isEmpty() || account.accessToken == "demo-token") {
            return false // need real MSAL token
        }
        // Real path: POST multipart PNG to Minecraft services skin endpoint
        // with Authorization: Bearer <xbox/minecraft token>
        return false
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
