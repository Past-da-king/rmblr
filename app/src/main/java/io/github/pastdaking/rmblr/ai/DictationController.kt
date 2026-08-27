package io.github.pastdaking.rmblr.ai

import io.github.pastdaking.rmblr.audio.AudioRecorderManager
import io.github.pastdaking.rmblr.data.DictionaryRepository
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.Provider
import io.github.pastdaking.rmblr.data.TranscriptionEngine
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.data.CleanupPreset
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
    private val dictionary: DictionaryRepository? = null,
    private val history: HistoryRepository? = null
) {

    private var session: LiveTranscriptionSession? = null
    private var startedWith: TranscriptionEngine = TranscriptionEngine.DEFAULT

    private val _liveText = MutableStateFlow("")

    /** Words heard so far, updated while the user is still speaking. Streaming only. */
    val liveText: StateFlow<String> = _liveText.asStateFlow()

    /** True when the engine in use puts text on screen before you stop talking. */
    val isStreaming: Boolean get() = session != null

    /**
     * @param translating true when the orb is in translate mode.
     *        A streaming session is deliberately NOT opened in that case: the transcribe
     *        models return the words as spoken and cannot be asked for anything else, so
     *        translating through them would mean a second call. Gemini can take the audio
     *        and hand back the translation in one, which is worth losing the streaming for.
     */
    fun begin(translating: Boolean = false) {
        val engine = prefs.getEngine()
        startedWith = engine
        _liveText.value = ""

        val apiKey = prefs.getEffectiveApiKey()
        val languageName = prefs.getSpokenLanguage()

        // The socket has to exist before the first buffer is read, or the opening
        // syllable is the one thing that never makes it up the wire.
        session = if (engine.streams && apiKey.isNotBlank() && !translating) {
            runCatching {
                LiveTranscriptionSession(
                    apiKey = apiKey,
                    model = engine.id,
                    languageHint = languageName.takeIf { it.isNotBlank() },
                    vocabulary = dictionary?.promptHint(),
                    textOnly = engine.textOnly
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
    /**
     * @param translateTo a language to end up in, set when the orb is in translate mode.
     *        On Gemini this becomes ONE call — the audio goes up with an instruction to
     *        translate rather than transcribe. Every other provider transcribes first and
     *        translates second, because their speech endpoints only ever return the words
     *        that were said. That is slower, and it is the honest cost of using them.
     */
    suspend fun finish(instruction: String?, translateTo: String? = null): Result<Pair<String, String>> {
        val engine = startedWith
        val live = session
        session = null

        val wav = recorder.stopRecording()
        val apiKey = prefs.getEffectiveApiKey()
        val groqKey = prefs.getEngineKey(engine)
        val languageName = prefs.getSpokenLanguage()
        val languageCode = prefs.getLanguageCode()

        // ---- streaming path: usually already finished before we get here ----
        if (live != null) {
            val heard = live.finish()
            val raw = heard.getOrNull()?.trim().orEmpty()
            if (raw.isNotEmpty()) {
                // A Live model cannot rewrite text, and the whole point of choosing it
                // was to avoid a second round trip. So the transcript is the answer.
                return Result.success(finished(raw, raw, translateTo))
            }
            // The socket died, the key is wrong, or there was genuinely nothing there.
            // We still hold the audio, so drop to the batch engine rather than losing a
            // dictation over a flaky connection.
            if (wav.size <= 200) {
                return Result.failure(heard.exceptionOrNull() ?: IllegalStateException("Nothing recorded."))
            }
        }

        if (wav.size <= 200) return Result.failure(IllegalStateException("Nothing recorded."))

        // ---- Groq / Mistral / OpenRouter: one endpoint shape, three hosts ----
        //
        // Transcribe there, then polish on Gemini if a tone was asked for. None of these
        // providers rewrites text as part of transcribing, so the tone is a second call
        // and only happens when one was actually requested.
        if (engine.needsGroqKey && groqKey.isNotBlank()) {
            val heard = SpeechToTextClient.transcribe(
                wavBytes = wav,
                baseUrl = prefs.getProviderBaseUrl(engine.provider),
                apiKey = groqKey,
                model = prefs.getEngineModel(engine),
                languageCode = languageCode,
                vocabulary = dictionary?.plainWordList(),
                providerName = engine.label
            )
            heard.onFailure { return Result.failure(it) }
            val raw = heard.getOrNull()?.trim().orEmpty()
            if (raw.isEmpty()) return Result.failure(IllegalStateException("Nothing was said."))
            if (instruction.isNullOrBlank() || apiKey.isBlank()) return Result.success(finished(raw, raw, translateTo))
            val polished = GeminiApiClient.postProcessText(
                inputText = raw,
                apiKey = apiKey,
                preset = CleanupPreset.CUSTOM,
                customPrompt = instruction
            )
            return Result.success(finished(raw, polished.getOrNull()?.takeIf { it.isNotBlank() } ?: raw, translateTo))
        }

        // ---- Gemini batch: one call does the transcript and the tone together ----
        val batchModel =
            if (engine.streams || engine.needsGroqKey) TranscriptionEngine.GEMINI_LITE.id else engine.id

        // Translating on Gemini is the same request with a different instruction, so the
        // audio is only uploaded once and no second model is involved at all.
        val effectiveInstruction = if (!translateTo.isNullOrBlank()) {
            translationInstruction(translateTo, languageName)
        } else {
            instruction
        }

        return GeminiApiClient.transcribeAudio(
            audioBytes = wav,
            apiKey = apiKey,
            model = batchModel,
            mode = if (effectiveInstruction.isNullOrBlank()) TranscriptionMode.DIRECT_VERBATIM
                   else TranscriptionMode.POST_PROCESS_CLEANUP,
            instruction = effectiveInstruction,
            languageHint = languageName.takeIf { it.isNotBlank() },
            vocabulary = dictionary?.promptHint()
        ).mapCatching { (raw, cleaned) -> finished(raw, cleaned, translateTo) }
    }

    /**
     * Put a capital at the start of each sentence, if the setting asks for it.
     *
     * The switch for this has been sitting in Settings since before I touched the app,
     * wired to precisely nothing — you could toggle it all day and no transcript ever
     * changed. Verbatim mode is where it earns its place: a model asked for exactly what
     * was said will happily return a lower-case opening word.
     */
    /**
     * Say the name of a snippet and get the snippet.
     *
     * Snippets shipped with a `shortcut` field that was only ever printed on screen —
     * you could define "/meet" and nothing on earth would ever expand it. Speaking is
     * the natural trigger for a dictation app: say "meet", or "slash meet", or the
     * snippet's name, and the saved text goes in instead of those words.
     */
    private fun expandSnippet(text: String): String? {
        val snippets = history?.snippetsFlow?.value ?: return null
        if (snippets.isEmpty()) return null
        val spoken = text.trim().trimEnd('.', '!', '?', ',').lowercase()
        if (spoken.isEmpty() || spoken.length > 40) return null
        val match = snippets.firstOrNull { snippet ->
            val shortcut = snippet.shortcut.trim().lowercase()
            val bare = shortcut.removePrefix("/")
            (shortcut.isNotEmpty() && (spoken == shortcut || spoken == bare || spoken == "slash $bare")) ||
                snippet.title.trim().lowercase() == spoken
        }
        return match?.content
    }

    /**
     * Write the dictation in another language, if one was asked for.
     *
     * Speak English, type Japanese. This runs on the transcript rather than the audio,
     * so it works on every engine including the streaming ones that cannot rewrite text
     * themselves — the translating is a separate call to whichever provider does the
     * translating, and it only happens when a language is actually set.
     */
    private suspend fun translatedIfAsked(text: String): String {
        val into = prefs.getDictationLanguage()
        if (into.isBlank() || text.isBlank()) return text
        return Translator.translate(text, prefs, into).getOrNull()?.takeIf { it.isNotBlank() } ?: text
    }

    /**
     * The instruction that turns a transcription request into a translation request.
     *
     * Naming the spoken language when we know it matters more than it looks: told only
     * "translate this", a model handed speech that is already partly in the target
     * language will sometimes hand it straight back.
     */
    private fun translationInstruction(into: String, spoken: String): String {
        val from = if (spoken.isBlank()) "" else "The speaker is talking in $spoken. "
        return from + "Do not transcribe what was said. TRANSLATE it into $into and output " +
            "only the translation — no transcript, no original, no explanation, no quotes. " +
            "Translate faithfully, keeping the speaker's tone and meaning, and keep names, " +
            "numbers and formatting as they are."
    }

    /**
     * Everything a transcript goes through between the model and the text field.
     *
     * @param translateTo already handled upstream on Gemini, where the model did the
     *        translating itself; here it only matters for the providers whose speech
     *        endpoints return the words as spoken and nothing else.
     */
    private suspend fun finished(
        raw: String,
        cleaned: String,
        translateTo: String? = null
    ): Pair<String, String> {
        expandSnippet(cleaned)?.let { return raw to it }
        val translated = if (!translateTo.isNullOrBlank() && startedWith.provider != Provider.GEMINI) {
            Translator.translate(cleaned, prefs, translateTo).getOrNull()?.takeIf { it.isNotBlank() } ?: cleaned
        } else {
            cleaned
        }
        return raw to capitalised(translatedIfAsked(translated))
    }

    private fun capitalised(text: String): String {
        if (!prefs.isAutoCapitalizeEnabled()) return text
        val out = StringBuilder(text)
        var startOfSentence = true
        for (i in out.indices) {
            val c = out[i]
            when {
                startOfSentence && c.isLetter() -> {
                    out[i] = c.uppercaseChar()
                    startOfSentence = false
                }
                c in SENTENCE_END -> startOfSentence = true
                // Whitespace and quotes after a full stop keep the flag alive; anything
                // else means we are mid-sentence again.
                !c.isWhitespace() && c !in CARRY_THROUGH -> startOfSentence = false
            }
        }
        return out.toString()
    }

    /** Throw the dictation away without transcribing anything. */
    fun cancel() {
        session?.cancel()
        session = null
        _liveText.value = ""
        recorder.cancelRecording()
    }

    private companion object {
        val SENTENCE_END = charArrayOf('.', '!', '?', '\n')
        val CARRY_THROUGH = charArrayOf('"', '\'', '(', '[', '“', '‘')
    }
}
