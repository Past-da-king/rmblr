package io.github.pastdaking.rmblr.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Every transcriber that speaks OpenAI's `/audio/transcriptions` shape.
 *
 * This started life as a Groq-only client, which turned out to be a waste: Groq, Mistral
 * and OpenRouter all accept the identical multipart POST — file, model, an optional
 * language code, an optional prompt — and all return `{"text": ...}`. So the provider is
 * a base URL and a model name, and adding the next one costs a line in
 * [io.github.pastdaking.rmblr.data.TranscriptionEngine] rather than a new client.
 *
 * That matters beyond tidiness. Someone on the launch thread pointed out that RMBLR had
 * added Mistral and OpenRouter for translation while dictation was still Gemini-only,
 * and that dictation was the part they actually wanted. They were right, and the reason
 * it had not been done was a client that only knew one host.
 */
object SpeechToTextClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * @param baseUrl up to and including the version segment, e.g. https://api.mistral.ai/v1.
     * @param languageCode ISO 639-1, or blank to let the model detect it.
     * @param vocabulary passed as `prompt`, which on every one of these providers is
     *                   plain context rather than an instruction: a word list steers
     *                   spelling without the model trying to obey it as a command.
     */
    suspend fun transcribe(
        wavBytes: ByteArray,
        baseUrl: String,
        apiKey: String,
        model: String,
        languageCode: String? = null,
        vocabulary: String? = null,
        providerName: String = "That provider"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No $providerName key set. Add one in Settings, or switch back to Gemini.")
            )
        }
        if (baseUrl.isBlank() || model.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("$providerName needs a base URL and a model name.")
            )
        }
        if (wavBytes.size <= 44) {
            return@withContext Result.failure(IllegalStateException("Nothing recorded."))
        }

        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "dictation.wav",
                    wavBytes.toRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", model)
                .addFormDataPart("response_format", "json")
                .apply {
                    if (!languageCode.isNullOrBlank()) addFormDataPart("language", languageCode)
                    if (!vocabulary.isNullOrBlank()) addFormDataPart("prompt", vocabulary)
                }
                .build()

            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(errorFrom(raw, response.code, providerName)))
            }

            val text = runCatching { JSONObject(raw).optString("text", "") }.getOrDefault("").trim()
            if (text.isBlank()) {
                Result.failure(Exception("$providerName heard nothing in that recording."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Listing models is the cheapest request every one of these hosts answers. */
    suspend fun testConnection(
        baseUrl: String,
        apiKey: String,
        providerName: String = "That provider"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("$providerName key cannot be empty."))
        try {
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Result.failure(Exception(errorFrom(raw, response.code, providerName)))
            } else {
                Result.success("Connection successful! $providerName is active.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun errorFrom(raw: String, code: Int, providerName: String): String {
        val message = runCatching {
            val root = JSONObject(raw)
            root.optJSONObject("error")?.optString("message") ?: root.optString("message")
        }.getOrNull()
        return if (message.isNullOrBlank()) "$providerName error (HTTP $code)"
        else "$providerName error ($code): $message"
    }
}
