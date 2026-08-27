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
 *  - [GROQ], [MISTRAL_VOXTRAL] and [OPENROUTER_STT] are here because people asked for
 *    them on the launch thread — the last of those pointing out, fairly, that Mistral
 *    and OpenRouter had been wired up for translation while dictation was still
 *    Gemini-only. All three speak the same OpenAI-shaped endpoint, so they share one
 *    client. Groq runs Whisper, which is the exact model that mangles code-switching —
 *    the reason this app exists — so it is offered honestly rather than recommended.
 */
enum class TranscriptionEngine(
    val id: String,
    val label: String,
    val blurb: String,
    /** Can a tone be applied on top? Live models cannot rewrite text at all. */
    val supportsTone: Boolean,
    /** Does audio go up while you are still speaking, rather than after you stop? */
    val streams: Boolean,
    /** Who this model belongs to. The key and the base URL live there, not here. */
    val provider: Provider = Provider.GEMINI
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
        label = "Groq Whisper",
        blurb = "Fast and free to start, but it is Whisper: strong on English, weaker when you switch languages mid-sentence.",
        supportsTone = true,
        streams = false,
    ),
    GROQ_WHISPER_LARGE(
        id = "whisper-large-v3",
        label = "Whisper Large v3",
        blurb = "The full-size Whisper. Slower than turbo, a little more careful.",
        supportsTone = true,
        streams = false,
        provider = Provider.GROQ
    ),
    GROQ_DISTIL(
        id = "distil-whisper-large-v3-en",
        label = "Distil-Whisper (English only)",
        blurb = "The quickest thing here by some way, and English only. Nothing else.",
        supportsTone = true,
        streams = false,
        provider = Provider.GROQ
    ),
    MISTRAL_VOXTRAL(
        id = "voxtral-mini-latest",
        label = "Mistral Voxtral",
        blurb = "Mistral's own transcriber. Strong across European languages, and priced to be used.",
        supportsTone = true,
        streams = false,
        provider = Provider.MISTRAL,
    ),
    OPENAI_MINI_TRANSCRIBE(
        id = "gpt-4o-mini-transcribe",
        label = "GPT-4o Mini Transcribe",
        blurb = "OpenAI's cheap transcriber. Quick, and good on clear English.",
        supportsTone = true,
        streams = false,
        provider = Provider.OPENAI
    ),
    OPENAI_TRANSCRIBE(
        id = "gpt-4o-transcribe",
        label = "GPT-4o Transcribe",
        blurb = "The larger one. Better on accents and noise, and priced like it.",
        supportsTone = true,
        streams = false,
        provider = Provider.OPENAI
    ),
    OPENAI_WHISPER(
        id = "whisper-1",
        label = "Whisper",
        blurb = "OpenAI's original. Still solid, and the cheapest of their three.",
        supportsTone = true,
        streams = false,
        provider = Provider.OPENAI
    ),
    CUSTOM_STT(
        id = "",
        label = "Anything OpenAI-compatible",
        blurb = "Your own base URL and model — a self-hosted Whisper, a company gateway, anything that answers /audio/transcriptions.",
        supportsTone = true,
        streams = false,
        provider = Provider.CUSTOM
    ),
    OPENROUTER_STT(
        id = "openai/gpt-4o-mini-transcribe",
        label = "OpenRouter",
        blurb = "Name any transcriber OpenRouter carries — Voxtral, GPT-4o Transcribe, Whisper.",
        supportsTone = true,
        streams = false,
        provider = Provider.OPENROUTER,
    );

    /**
     * True for the models that answer in text rather than speech.
     *
     * The distinction is not cosmetic: a speech-to-speech model generates a spoken reply
     * for every dictation, which nobody hears and everybody pays for. A transcribe model
     * refuses AUDIO outright and returns nothing but the transcript.
     */
    val textOnly: Boolean get() = this == GEMINI_TRANSCRIBE_LIVE

    /** Everything that is not Gemini goes out over the shared OpenAI-shaped client. */
    val needsGroqKey: Boolean get() = provider.needsKey

    val baseUrl: String get() = provider.baseUrl

    val editableModel: Boolean get() = provider.editableModel

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

/**
 * The ISO code for a typed language name, or blank if we do not recognise it.
 *
 * Blank is a perfectly good answer. The name still goes to the model as context; it
 * simply does not also become a `language` form field on the providers that take one.
 */
fun languageCodeFor(name: String?): String {
    val cleaned = name?.trim().orEmpty()
    if (cleaned.isEmpty()) return ""
    return SPOKEN_LANGUAGES.firstOrNull { it.label.equals(cleaned, ignoreCase = true) }?.code
        ?: SPOKEN_LANGUAGES.firstOrNull { it.code.equals(cleaned, ignoreCase = true) }?.code
        ?: ""
}

/** The handful worth offering as one-tap shortcuts under the text field. */
val SUGGESTED_LANGUAGES: List<String> = listOf(
    "English", "isiZulu", "isiXhosa", "Afrikaans", "Sesotho",
    "Spanish", "French", "Portuguese", "German", "Arabic"
)
