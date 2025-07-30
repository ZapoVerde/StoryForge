package com.example.storyforge.save

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

object GameSnapshotManager {

    private const val SNAPSHOT_DIR = "snapshots"

    private fun snapshotDir(context: Context): File =
        File(context.filesDir, SNAPSHOT_DIR).apply { mkdirs() }

    fun saveSnapshot(context: Context, snapshot: GameSnapshot, name: String): File {
        val fileName = "$name.json"
        val file = File(snapshotDir(context), fileName)
        val json = Json.encodeToString(snapshot)
        file.writeText(json)
        return file
    }

    fun loadSnapshot(context: Context, name: String): GameSnapshot? {
        val file = File(snapshotDir(context), "$name.json")
        return if (file.exists()) {
            try {
                val json = file.readText()
                Json.decodeFromString<GameSnapshot>(json)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    fun listSnapshots(context: Context): List<File> {
        return snapshotDir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteSnapshot(context: Context, name: String): Boolean {
        val file = File(snapshotDir(context), "$name.json")
        return file.exists() && file.delete()
    }

    fun createTimestampedName(base: String): String {
        val safeBase = base.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "$safeBase-${Instant.now().toString().replace(":", "-")}"
    }
}
