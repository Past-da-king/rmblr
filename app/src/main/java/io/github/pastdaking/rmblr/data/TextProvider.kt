package io.github.pastdaking.rmblr.data

/**
 * Where text work goes — translation today, anything text-shaped later.
 *
 * This is deliberately separate from [TranscriptionEngine]. When people on the launch
 * thread asked for Mistral and OpenRouter, the obvious reading was "more transcribers",
 * and that reading is wrong: those endpoints are overwhelmingly text-in, text-out, and
 * almost none of them take audio at all. What they are genuinely excellent at is
 * translation, which is what was actually being asked for.
 *
 * Everything except Gemini speaks the OpenAI chat-completions shape, so one client
 * covers Mistral, OpenRouter, Groq, OpenAI itself, a local llama.cpp server, and
 * whatever appears next. [CUSTOM] exists so a provider nobody has heard of yet does not
 * need an app update: paste a base URL and a model name and it works.
 */
enum class TextProvider(
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val blurb: String,
    /** Gemini reuses the key already in Settings; everything else brings its own. */
    val usesGeminiKey: Boolean = false,
    val editable: Boolean = false
) {
    GEMINI(
        label = "Gemini",
        baseUrl = "",
        defaultModel = "gemini-3.1-flash-lite",
        blurb = "Uses the Gemini key you already have. Best on African languages.",
        usesGeminiKey = true
    ),
    MISTRAL(
        label = "Mistral",
        baseUrl = "https://api.mistral.ai/v1",
        defaultModel = "mistral-small-latest",
        blurb = "Strong and cheap across European languages."
    ),
    OPENROUTER(
        label = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "mistralai/mistral-small",
        blurb = "One key, hundreds of models. Set the model name yourself.",
        editable = true
    ),
    GROQ_TEXT(
        label = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        blurb = "The fastest of these by a distance.",
        editable = true
    ),
    OPENAI(
        label = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        blurb = "Works if that is the key you already pay for.",
        editable = true
    ),
    CUSTOM(
        label = "Anything OpenAI-compatible",
        baseUrl = "",
        defaultModel = "",
        blurb = "Your own base URL and model. Works with a local server too.",
        editable = true
    );

    companion object {
        val DEFAULT = GEMINI

        fun from(name: String?): TextProvider =
            values().firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Languages worth translating INTO. Auto-detect is meaningless as a destination. */
val TARGET_LANGUAGES: List<SpokenLanguage> = SPOKEN_LANGUAGES.filter { it.code.isNotBlank() }
