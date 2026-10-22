package com.openmojang.launcher.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class McPack(
    val id: String,
    val name: String,
    val path: String,
    val kind: String,
    val enabled: Boolean,
    val sizeBytes: Long
)

/**
 * Scans Bedrock resource/behavior packs under games/com.mojang
 * and imports .mcpack / .zip via All-files access.
 */
class ModManager(private val context: Context) {

    private val editor = OptionsEditor(context)

    fun hasAccess(): Boolean = editor.hasFullStorageAccess()

    fun openAccessSettings() = editor.openAllFilesSettings()

    private fun root(): File {
        val base = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
        return File(base, "games/com.mojang")
    }

    private fun packDirs(): List<Pair<String, File>> {
        val r = root()
        return listOf(
            "resource" to File(r, "resource_packs"),
            "behavior" to File(r, "behavior_packs")
        )
    }

    private fun disabledDir(kind: String): File {
        val r = root()
        val name = if (kind == "behavior") "behavior_packs_disabled" else "resource_packs_disabled"
        return File(r, name).also { it.mkdirs() }
    }

    fun listPacks(): List<McPack> {
        if (!hasAccess()) return emptyList()
        val out = mutableListOf<McPack>()
        for ((kind, dir) in packDirs()) {
            if (!dir.isDirectory) continue
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory) {
                    out += McPack(
                        id = "${kind}:${f.name}",
                        name = f.name,
                        path = f.absolutePath,
                        kind = kind,
                        enabled = true,
                        sizeBytes = dirSize(f)
                    )
                }
            }
            val off = disabledDir(kind)
            if (off.isDirectory) {
                off.listFiles()?.forEach { f ->
                    if (f.isDirectory) {
                        out += McPack(
                            id = "${kind}-off:${f.name}",
                            name = f.name,
                            path = f.absolutePath,
                            kind = kind,
                            enabled = false,
                            sizeBytes = dirSize(f)
                        )
                    }
                }
            }
        }
        return out.sortedBy { it.name.lowercase() }
    }

    fun setEnabled(pack: McPack, enable: Boolean): String {
        if (!hasAccess()) return "Need All files access"
        val src = File(pack.path)
        if (!src.isDirectory) return "Pack folder missing"
        val kind = pack.kind
        val r = root()
        val active = File(r, if (kind == "behavior") "behavior_packs" else "resource_packs")
        val off = disabledDir(kind)
        active.mkdirs()
        off.mkdirs()
        val destParent = if (enable) active else off
        val dest = File(destParent, src.name)
        if (src.absolutePath == dest.absolutePath) return "Already ${if (enable) "enabled" else "disabled"}"
        return try {
            if (dest.exists()) dest.deleteRecursively()
            val ok = src.renameTo(dest)
            if (!ok) {
                src.copyRecursively(dest, overwrite = true)
                src.deleteRecursively()
            }
            if (enable) "Enabled ${pack.name}" else "Disabled ${pack.name}"
        } catch (e: Exception) {
            "Failed: ${e.message}"
        }
    }

    fun deletePack(pack: McPack): String {
        if (!hasAccess()) return "Need All files access"
        return try {
            val f = File(pack.path)
            if (f.isDirectory) f.deleteRecursively() else f.delete()
            "Deleted ${pack.name}"
        } catch (e: Exception) {
            "Delete failed: ${e.message}"
        }
    }

    fun importPack(uri: Uri, preferBehavior: Boolean = false): String {
        if (!hasAccess()) return "Need All files access"
        val r = root()
        val destRoot = File(
            r,
            if (preferBehavior) "behavior_packs" else "resource_packs"
        ).also { it.mkdirs() }
        val tmp = File(context.cacheDir, "import_${System.currentTimeMillis()}.zip")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { out -> input.copyTo(out) }
            } ?: return "Could not read file"
            val folderName = guessFolderName(uri, tmp)
            val dest = File(destRoot, folderName)
            if (dest.exists()) dest.deleteRecursively()
            dest.mkdirs()
            unzip(tmp, dest)
            tmp.delete()
            "Imported $folderName → ${destRoot.name}"
        } catch (e: Exception) {
            tmp.delete()
            "Import failed: ${e.message}"
        }
    }

    private fun guessFolderName(uri: Uri, tmp: File): String {
        val fromUri = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.removeSuffix(".mcpack")
            ?.removeSuffix(".zip")
            ?.removeSuffix(".mcaddon")
        if (!fromUri.isNullOrBlank() && fromUri != "primary") return fromUri.take(64)
        return "pack_${tmp.nameWithoutExtension.takeLast(8)}"
    }

    private fun unzip(zip: File, dest: File) {
        ZipInputStream(BufferedInputStream(zip.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(dest, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun dirSize(dir: File): Long {
        var total = 0L
        dir.walkTopDown().forEach { if (it.isFile) total += it.length() }
        return total
    }
}
