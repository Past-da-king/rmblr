package io.github.pastdaking.rmblr.ai

import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TextProvider

/**
 * Copy something, tap the orb, read it in your language.
 *
 * The prompt is doing more work than it looks. A translation model handed a stray
 * sentence loves to explain itself — "Sure! Here is the translation:" — or to answer a
 * question it has been handed instead of translating it. Both are useless in a bubble
 * that is meant to be readable at a glance, so both are ruled out explicitly.
 */
object Translator {

    private fun systemPrompt(target: String) =
        "You are a translation engine. Translate everything the user sends into $target. " +
            "Output ONLY the translation. Never explain, never comment, never apologise, never " +
            "add quotes or a preamble. If the text is a question, translate the question rather " +
            "than answering it. If the text is already in $target, return it unchanged. " +
            "Keep names, numbers, code, URLs and formatting exactly as they are, and preserve " +
            "the line breaks of the original."

    suspend fun translate(text: String, prefs: PreferencesManager): Result<String> {
        val source = text.trim()
        if (source.isEmpty()) {
            return Result.failure(IllegalStateException("Nothing to translate. Copy some text first."))
        }

        val target = prefs.getTranslateTarget()
        val provider = prefs.getTextProvider()

        if (provider == TextProvider.GEMINI) {
            return GeminiApiClient.postProcessText(
                inputText = source,
                apiKey = prefs.getEffectiveApiKey(),
                preset = CleanupPreset.CUSTOM,
                customPrompt = systemPrompt(target)
            ).mapCatching { it.ifBlank { throw IllegalStateException("Gemini returned nothing.") } }
        }

        return OpenAiCompatibleClient.complete(
            baseUrl = prefs.getTextBaseUrl(),
            apiKey = prefs.getTextApiKey(),
            model = prefs.getTextModel(),
            systemPrompt = systemPrompt(target),
            userText = source
        )
    }
}
