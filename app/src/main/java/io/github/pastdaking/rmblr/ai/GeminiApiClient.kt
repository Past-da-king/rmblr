package io.github.pastdaking.rmblr.ai

import android.util.Base64
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.TranscriptionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Silence in, silence out.
     *
     * Without this the model treats an empty recording as a puzzle to solve and invents a
     * plausible sentence, or apologises for not hearing anything. Both end up pasted into
     * whatever the operator was typing in, which is worse than doing nothing.
     */
    private const val SILENCE_RULE =
        "If the audio contains no speech, or only silence, breathing or background noise, " +
            "respond with absolutely nothing: an empty response. Never explain, never apologise, " +
            "never say you could not hear anything."

    /**
     * Instructions the speaker says out loud, to you, in the middle of dictating.
     *
     * This is the single most annoying thing the app got wrong. Mid-sentence you turn
     * and address the transcriber — "hey this is for you, spell it like this" — and it
     * dutifully types out the aside word for word, which is precisely the opposite of
     * what you asked for. Those sentences are commands, not content.
     */
    private const val ASIDE_RULE =
        "The speaker sometimes stops dictating and speaks TO you instead: \"hey, this one is for you\", " +
            "\"spell that D-E-B-U-G\", \"scratch that last bit\", \"actually make this a bullet list\", " +
            "\"no wait, say it like this instead\", \"new paragraph\". Those sentences are INSTRUCTIONS " +
            "addressed to you, not words to be typed. Carry each one out on the surrounding text, then " +
            "remove it completely from your output. Never transcribe an instruction that was aimed at you. " +
            "Where a later instruction contradicts something said earlier, the later one wins. " +
            "Everything the speaker was actually dictating must survive untouched."

    /** Live models cannot rewrite text, so the cleanup pass always runs on this one. */
    private const val POLISH_MODEL = "gemini-3.1-flash-lite"

    /**
     * If the chosen model is out of quota or having a moment, drop down the list rather
     * than losing the dictation.
     *
     * Flash Lite leads because it is genuinely good at plain transcription for a
     * fraction of the price, and transcription is not the part that needs a big model.
     * Flash sits behind it for the days Lite is rate limited. Live models are absent on
     * purpose: they have no REST endpoint at all and are handled by
     * [LiveTranscriptionSession] over a socket instead.
     */
    private val FALLBACKS = listOf(
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash"
    )

    /** The chosen model, then anything else worth trying, without repeats. */
    private fun chainFor(model: String): List<String> =
        (listOf(model) + FALLBACKS)
            .filterNot { LiveTranscriptionSession.isLiveModel(it) }
            .distinct()
            .ifEmpty { FALLBACKS }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/wav",
        apiKey: String,
        model: String = POLISH_MODEL,
        mode: TranscriptionMode = TranscriptionMode.POST_PROCESS_CLEANUP,
        preset: CleanupPreset = CleanupPreset.SMART_CLEAN,
        customPrompt: String? = null,
        instruction: String? = null,
        languageHint: String? = null,
        vocabulary: String? = null
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key is missing. Please add your API key in the iLight Settings.")
            )
        }

        // Walk the fallback chain: the first model that actually answers wins.
        var lastError: Throwable? = null
        for (candidate in chainFor(model)) {
            val attempt = attemptTranscribe(audioBytes, mimeType, apiKey, candidate, mode, preset, customPrompt, instruction, languageHint, vocabulary)
            attempt.onSuccess { return@withContext Result.success(it) }
            attempt.onFailure { lastError = it }
        }
        return@withContext Result.failure(
            lastError ?: IllegalStateException("Transcription failed on every model.")
        )
    }

    private suspend fun attemptTranscribe(
        audioBytes: ByteArray,
        mimeType: String,
        apiKey: String,
        model: String,
        mode: TranscriptionMode,
        preset: CleanupPreset,
        customPrompt: String?,
        instruction: String? = null,
        languageHint: String? = null,
        vocabulary: String? = null
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {

        try {
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // A named language is a hint, never a constraint: the speaker may well drop
            // into English for half a sentence, and that has to keep working.
            val languageRule = if (languageHint.isNullOrBlank()) "" else
                "The speaker is likely using $languageHint, possibly mixed with English in the same sentence. " +
                    "Transcribe each word in whatever language it was actually spoken in. "

            // Step 1: Direct Verbatim transcription instruction
            val transcriptionPrompt = if (mode == TranscriptionMode.DIRECT_VERBATIM) {
                "Transcribe this audio file verbatim with accurate punctuation, numbers, and capitalization. " +
                    "Do not summarize or add conversational filler comments. Output ONLY the exact transcribed text. " +
                    languageRule + vocabulary.orEmpty() + SILENCE_RULE
            } else {
                // A tone is only ever a system prompt, so a custom one slots in here
                // exactly where a built-in preset would.
                val postInstruction = instruction
                    ?: customPrompt.takeIf { preset == CleanupPreset.CUSTOM && !it.isNullOrBlank() }
                    ?: preset.systemPrompt
                "You are an expert speech recognition and language polish agent. Transcribe this audio recording into clean, fluid, well-punctuated text. " +
                    "$postInstruction Output ONLY the resulting text. " +
                    languageRule + vocabulary.orEmpty() + ASIDE_RULE + " " + SILENCE_RULE
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()

                // Text instruction part
                parts.put(JSONObject().apply {
                    put("text", transcriptionPrompt)
                })

                // Audio inlineData part
                parts.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", audioBase64)
                    })
                })

                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)

                // Optional system instruction
                put("systemInstruction", JSONObject().apply {
                    val sysParts = JSONArray()
                    sysParts.put(JSONObject().apply {
                        put("text", "You are the iLight AI Keyboard voice transcriber. Output ONLY the transcript without markdown wrappers, explanations, quotes, or introductory text.")
                    })
                    put("parts", sysParts)
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.95)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val text = parseGeneratedText(responseBody)
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("No speech detected in audio."))
            }

            Result.success(Pair(text, text))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postProcessText(
        inputText: String,
        apiKey: String,
        model: String = POLISH_MODEL,
        preset: CleanupPreset = CleanupPreset.SMART_CLEAN,
        customPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key is missing. Please add your key in Settings.")
            )
        }
        if (inputText.isBlank()) {
            return@withContext Result.success("")
        }

        // A live model has no REST endpoint, so anything pointed here gets quietly
        // redirected to the model that can actually rewrite text.
        val polishModel = if (LiveTranscriptionSession.isLiveModel(model)) POLISH_MODEL else model

        try {
            val instructions = if (preset == CleanupPreset.CUSTOM && !customPrompt.isNullOrBlank()) {
                customPrompt
            } else {
                preset.systemPrompt
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()

                val prompt = """
                    Input text:
                    \"\"\"
                    $inputText
                    \"\"\"
                    
                    Instruction:
                    $instructions

                    $ASIDE_RULE

                    Important: Output ONLY the polished resulting text. No quotes, no markdown backticks, no explanations.
                """.trimIndent()

                parts.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val url = "$BASE_URL/$polishModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val cleaned = parseGeneratedText(responseBody)
            Result.success(cleaned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(apiKey: String, requestedModel: String = "gemini-3.5-flash"): Result<String> = withContext(Dispatchers.IO) {
        // A Live model has no REST endpoint to ping, so test the key against the text
        // model instead: the key is what we are actually checking.
        val model = if (LiveTranscriptionSession.isLiveModel(requestedModel)) POLISH_MODEL else requestedModel
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty."))
        }

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()
                parts.put(JSONObject().apply {
                    put("text", "Respond with 'API_CONNECTED_OK' if you receive this.")
                })
                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return@withContext Result.failure(Exception(errorMsg))
            }

            Result.success("Connection successful! Gemini model ($model) is active.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGeneratedText(jsonStr: String): String {
        try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = java.lang.StringBuilder()
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                val text = p.optString("text", "")
                sb.append(text)
            }
            return sb.toString().trim()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun parseErrorMessage(jsonStr: String, code: Int): String {
        try {
            val root = JSONObject(jsonStr)
            val error = root.optJSONObject("error")
            if (error != null) {
                val message = error.optString("message", "HTTP $code")
                return "Gemini API Error ($code): $message"
            }
        } catch (e: Exception) {
            // Ignore
        }
        return "Gemini API Error (HTTP $code)"
    }
}
