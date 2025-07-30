package com.example.storyforge.network

import android.util.Log
import com.example.storyforge.settings.AiConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

suspend fun testConnection(connection: AiConnection): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null

        try {
            // Setup connection
            val fullUrl = connection.apiUrl.trimEnd('/') + "/chat/completions"
            Log.d("AiConnectionTester", "Testing connection to: ${connection.displayName}")
            Log.d("AiConnectionTester", "  Endpoint: $fullUrl")
            Log.d("AiConnectionTester", "  Model: ${connection.modelName} (${connection.modelSlug})")

            val url = URL(fullUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer ${connection.apiToken}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", connection.userAgent)
                doOutput = true
                connectTimeout = 10000
                readTimeout = 30000
            }

            // Prepare request body
            val body = """
                {
                    "model": "${connection.modelSlug}",
                    "messages": [{
                        "role": "user",
                        "content": "This is a connection test from StoryForge"
                    }],
                    "stream": false,
                    "max_tokens": 15
                }
            """.trimIndent()

            // Send request
            conn.outputStream.bufferedWriter().use { writer ->
                writer.write(body)
                writer.flush()
            }

            // Get response
            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            }

            Log.d("AiConnectionTester", "  Response status: $responseCode")
            Log.d("AiConnectionTester", "  Response size: ${responseText.length} chars")

            // Parse response
            val json = Json.parseToJsonElement(responseText).jsonObject
            val firstChoice = json["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            val message = firstChoice?.get("message")?.jsonObject

            // Check both standard and DeepSeek-specific response fields
            val content = message?.get("content")?.jsonPrimitive?.contentOrNull
            val reasoningContent = message?.get("reasoning_content")?.jsonPrimitive?.contentOrNull

            val isValid = !content.isNullOrBlank() || !reasoningContent.isNullOrBlank()

            Log.d("AiConnectionTester", "  Connection test ${if (isValid) "succeeded" else "failed"}")
            Log.d("AiConnectionTester", "  Standard content: ${content ?: "empty"}")
            Log.d("AiConnectionTester", "  Reasoning content: ${reasoningContent ?: "empty"}")

            return@withContext isValid

        } catch (e: Exception) {
            Log.e("AiConnectionTester", "Connection test failed", e)
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}