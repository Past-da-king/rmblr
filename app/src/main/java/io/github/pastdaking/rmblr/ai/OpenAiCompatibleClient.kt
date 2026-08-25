package io.github.pastdaking.rmblr.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * One client for every provider that speaks OpenAI's chat-completions shape.
 *
 * Mistral, OpenRouter, Groq, OpenAI and a llama.cpp server on your own laptop all accept
 * the same POST with the same JSON, so there is no reason for five clients or for a new
 * app release each time someone names a provider. Base URL and model come from settings,
 * which is what makes "anything OpenAI-compatible" an honest claim rather than a list.
 */
object OpenAiCompatibleClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * @param baseUrl up to and including the version segment, e.g. https://api.mistral.ai/v1.
     *                A trailing slash is tolerated because everyone pastes one eventually.
     */
    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext Result.failure(IllegalStateException("No base URL set for this provider."))
        if (model.isBlank()) return@withContext Result.failure(IllegalStateException("No model name set for this provider."))
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalStateException("No API key set for this provider."))

        try {
            val body = JSONObject()
                .put("model", model)
                .put("temperature", 0.2)
                .put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().put("role", "system").put("content", systemPrompt))
                        .put(JSONObject().put("role", "user").put("content", userText))
                )
                .toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(errorFrom(raw, response.code)))
            }

            val text = runCatching {
                JSONObject(raw)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "")
            }.getOrDefault("").trim()

            if (text.isBlank()) Result.failure(Exception("That provider returned nothing."))
            else Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun errorFrom(raw: String, code: Int): String {
        val message = runCatching {
            val root = JSONObject(raw)
            root.optJSONObject("error")?.optString("message") ?: root.optString("message")
        }.getOrNull()
        return if (message.isNullOrBlank()) "Provider error (HTTP $code)" else "Provider error ($code): $message"
    }
}
