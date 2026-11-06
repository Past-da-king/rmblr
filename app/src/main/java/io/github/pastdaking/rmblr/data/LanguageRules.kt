package io.github.pastdaking.rmblr.data

/**
 * What the Languages card in Settings stands for, kept apart from the screen so it can be
 * tested without one.
 *
 * The stored value has always been a single name that is handed to the model as a hint,
 * blank meaning "work it out". Two things were missing from that. There was no way to say
 * "detect it, but I also speak French and Spanish", and no way to keep a language around
 * while auto-detect was switched on. This adds both without changing what is stored for
 * anyone who has already set a language: a name that was saved before this existed reads
 * back as the single language with auto-detect off, and a blank one as auto-detect on.
 */
data class LanguageState(
    /** True when the model should work the language out for itself. */
    val auto: Boolean,
    /** With auto-detect on: languages that are also likely to turn up. */
    val hints: List<String>,
    /** With auto-detect off: the one language spoken. Remembered while auto is on. */
    val single: String
) {
    /**
     * The name handed to the transcribers.
     *
     * Auto with nothing listed is blank, exactly as it always was. Auto with hints is the
     * list written as a sentence fragment ("French, Spanish"), which is what both the
     * Gemini prompt and the Live setup frame read naturally.
     */
    val spokenName: String
        get() = if (auto) hints.joinToString(", ") else single.trim()
}

object LanguageRules {

    /** The most hint languages worth listing; past this the prompt stops helping. */
    const val MAX_HINTS = 6

    fun parseList(raw: String?): List<String> =
        raw.orEmpty()
            .split('|', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

    fun writeList(items: List<String>): String = items.joinToString("|")

    /**
     * Rebuild the state from what is on disk.
     *
     * @param storedName the long-standing `language_name` value
     * @param autoFlag the new flag, null when it has never been written
     * @param hintsRaw the new hint list, null when it has never been written
     * @param singleRaw the remembered single language, null when never written
     */
    fun read(storedName: String, autoFlag: Boolean?, hintsRaw: String?, singleRaw: String?): LanguageState {
        val name = storedName.trim()
        val auto = autoFlag ?: name.isEmpty()
        val hints = parseList(hintsRaw)
        val single = singleRaw ?: if (auto) "" else name
        return LanguageState(auto = auto, hints = hints, single = single)
    }

    fun withHint(state: LanguageState, name: String): LanguageState {
        val cleaned = name.trim()
        if (cleaned.isEmpty() || state.hints.any { it.equals(cleaned, ignoreCase = true) }) return state
        return state.copy(hints = (state.hints + cleaned).take(MAX_HINTS))
    }

    fun withoutHint(state: LanguageState, name: String): LanguageState =
        state.copy(hints = state.hints.filterNot { it.equals(name, ignoreCase = true) })
}
