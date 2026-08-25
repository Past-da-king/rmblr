package io.github.pastdaking.rmblr.ai

import io.github.pastdaking.rmblr.audio.AudioRecorderManager
import io.github.pastdaking.rmblr.data.DictionaryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TranscriptionEngine
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.languageFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One place that knows how a dictation actually runs, so the orb and the home screen
 * cannot drift apart.
 *
 * Both used to inline the same twenty lines of "stop the recorder, decide a mode, call
 * Gemini, hope", and both had already grown a dead branch for a live-transcript feature
 * that no longer existed. Everything about choosing an engine, streaming to it, falling
 * back when it fails and applying a tone afterwards now lives here.
 */
class DictationController(
    private val prefs: PreferencesManager,
    private val recorder: AudioRecorderManager,
    private val dictionary: DictionaryRepository? = null
) {

    private var session: LiveTranscriptionSession? = null
    private var startedWith: TranscriptionEngine = TranscriptionEngine.DEFAULT

    private val _liveText = MutableStateFlow("")

    /** Words heard so far, updated while the user is still speaking. Streaming only. */
    val liveText: StateFlow<String> = _liveText.asStateFlow()

    /** True when the engine in use puts text on screen before you stop talking. */
    val isStreaming: Boolean get() = session != null

    fun begin() {
        val engine = prefs.getEngine()
        startedWith = engine
        _liveText.value = ""

        val apiKey = prefs.getEffectiveApiKey()
        val language = languageFor(prefs.getLanguageCode())

        // The socket has to exist before the first buffer is read, or the opening
        // syllable is the one thing that never makes it up the wire.
        session = if (engine.streams && apiKey.isNotBlank()) {
            runCatching {
                LiveTranscriptionSession(
                    apiKey = apiKey,
                    model = engine.id,
                    languageHint = language.code.takeIf { it.isNotBlank() }?.let { language.label },
                    vocabulary = dictionary?.promptHint()
                )
            }.getOrNull()
        } else {
            null
        }

        val live = session
        if (live != null) {
            recorder.startRecording { buffer, length ->
                live.feed(buffer, length)
                _liveText.value = live.text.value
            }
        } else {
            recorder.startRecording()
        }
    }

    /**
     * Stop, and return the raw transcript alongside whatever should actually be typed.
     *
     * @param instruction the tone's prompt, or null for a plain "just type what I said".
     *        A null instruction is not merely cosmetic: it means no rewriting call is
     *        made at all, which is the difference between text landing immediately and
     *        text landing a second later.
     */
    suspend fun finish(instruction: String?): Result<Pair<String, String>> {
        val engine = startedWith
        val live = session
        session = null

        val wav = recorder.stopRecording()
        val apiKey = prefs.getEffectiveApiKey()
        val groqKey = prefs.getGroqApiKey()
        val language = languageFor(prefs.getLanguageCode())

        // ---- streaming path: usually already finished before we get here ----
        if (live != null) {
            val heard = live.finish()
            val raw = heard.getOrNull()?.trim().orEmpty()
            if (raw.isNotEmpty()) {
                // A Live model cannot rewrite text, and the whole point of choosing it
                // was to avoid a second round trip. So the transcript is the answer.
                return Result.success(raw to raw)
            }
            // The socket died, the key is wrong, or there was genuinely nothing there.
            // We still hold the audio, so drop to the batch engine rather than losing a
            // dictation over a flaky connection.
            if (wav.size <= 200) {
                return Result.failure(heard.exceptionOrNull() ?: IllegalStateException("Nothing recorded."))
            }
        }

        if (wav.size <= 200) return Result.failure(IllegalStateException("Nothing recorded."))

        // ---- Groq: transcribe there, then polish on Gemini if a tone was asked for ----
        if (engine == TranscriptionEngine.GROQ && groqKey.isNotBlank()) {
            val heard = GroqApiClient.transcribe(
                wavBytes = wav,
                apiKey = groqKey,
                model = engine.id,
                languageCode = language.code,
                vocabulary = dictionary?.plainWordList()
            )
            heard.onFailure { return Result.failure(it) }
            val raw = heard.getOrNull()?.trim().orEmpty()
            if (raw.isEmpty()) return Result.failure(IllegalStateException("Nothing was said."))
            if (instruction.isNullOrBlank() || apiKey.isBlank()) return Result.success(raw to raw)
            val polished = GeminiApiClient.postProcessText(
                inputText = raw,
                apiKey = apiKey,
                preset = CleanupPreset.CUSTOM,
                customPrompt = instruction
            )
            return Result.success(raw to (polished.getOrNull()?.takeIf { it.isNotBlank() } ?: raw))
        }

        // ---- Gemini batch: one call does the transcript and the tone together ----
        val batchModel =
            if (engine.streams || engine.needsGroqKey) TranscriptionEngine.GEMINI_LITE.id else engine.id

        return GeminiApiClient.transcribeAudio(
            audioBytes = wav,
            apiKey = apiKey,
            model = batchModel,
            mode = if (instruction.isNullOrBlank()) TranscriptionMode.DIRECT_VERBATIM
                   else TranscriptionMode.POST_PROCESS_CLEANUP,
            instruction = instruction,
            languageHint = language.label.takeIf { language.code.isNotBlank() },
            vocabulary = dictionary?.promptHint()
        )
    }

    /** Throw the dictation away without transcribing anything. */
    fun cancel() {
        session?.cancel()
        session = null
        _liveText.value = ""
        recorder.cancelRecording()
    }
}
