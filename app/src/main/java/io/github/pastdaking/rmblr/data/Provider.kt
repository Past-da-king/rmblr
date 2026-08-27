package io.github.pastdaking.rmblr.data

/**
 * Who you have an account with, as opposed to which of their models you picked.
 *
 * These were the same thing for a while, and it showed. "Gemini Transcribe Live",
 * "Gemini Flash Lite" and "Gemini Live" sat in one flat list next to "Mistral" and
 * "OpenRouter", so three entries were models and three were companies, and the page had
 * no way to say that they all share one Gemini key. Worse, dictation and translation
 * each kept their own copy of every key, so pasting a Mistral key in one place did
 * nothing for the other.
 *
 * Splitting them fixes both. A key belongs to a PROVIDER and is entered once. A model
 * belongs to a provider and is chosen per job — one model does the dictation, another
 * does the translating, and they can be from different companies without anything being
 * typed twice.
 */
enum class Provider(
    val label: String,
    /** OpenAI-shaped base URL. Blank for Gemini, which has its own protocol. */
    val baseUrl: String,
    /** Gemini's key already exists in the app; the rest bring their own. */
    val needsKey: Boolean,
    val keyHint: String,
    val note: String,
    /** Providers that front many models, where the model name is yours to type. */
    val editableModel: Boolean = false
) {
    GEMINI(
        label = "Gemini",
        baseUrl = "",
        needsKey = false,
        keyHint = "AIzaSy...",
        note = "Free from Google AI Studio. The key is stored on this device and calls " +
            "Google directly. Best on African languages, and the only provider here that " +
            "streams as you speak."
    ),
    GROQ(
        label = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        needsKey = true,
        keyHint = "gsk_...",
        note = "Free from console.groq.com. Groq runs Whisper: quick and accurate in one " +
            "language, but the model that struggles when you switch languages mid-sentence.",
        editableModel = true
    ),
    MISTRAL(
        label = "Mistral",
        baseUrl = "https://api.mistral.ai/v1",
        needsKey = true,
        keyHint = "Mistral API key",
        note = "From console.mistral.ai. Voxtral is Mistral's own transcriber and it is " +
            "strong across European languages. Only the voxtral-mini models answer the " +
            "transcription endpoint — voxtral-small is chat-only.",
        editableModel = true
    ),
    OPENROUTER(
        label = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        needsKey = true,
        keyHint = "sk-or-...",
        note = "From openrouter.ai. One key reaches Voxtral, GPT-4o Transcribe, Whisper and " +
            "hundreds of text models — type whichever you want.",
        editableModel = true
    ),
    OPENAI(
        label = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        needsKey = true,
        keyHint = "sk-...",
        note = "Works if that is the key you already pay for.",
        editableModel = true
    ),
    CUSTOM(
        label = "Anything OpenAI-compatible",
        baseUrl = "",
        needsKey = true,
        keyHint = "API key",
        note = "Your own base URL and model, including a server running on your own machine. " +
            "Give the base URL up to and including /v1.",
        editableModel = true
    );

    companion object {
        fun from(name: String?): Provider = values().firstOrNull { it.name == name } ?: GEMINI
    }
}
