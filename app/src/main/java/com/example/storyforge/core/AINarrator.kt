// file: com/example/storyforge/core/AINarrator.kt
package com.example.storyforge.core

import android.content.Context
import android.util.Log
import com.example.storyforge.logging.NarratorStackLog
import com.example.storyforge.model.DeltaInstruction
import com.example.storyforge.model.Message
import com.example.storyforge.prompt.AiSettings
import com.example.storyforge.settings.AiConnection
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- Retrofit API Definition ---
internal interface AINarratorApiService {
    @POST("chat/completions")
    suspend fun generateNarration(
        @Header("Authorization") auth: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

// --- API DTOs (External Format) ---
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float,
    val top_p: Float,
    val max_tokens: Int,
    val presence_penalty: Float,
    val frequency_penalty: Float,
    val stream: Boolean = false
)

@Serializable
internal data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
internal data class Choice(
    val message: Message
)

// --- Result Container ---
data class NarratorResult(
    val narration: String,
    val deltas: Map<String, DeltaInstruction>,
    val apiRequestBody: String,
    val apiResponseBody: String,
    val apiUrl: String,
    val latencyMs: Long,
    val modelSlugUsed: String
)

// --- Extension ---
fun String.ensureEndsWithSlash(): String = if (endsWith("/")) this else "$this/"

// --- Main Client ---
class AINarrator private constructor(
    private val apiService: AINarratorApiService,
    private val apiKey: String,
    private val isDebug: Boolean,
    private val baseUrl: String,
    private val modelSlug: String
) : Narrator {

    override suspend fun generateFull(
        messages: List<Message>,
        settings: AiSettings,
        modelName: String,
        turnId: Int,
        context: Context
    ): Result<NarratorResult> {
        if (apiKey == "MISSING_API_KEY") {
            return Result.failure(IllegalStateException("API key not configured"))
        }

        return try {
            val startTime = System.nanoTime()

            val request = ChatCompletionRequest(
                model = modelSlug,
                messages = messages,
                temperature = settings.temperature,
                top_p = settings.topP,
                max_tokens = settings.maxTokens,
                presence_penalty = settings.presencePenalty,
                frequency_penalty = settings.frequencyPenalty,
                stream = false
            )

            val requestJson = Json.encodeToString(request.copy(messages = emptyList()))
            val apiUrl = baseUrl + "chat/completions"

            val response = apiService.generateNarration(
                auth = "Bearer $apiKey",
                request = request
            )

            val responseJson = Json.encodeToString(response.copy(choices = emptyList()))
            val durationMs = (System.nanoTime() - startTime) / 1_000_000

            val raw = response.choices.firstOrNull()?.message?.content?.trim()
                ?: return Result.failure(Exception("Empty response from API"))

            DebugLogger.writeText(context, "turn_%04d_raw.txt".format(turnId), raw)

            NarratorStackLog.append(
                context = context,
                turn = turnId,
                model = modelName,
                messages = messages,
                latencyMs = durationMs,
                inputTokens = estimateTokens(messages),
                outputTokens = estimateTokens(raw)
            )

            val (narration, deltas, digestLines) = NarrationParser.extractJsonAndCleanNarration(raw)
            DigestManager.addParsedLines(context, turnId + 1, narration, deltas)
            logDeltas(deltas)

            Result.success(
                NarratorResult(
                    narration = narration,
                    deltas = deltas,
                    apiRequestBody = requestJson,
                    apiResponseBody = responseJson,
                    apiUrl = apiUrl,
                    latencyMs = durationMs,
                    modelSlugUsed = modelSlug
                )
            )
        } catch (ex: Exception) {
            Log.e("AINarrator", "API call failed", ex)
            Result.failure(Exception("Network error: ${ex.localizedMessage}"))
        }
    }

    override suspend fun generate(
        messages: List<Message>,
        settings: AiSettings,
        modelName: String,
        turnId: Int,
        context: Context
    ): Result<Pair<String, Map<String, DeltaInstruction>>> {
        return generateFull(messages, settings, modelName, turnId, context)
            .map { it.narration to it.deltas }
    }

    fun logDeltas(deltas: Map<String, DeltaInstruction>) {
        for ((key, instruction) in deltas) {
            Log.d("NarrationParser", "Delta: $key => ${instruction.toLogValue()}")
        }
    }

    private fun estimateTokens(messages: List<Message>): Int {
        return messages.sumOf { it.content.length / 4 }
    }

    private fun estimateTokens(response: String): Int {
        return response.length / 4
    }

    companion object {
        fun create(
            apiKey: String = "MISSING_API_KEY",
            isDebug: Boolean = false,
            baseUrl: String = "https://api.deepseek.com/v1/",
            modelSlug: String
        ): AINarrator {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = if (isDebug) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                })
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return AINarrator(
                apiService = retrofit.create(AINarratorApiService::class.java),
                apiKey = apiKey,
                isDebug = isDebug,
                baseUrl = baseUrl,
                modelSlug = modelSlug
            )
        }

        fun fromConnection(conn: AiConnection): Narrator {
            val url = conn.apiUrl.trim().ensureEndsWithSlash()
            if (!url.startsWith("http")) {
                Log.w("AINarrator", "Invalid baseUrl: $url — falling back to DummyNarrator")
                return DummyNarrator()
            }

            return create(
                apiKey = conn.apiToken,
                baseUrl = url,
                isDebug = true,
                modelSlug = conn.modelSlug
            ).also {
                Log.d("AINarrator", "Loaded model: ${conn.modelName} → ${conn.modelSlug}")
            }
        }
    }
}
