package io.github.pastdaking.rmblr.data

/**
 * What the user told the app about the language they speak.
 *
 * - Auto with no names: work it out (the default, and what an upgrading user who never set
 *   a language already had).
 * - Auto with names: work it out, but these are likely.
 * - Not auto, one name: that language.
 * - Not auto, several names: mixed speech, any of these in one dictation.
 *
 * [effectiveName] is what the engines have always been given as "the spoken language",
 * so the existing transcription code keeps working unchanged: blank means auto, a single
 * name is the old pinned language.
 */
data class LanguageSelection(val auto: Boolean, val names: List<String>) {

    val effectiveName: String
        get() = when {
            names.isEmpty() -> ""
            auto -> names.joinToString(" or ")
            else -> names.joinToString(" and ")
        }

    /** True when exactly one language is pinned, so an ISO code may be sent as well. */
    val isSinglePinned: Boolean get() = !auto && names.size == 1

    /** Tap on a one-tap chip: that language alone, or back to Auto when it was the only one. */
    fun tapChip(name: String): LanguageSelection =
        if (isSinglePinned && names[0].equals(name, ignoreCase = true)) AUTO
        else LanguageSelection(auto = false, names = listOf(name))

    /** Toggle one language in the multi-select list, keeping the Auto switch as it is. */
    fun toggle(name: String): LanguageSelection {
        val has = names.any { it.equals(name, ignoreCase = true) }
        val next = if (has) names.filterNot { it.equals(name, ignoreCase = true) } else names + name
        return copy(names = next, auto = auto || next.isEmpty())
    }

    fun withAuto(on: Boolean): LanguageSelection =
        if (on) copy(auto = true) else copy(auto = names.isEmpty())

    fun encode(): String = (if (auto) "A" else "P") + "|" + names.joinToString("|")

    companion object {
        val AUTO = LanguageSelection(auto = true, names = emptyList())

        fun decode(raw: String?): LanguageSelection? {
            if (raw.isNullOrEmpty()) return null
            val parts = raw.split('|')
            val auto = parts.firstOrNull() == "A"
            val names = parts.drop(1).filter { it.isNotBlank() }
            return LanguageSelection(auto || names.isEmpty(), names)
        }

        /** An upgrading user's single language_name becomes a pinned language, blank becomes Auto. */
        fun fromLegacy(name: String?): LanguageSelection {
            val n = name?.trim().orEmpty()
            return if (n.isEmpty()) AUTO else LanguageSelection(false, listOf(n))
        }
    }
}
