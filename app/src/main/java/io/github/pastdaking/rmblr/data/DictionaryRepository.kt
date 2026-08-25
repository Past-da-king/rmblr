package io.github.pastdaking.rmblr.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * One word and, optionally, the way it is supposed to come out.
 *
 * [spelling] is blank for the ordinary case, where the word is simply unusual and the
 * transcriber needs to know it exists. It earns its keep for the awkward case: you say
 * "kay-cee" and mean "KC", or you say a colleague's name and get a different name back
 * every time.
 */
data class DictionaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val spelling: String = ""
) {
    /** What the model is told: either just the word, or "heard → written". */
    fun hint(): String = if (spelling.isBlank()) word else "$word (write it as \"$spelling\")"
}

/**
 * The words RMBLR should always get right.
 *
 * Every transcriber, however good, guesses at names, jargon and anything it has not
 * been trained to expect — and it guesses differently each time, which is worse than
 * guessing wrong consistently. This is the list of words it stops guessing about: they
 * are handed to whichever engine is running as a vocabulary hint before it hears a
 * thing.
 *
 * Nothing here is a correction after the fact. It is context supplied up front, which
 * is why it works on the streaming path too.
 */
class DictionaryRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("rmblr_dictionary", Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow<List<DictionaryEntry>>(emptyList())
    val entries: StateFlow<List<DictionaryEntry>> = _entries.asStateFlow()

    init {
        load()
    }

    fun add(word: String, spelling: String = ""): Boolean {
        val cleaned = word.trim()
        if (cleaned.isEmpty()) return false
        if (_entries.value.any { it.word.equals(cleaned, ignoreCase = true) }) return false
        save(listOf(DictionaryEntry(word = cleaned, spelling = spelling.trim())) + _entries.value)
        return true
    }

    fun delete(id: String) {
        save(_entries.value.filterNot { it.id == id })
    }

    fun contains(word: String): Boolean =
        _entries.value.any { it.word.equals(word.trim(), ignoreCase = true) }

    /**
     * The line handed to the model, or null when there is nothing to say.
     *
     * Capped deliberately. A vocabulary hint is prepended to every single request, so an
     * unbounded list would quietly tax every dictation you ever make — and a model given
     * three hundred words to watch for starts seeing them in audio that does not contain
     * them. The most recently added win, because those are the ones you are using now.
     */
    fun promptHint(limit: Int = MAX_IN_PROMPT): String? {
        val words = _entries.value.take(limit)
        if (words.isEmpty()) return null
        return "These words, names and terms come up often in this speaker's dictation. " +
            "Spell them exactly as given whenever you hear them, and do not substitute a " +
            "similar-sounding ordinary word for one of them: " +
            words.joinToString("; ") { it.hint() } + ". "
    }

    /** Groq's `prompt` field takes plain context rather than an instruction. */
    fun plainWordList(limit: Int = MAX_IN_PROMPT): String? {
        val words = _entries.value.take(limit)
        if (words.isEmpty()) return null
        return words.joinToString(", ") { it.spelling.ifBlank { it.word } }
    }

    private fun load() {
        val raw = prefs.getString("entries", null) ?: return
        val parsed = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                DictionaryEntry(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    word = o.optString("word"),
                    spelling = o.optString("spelling")
                )
            }.filter { it.word.isNotBlank() }
        }.getOrDefault(emptyList())
        _entries.value = parsed
    }

    private fun save(list: List<DictionaryEntry>) {
        _entries.value = list
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("word", it.word)
                    .put("spelling", it.spelling)
            )
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    companion object {
        private const val MAX_IN_PROMPT = 60

        @Volatile
        private var instance: DictionaryRepository? = null

        fun getInstance(context: Context): DictionaryRepository =
            instance ?: synchronized(this) {
                instance ?: DictionaryRepository(context.applicationContext).also { instance = it }
            }

        /**
         * Words worth offering to add, mined from what has already been dictated.
         *
         * Typing a vocabulary list is a chore nobody does, so the list suggests itself:
         * anything said more than once that is not an everyday English word is probably
         * a name, a place or a piece of jargon — exactly the things a transcriber
         * fumbles. Capitalised words count from a single appearance, since a name only
         * has to show up once to be worth pinning down.
         */
        fun suggestionsFrom(
            history: List<DictationHistoryItem>,
            already: List<DictionaryEntry>,
            limit: Int = 12
        ): List<String> {
            val known = already.map { it.word.lowercase() }.toSet()
            val counts = mutableMapOf<String, Int>()
            val display = mutableMapOf<String, String>()

            history.asSequence()
                .flatMap { (it.rawText + " " + it.cleanedText).split(WORD_SPLIT).asSequence() }
                .map { it.trim('\'', '"', '.', ',', '!', '?', ':', ';', '(', ')', '-') }
                .filter { it.length >= 4 && it.any { c -> c.isLetter() } && it.none { c -> c.isDigit() } }
                .forEach { word ->
                    val key = word.lowercase()
                    if (key in known || key in COMMON_WORDS) return@forEach
                    counts[key] = (counts[key] ?: 0) + 1
                    // Keep the capitalised form if it ever appeared as one: "Thabo", not "thabo".
                    val seen = display[key]
                    if (seen == null || (word.first().isUpperCase() && !seen.first().isUpperCase())) {
                        display[key] = word
                    }
                }

            return counts.entries
                .filter { (key, count) -> count > 1 || display[key]?.first()?.isUpperCase() == true }
                .sortedByDescending { it.value }
                .mapNotNull { display[it.key] }
                .take(limit)
        }

        private val WORD_SPLIT = Regex("[^\\p{L}'-]+")

        /**
         * Everyday words, skipped when mining. Not a stopword list in the search sense —
         * it only has to be good enough that the suggestions are mostly proper nouns and
         * jargon rather than "because" and "actually".
         */
        private val COMMON_WORDS = setOf(
            "about", "actually", "after", "again", "against", "already", "also", "always",
            "another", "anything", "around", "because", "been", "before", "being", "better",
            "between", "both", "bring", "came", "could", "does", "doing", "done", "down",
            "each", "even", "every", "everything", "first", "from", "gave", "getting", "going",
            "gone", "good", "great", "have", "having", "here", "himself", "into", "just",
            "keep", "kind", "know", "later", "less", "let's", "like", "little", "long",
            "look", "looking", "made", "make", "making", "many", "maybe", "mean", "might",
            "more", "most", "much", "must", "need", "never", "next", "nothing", "only",
            "other", "over", "people", "perhaps", "place", "please", "point", "probably",
            "quite", "rather", "really", "right", "said", "same", "says", "seen", "sent",
            "should", "since", "some", "something", "soon", "sorry", "still", "such",
            "sure", "take", "than", "thanks", "that", "their", "them", "then", "there",
            "these", "they", "thing", "things", "think", "this", "those", "though",
            "thought", "through", "time", "today", "together", "tomorrow", "took", "very",
            "want", "wanted", "well", "went", "were", "what", "when", "where", "which",
            "while", "will", "with", "without", "work", "working", "would", "yeah",
            "year", "your", "yourself"
        )
    }
}
