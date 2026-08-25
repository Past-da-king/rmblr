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
 * Groq's Whisper endpoint, added because two people asked for it on the launch thread.
 *
 * It is worth being honest about what this is. Groq is the fastest batch transcriber
 * available and the free tier is generous, so for English dictation it is excellent.
 * But it runs Whisper, and Whisper mangling code-switching is the entire reason this
 * app exists — so this is offered as a choice, never as the default, and the UI says
 * as much rather than letting someone find out the hard way.
 *
 * The API is OpenAI-shaped: multipart upload, `text` back in JSON.
 */
object GroqApiClient {

    private const val TRANSCRIBE_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val MODELS_URL = "https://api.groq.com/openai/v1/models"

    /** Turbo is the one worth using: same family, several times quicker. */
    const val DEFAULT_MODEL = "whisper-large-v3-turbo"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * @param languageCode ISO 639-1, or blank to let Whisper detect it. Groq wants the
     *                     code rather than the name, which is why [io.github.pastdaking.rmblr.data.SpokenLanguage]
     *                     carries both.
     */
    suspend fun transcribe(
        wavBytes: ByteArray,
        apiKey: String,
        model: String = DEFAULT_MODEL,
        languageCode: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No Groq key set. Add one in Settings, or switch back to Gemini.")
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
                }
                .build()

            val request = Request.Builder()
                .url(TRANSCRIBE_URL)
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(errorFrom(raw, response.code)))
            }

            val text = runCatching { JSONObject(raw).optString("text", "") }.getOrDefault("").trim()
            if (text.isBlank()) {
                Result.failure(Exception("Groq heard nothing in that recording."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Groq key cannot be empty."))
        try {
            val request = Request.Builder()
                .url(MODELS_URL)
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Result.failure(Exception(errorFrom(raw, response.code)))
            } else {
                Result.success("Connection successful! Groq is active.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun errorFrom(raw: String, code: Int): String {
        val message = runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return if (message.isNullOrBlank()) "Groq API Error (HTTP $code)" else "Groq API Error ($code): $message"
    }
}
