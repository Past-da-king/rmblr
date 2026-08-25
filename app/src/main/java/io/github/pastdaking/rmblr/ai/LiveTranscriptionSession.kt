package io.github.pastdaking.rmblr.ai

import android.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * A Gemini Live socket that is open for the whole time you are talking.
 *
 * This is the difference between the app feeling instant and feeling broken, so it is
 * worth being explicit about what changed. The old path recorded the entire clip, and
 * only once you let go did it dial the socket, upload every byte, and then sit waiting
 * for the model to finish speaking a reply nobody would ever hear. All of that latency
 * landed AFTER you stopped talking, which is the one moment you are watching for text.
 *
 * Here the socket opens when you press, audio goes up in 200ms chunks as the microphone
 * produces them, and `inputTranscription` deltas come back while you are still mid
 * sentence. By the time you let go there is usually nothing left to do but read the
 * buffer.
 *
 * Two details that matter:
 *  - `responseModalities` must be AUDIO. These are speech-to-speech models and they
 *    reject TEXT outright; the transcript never comes from the model's reply. It comes
 *    from `inputAudioTranscription`, which transcribes what YOU said. So we ask for
 *    audio out, ignore every byte of it, and read the input transcript.
 *  - We deliberately do NOT wait for `turnComplete`. That flag means the model has
 *    finished generating the spoken answer we are throwing away, which can take
 *    seconds. Once the input transcript has gone quiet for [QUIET_MS] after the audio
 *    ended, everything we asked for has arrived and the socket can go.
 */
class LiveTranscriptionSession(
    apiKey: String,
    model: String,
    languageHint: String?
) {

    private val transcript = StringBuilder()
    private val pending = ArrayList<String>()
    private val carry = ByteArrayOutputStream()

    private val _text = MutableStateFlow("")

    /** What has been heard so far. Safe to show while the user is still speaking. */
    val text: StateFlow<String> = _text.asStateFlow()

    private val settled = CompletableDeferred<Result<String>>()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    @Volatile private var ready = false
    @Volatile private var ended = false
    @Volatile private var turnDone = false
    @Volatile private var lastDeltaAt = 0L
    @Volatile private var failure: Throwable? = null

    private val socket: WebSocket

    init {
        val request = Request.Builder().url("$WS_URL?key=$apiKey").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(setupFrame(model, languageHint).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) = handle(webSocket, text)

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) =
                handle(webSocket, bytes.utf8())

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!ready && transcript.isEmpty()) {
                    failure = IllegalStateException(reason.ifBlank { "Live transcription closed early." })
                }
                turnDone = true
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                failure = t
                turnDone = true
            }

            private fun handle(webSocket: WebSocket, raw: String) {
                val json = runCatching { JSONObject(raw) }.getOrNull() ?: return

                if (json.has("setupComplete")) {
                    synchronized(pending) {
                        ready = true
                        pending.forEach { webSocket.send(it) }
                        pending.clear()
                    }
                    return
                }

                val server = json.optJSONObject("serverContent") ?: return
                server.optJSONObject("inputTranscription")?.optString("text")?.let { delta ->
                    if (delta.isNotEmpty()) {
                        synchronized(transcript) { transcript.append(delta) }
                        lastDeltaAt = System.currentTimeMillis()
                        _text.value = synchronized(transcript) { transcript.toString() }
                    }
                }

                if (server.optBoolean("turnComplete") || server.optBoolean("generationComplete")) {
                    turnDone = true
                }
            }
        })
    }

    /**
     * Hand over microphone bytes as they are read. Anything spoken before the socket
     * finished its handshake is held and sent the moment it does, so pressing and
     * talking immediately loses nothing.
     */
    fun feed(pcm: ByteArray, length: Int) {
        if (ended || length <= 0) return
        val frames = synchronized(carry) {
            carry.write(pcm, 0, length)
            if (carry.size() < CHUNK_BYTES) return@synchronized emptyList<String>()
            val all = carry.toByteArray()
            carry.reset()
            var offset = 0
            val out = ArrayList<String>()
            while (offset + CHUNK_BYTES <= all.size) {
                out.add(audioFrame(all.copyOfRange(offset, offset + CHUNK_BYTES)))
                offset += CHUNK_BYTES
            }
            if (offset < all.size) carry.write(all, offset, all.size - offset)
            out
        }
        frames.forEach { send(it) }
    }

    /**
     * Stop the audio and return what was heard.
     *
     * By this point the transcript is nearly always already complete, so this usually
     * returns within a few hundred milliseconds rather than the several seconds the
     * old batch path cost.
     */
    suspend fun finish(): Result<String> {
        if (ended) return settled.await()
        ended = true

        val tail = synchronized(carry) {
            val rest = carry.toByteArray()
            carry.reset()
            rest
        }
        if (tail.isNotEmpty()) send(audioFrame(tail))
        send(JSONObject().put("realtimeInput", JSONObject().put("audioStreamEnd", true)).toString())

        scope.launch {
            val startedWaiting = System.currentTimeMillis()
            while (true) {
                val heard = synchronized(transcript) { transcript.toString().trim() }
                val quietFor = System.currentTimeMillis() - lastDeltaAt
                val waited = System.currentTimeMillis() - startedWaiting

                val done = turnDone ||
                    (heard.isNotEmpty() && lastDeltaAt > 0L && quietFor > QUIET_MS) ||
                    waited > MAX_WAIT_MS

                if (done) {
                    settle(
                        when {
                            heard.isNotEmpty() -> Result.success(heard)
                            failure != null -> Result.failure(failure!!)
                            else -> Result.failure(IllegalStateException("Nothing was said."))
                        }
                    )
                    return@launch
                }
                delay(POLL_MS)
            }
        }

        return settled.await()
    }

    /** Give up on this dictation and let go of the socket. */
    fun cancel() {
        ended = true
        settle(Result.failure(IllegalStateException("Cancelled.")))
    }

    private fun settle(result: Result<String>) {
        if (settled.isCompleted) return
        settled.complete(result)
        runCatching { socket.close(1000, null) }
        runCatching { socket.cancel() }
        runCatching { scope.cancel() }
    }

    private fun send(frame: String) {
        if (ready) {
            socket.send(frame)
        } else {
            synchronized(pending) {
                // If the handshake is somehow never coming, do not grow without bound.
                if (pending.size < MAX_PENDING_FRAMES) pending.add(frame)
            }
        }
    }

    private fun audioFrame(slice: ByteArray): String = JSONObject().put(
        "realtimeInput",
        JSONObject().put(
            "audio",
            JSONObject()
                .put("mimeType", "audio/pcm;rate=16000")
                .put("data", Base64.encodeToString(slice, Base64.NO_WRAP))
        )
    ).toString()

    private fun setupFrame(model: String, languageHint: String?): JSONObject {
        val instruction = buildString {
            append("You are a transcription service. Transcribe the user's speech exactly. ")
            append("Never reply, never answer, never add commentary. ")
            if (!languageHint.isNullOrBlank()) {
                append("The speaker is likely using $languageHint, possibly mixed with English in the same sentence. ")
            }
        }

        return JSONObject().put(
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
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction)))
                )
        )
    }

    companion object {
        private const val WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

        /** 200ms of 16kHz mono PCM16. Smaller chunks just add framing overhead. */
        private const val CHUNK_BYTES = 16000 * 2 / 5

        /**
         * How long the input transcript has to stay silent after the audio ended before
         * we call it finished. Long enough that a pause between the last two words does
         * not truncate anything, short enough to be invisible.
         */
        private const val QUIET_MS = 600L

        /** Ceiling on the tail wait only — not on the recording, which has no limit. */
        private const val MAX_WAIT_MS = 8_000L

        private const val POLL_MS = 60L

        /** ~40 seconds of held audio if the handshake never lands. */
        private const val MAX_PENDING_FRAMES = 200

        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)   // the socket is meant to stay open
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        fun isLiveModel(model: String): Boolean = model.contains("live", ignoreCase = true)
    }
}
