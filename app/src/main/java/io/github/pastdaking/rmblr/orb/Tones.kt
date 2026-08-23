package io.github.pastdaking.rmblr.orb

import android.content.Context
import io.github.pastdaking.rmblr.data.CleanupPreset
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A tone is a name and a system prompt. That is all it ever was.
 *
 * They used to be a fixed enum, which meant the six we happened to ship were the only six
 * that could ever exist. Now they are data: the built-ins are seeded from that enum and
 * anyone can add their own, because "write this as a LinkedIn post" is exactly the same
 * kind of thing as "clean this up" and there is no reason one is possible and the other
 * is not.
 */
data class Tone(
    val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val builtIn: Boolean
)

class ToneStore(context: Context) {

    private val prefs = context.getSharedPreferences("rmblr_tones", Context.MODE_PRIVATE)

    fun load(): List<Tone> {
        val raw = prefs.getString("tones", null) ?: return builtIns().also { save(it) }
        return runCatching { parse(raw) }.getOrDefault(builtIns()).ifEmpty { builtIns() }
    }

    fun save(tones: List<Tone>) {
        val array = JSONArray()
        tones.forEach { t ->
            array.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("description", t.description)
                    .put("prompt", t.prompt)
                    .put("builtIn", t.builtIn)
            )
        }
        prefs.edit().putString("tones", array.toString()).apply()
    }

    fun add(name: String, prompt: String): Tone {
        val tone = Tone(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifBlank { "My tone" },
            description = "Yours",
            prompt = prompt.trim(),
            builtIn = false
        )
        save(load() + tone)
        return tone
    }

    fun update(tone: Tone) {
        save(load().map { if (it.id == tone.id) tone else it })
    }

    fun delete(id: String) {
        save(load().filterNot { it.id == id && !it.builtIn })
    }

    /** Never null: a profile pointing at a deleted tone falls back to the first one. */
    fun byId(id: String?): Tone {
        val all = load()
        return all.firstOrNull { it.id == id } ?: all.first()
    }

    private fun parse(raw: String): List<Tone> {
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Tone(
                id = o.getString("id"),
                name = o.getString("name"),
                description = o.optString("description"),
                prompt = o.getString("prompt"),
                builtIn = o.optBoolean("builtIn", false)
            )
        }
    }

    /** The originals, carried over so nothing anyone had set up changes meaning. */
    private fun builtIns(): List<Tone> = CleanupPreset.values()
        .filter { it != CleanupPreset.CUSTOM }
        .map {
            Tone(
                id = it.name,
                name = it.displayName,
                description = it.description,
                prompt = it.systemPrompt,
                builtIn = true
            )
        }
}
