package com.example.storyforge.save

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

inline fun <reified T> readJsonLogLines(context: Context, fileName: String): List<T> {
    val file = File(context.filesDir, fileName)
    if (!file.exists()) return emptyList()
    return file.readLines().mapNotNull {
        try {
            Json.decodeFromString<T>(it)
        } catch (_: Exception) {
            null
        }
    }
}
