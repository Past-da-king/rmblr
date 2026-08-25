package io.github.pastdaking.rmblr.data

/**
 * What actually turns your voice into words, and what each one costs you.
 *
 * There is no single best answer here, which is why this is a choice rather than a
 * constant. The trade is always the same three-way one: how fast the text lands, how
 * well it survives switching languages mid-sentence, and whether a tone can be applied
 * to it afterwards.
 *
 *  - [GEMINI_LITE] is the default because it is genuinely good at plain transcription
 *    and costs a fraction of the bigger models. Transcribing is not rocket science and
 *    there is no reason to pay Flash prices for it.
 *  - [GEMINI_LIVE] is the fast one. Audio goes up the socket while you are still
 *    talking, so the words are already there when you let go. The catch is real and is
 *    stated plainly in the UI: a Live model is speech-to-speech and cannot rewrite
 *    text, so tone actions are off.
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
    GEMINI_LITE(
        id = "gemini-3.1-flash-lite",
        label = "Gemini Flash Lite",
        blurb = "Default. Cheap, accurate, handles mixed languages. Tone actions work.",
        supportsTone = true,
        streams = false
    ),
    GEMINI_LIVE(
        id = "gemini-3.1-flash-live-preview",
        label = "Gemini Live — fast",
        blurb = "Words appear while you speak, so there is no wait when you let go. Tone actions are off in this mode.",
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

    companion object {
        val DEFAULT = GEMINI_LITE

        fun from(name: String?): TranscriptionEngine =
            values().firstOrNull { it.name == name }
                ?: values().firstOrNull { it.id == name }
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
