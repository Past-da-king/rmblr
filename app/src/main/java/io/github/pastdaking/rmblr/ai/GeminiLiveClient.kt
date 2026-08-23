package io.github.pastdaking.rmblr.ai

import android.util.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Transcription through Gemini's Live API.
 *
 * The Live models are the ones that actually handle isiZulu and code-switching well,
 * but they do not answer the REST `generateContent` endpoint at all: they only speak
 * `bidiGenerateContent` over a WebSocket. Pointing the normal client at them returns a
 * flat 404, which is why they looked broken rather than missing.
 *
 * Two things about the protocol that are not obvious and cost a while to find:
 *  - `responseModalities` must be AUDIO. These are speech-to-speech models and they
 *    reject TEXT outright; the transcript does not come from the model's reply at all.
 *  - It comes from `inputAudioTranscription`, which transcribes what YOU said. So we
 *    ask for audio out, ignore every byte of it, and read the input transcript.
 *
 * Verified end to end against gemini-3.1-flash-live-preview on 2026-08-23.
 */
object GeminiLiveClient {

    private const val WS_URL =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    /** 200ms of 16kHz mono PCM16. Smaller chunks just add framing overhead. */
    private const val CHUNK_BYTES = 16000 * 2 / 5

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)   // the socket is meant to stay open
        .writeTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun isLiveModel(model: String): Boolean = model.contains("live", ignoreCase = true)

    /**
     * @param wavBytes a WAV produced by AudioRecorderManager: 16kHz mono PCM16 behind a
     *                 44 byte header, which is exactly the format the socket wants once
     *                 the header is off.
     */
    suspend fun transcribe(
        wavBytes: ByteArray,
        apiKey: String,
        model: String,
        languageHint: String? = null
    ): Result<String> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("No Gemini API key set."))
        if (wavBytes.size <= 44) return Result.failure(IllegalStateException("Nothing recorded."))

        val pcm = wavBytes.copyOfRange(44, wavBytes.size)

        return suspendCancellableCoroutine { cont ->
            val transcript = StringBuilder()
            var settled = false

            fun settle(result: Result<String>) {
                if (!settled) {
                    settled = true
                    if (cont.isActive) cont.resume(result)
                }
            }

            val request = Request.Builder().url("$WS_URL?key=$apiKey").build()

            val socket = client.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val instruction = buildString {
                        append("You are a transcription service. Transcribe the user's speech exactly. ")
                        append("Never reply, never answer, never add commentary. ")
                        if (!languageHint.isNullOrBlank()) {
                            append("The speaker is likely using $languageHint. ")
                        }
                    }

                    val setup = JSONObject().put(
                        "setup",
                        JSONObject()
                            .put("model", "models/$model")
                            .put(
                                "generationConfig",
                                JSONObject().put("responseModalities", JSONArray().put("AUDIO"))
                            )
                            .put("inputAudioTranscription", JSONObject())
                            .put(
                                "systemInstruction",
                                JSONObject().put(
                                    "parts",
                                    JSONArray().put(JSONObject().put("text", instruction))
                                )
                            )
                    )
                    webSocket.send(setup.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handle(webSocket, text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                    handle(webSocket, bytes.utf8())
                }

                private fun handle(webSocket: WebSocket, raw: String) {
                    val json = runCatching { JSONObject(raw) }.getOrNull() ?: return

                    if (json.has("setupComplete")) {
                        var offset = 0
                        while (offset < pcm.size) {
                            val end = minOf(offset + CHUNK_BYTES, pcm.size)
                            val slice = pcm.copyOfRange(offset, end)
                            val payload = JSONObject().put(
                                "realtimeInput",
                                JSONObject().put(
                                    "audio",
                                    JSONObject()
                                        .put("mimeType", "audio/pcm;rate=16000")
                                        .put("data", Base64.encodeToString(slice, Base64.NO_WRAP))
                                )
                            )
                            webSocket.send(payload.toString())
                            offset = end
                        }
                        webSocket.send(
                            JSONObject().put(
                                "realtimeInput",
                                JSONObject().put("audioStreamEnd", true)
                            ).toString()
                        )
                        return
                    }

                    val server = json.optJSONObject("serverContent") ?: return
                    server.optJSONObject("inputTranscription")?.optString("text")?.let {
                        if (it.isNotEmpty()) transcript.append(it)
                    }

                    // The turn is over once the model has finished answering the audio it
                    // will never actually be asked to answer.
                    if (server.optBoolean("turnComplete") || server.optBoolean("generationComplete")) {
                        val out = transcript.toString().trim()
                        if (out.isNotEmpty()) {
                            settle(Result.success(out))
                            webSocket.close(1000, null)
                        }
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    val out = transcript.toString().trim()
                    if (out.isNotEmpty()) {
                        settle(Result.success(out))
                    } else {
                        settle(Result.failure(IllegalStateException(
                            if (reason.isNotBlank()) reason else "Live transcription returned nothing."
                        )))
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val out = transcript.toString().trim()
                    if (out.isNotEmpty()) settle(Result.success(out))
                    else settle(Result.failure(t))
                }
            })

            cont.invokeOnCancellation { runCatching { socket.cancel() } }

            // Belt and braces: if the server goes quiet, take whatever we heard.
            Thread {
                Thread.sleep(45_000)
                val out = transcript.toString().trim()
                if (out.isNotEmpty()) settle(Result.success(out))
                else settle(Result.failure(IllegalStateException("Live transcription timed out.")))
                runCatching { socket.close(1000, null) }
            }.apply { isDaemon = true }.start()
        }
    }
}
