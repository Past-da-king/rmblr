package io.github.pastdaking.rmblr.data

/**
 * What actually turns your voice into words, and what each one costs you.
 *
 * There is no single best answer here, which is why this is a choice rather than a
 * constant. The trade is always the same three-way one: how fast the text lands, how
 * well it survives switching languages mid-sentence, and whether a tone can be applied
 * to it afterwards.
 *
 *  - [GEMINI_TRANSCRIBE_LIVE] is the default: a model built for this one job. Audio goes
 *    up the socket while you are still talking, and because it answers in text rather
 *    than speech there is no spoken reply to generate, wait for or pay for. It cannot
 *    rewrite text, so tone actions are off — which is stated plainly in the UI.
 *  - [GEMINI_LITE] is the one to pick when you want a tone. It is genuinely good at
 *    plain transcription for a fraction of the price of the bigger models, but it only
 *    starts once you stop talking.
 *  - [GEMINI_LIVE] is the previous streaming model, kept so nobody's choice disappears
 *    under them. It is speech-to-speech: it generates a spoken answer for every
 *    dictation that is thrown away unheard and billed anyway.
 *  - [GROQ] is here because two people asked for it on the launch thread. It is the
 *    fastest batch transcriber going, but it runs Whisper, which is the exact model
 *    that mangles code-switching — the reason this app exists. So it is offered
 *    honestly rather than recommended.
 */
enum class TranscriptionEngine(
    val id: String,
    val label: String,
    val blurb: String,
    /** Can a tone be applied on top? Live models cannot rewrite text at all. */
    val supportsTone: Boolean,
    /** Does audio go up while you are still speaking, rather than after you stop? */
    val streams: Boolean,
    /** Needs its own key, separate from the Gemini one. */
    val needsGroqKey: Boolean = false
) {
    GEMINI_TRANSCRIBE_LIVE(
        id = "gemini-3.5-transcribe-live",
        label = "Gemini Transcribe Live — fastest",
        blurb = "Built for exactly this job. Words appear while you speak and nothing is wasted, so it is the cheapest way to stream as well as the quickest. No tone actions.",
        supportsTone = false,
        streams = true
    ),
    GEMINI_LITE(
        id = "gemini-3.5-flash-lite",
        label = "Gemini Flash Lite",
        blurb = "Cheap, accurate, handles mixed languages, and the only streaming-free option that can apply a tone.",
        supportsTone = true,
        streams = false
    ),
    GEMINI_LIVE(
        id = "gemini-3.1-flash-live-preview",
        label = "Gemini Live — older fast mode",
        blurb = "The previous streaming model. It still works, but it generates a spoken reply that is thrown away and billed, so Transcribe Live replaces it.",
        supportsTone = false,
        streams = true
    ),
    GROQ(
        id = "whisper-large-v3-turbo",
        label = "Groq Whisper — fastest",
        blurb = "Very fast and free to start, but it is Whisper: strong on English, weaker when you switch languages mid-sentence. Needs a Groq key.",
        supportsTone = true,
        streams = false,
        needsGroqKey = true
    );

    /**
     * True for the models that answer in text rather than speech.
     *
     * The distinction is not cosmetic: a speech-to-speech model generates a spoken reply
     * for every dictation, which nobody hears and everybody pays for. A transcribe model
     * refuses AUDIO outright and returns nothing but the transcript.
     */
    val textOnly: Boolean get() = this == GEMINI_TRANSCRIBE_LIVE

    companion object {
        val DEFAULT = GEMINI_TRANSCRIBE_LIVE

        /**
         * Model ids move on. Anything stored by an older version is mapped forward here
         * rather than silently falling back to the default and losing someone's choice.
         */
        private val RENAMED = mapOf(
            "gemini-3.1-flash-lite" to GEMINI_LITE,
            "gemini-3.5-flash" to GEMINI_LITE,
            "gemini-3.1-flash-live-preview" to GEMINI_LIVE
        )

        fun from(name: String?): TranscriptionEngine =
            values().firstOrNull { it.name == name }
                ?: values().firstOrNull { it.id == name }
                ?: RENAMED[name]
                ?: DEFAULT
    }
}

/**
 * A language you can pin the transcriber to.
 *
 * Two people on the launch thread asked variations of "what languages does this
 * support?", and the honest answer — whatever Gemini can hear, which is far more than
 * the documented list — is not something a user can act on. So this list exists to be
 * acted on: leave it on Auto and nothing changes, or name your language and every
 * engine gets told about it.
 *
 * [code] is ISO 639-1 for Groq, which wants a code; [label] is what Gemini gets, which
 * reads a plain sentence better than it reads an abbreviation.
 */
data class SpokenLanguage(val code: String, val label: String)

val SPOKEN_LANGUAGES: List<SpokenLanguage> = listOf(
    SpokenLanguage("", "Auto — work it out"),
    SpokenLanguage("en", "English"),
    SpokenLanguage("zu", "isiZulu"),
    SpokenLanguage("xh", "isiXhosa"),
    SpokenLanguage("af", "Afrikaans"),
    SpokenLanguage("st", "Sesotho"),
    SpokenLanguage("tn", "Setswana"),
    SpokenLanguage("es", "Spanish"),
    SpokenLanguage("pt", "Portuguese"),
    SpokenLanguage("fr", "French"),
    SpokenLanguage("de", "German"),
    SpokenLanguage("it", "Italian"),
    SpokenLanguage("nl", "Dutch"),
    SpokenLanguage("pl", "Polish"),
    SpokenLanguage("tr", "Turkish"),
    SpokenLanguage("ar", "Arabic"),
    SpokenLanguage("hi", "Hindi"),
    SpokenLanguage("ur", "Urdu"),
    SpokenLanguage("sw", "Swahili"),
    SpokenLanguage("id", "Indonesian"),
    SpokenLanguage("ru", "Russian"),
    SpokenLanguage("uk", "Ukrainian"),
    SpokenLanguage("ja", "Japanese"),
    SpokenLanguage("ko", "Korean"),
    SpokenLanguage("zh", "Chinese")
)

fun languageFor(code: String?): SpokenLanguage =
    SPOKEN_LANGUAGES.firstOrNull { it.code == (code ?: "") } ?: SPOKEN_LANGUAGES.first()
