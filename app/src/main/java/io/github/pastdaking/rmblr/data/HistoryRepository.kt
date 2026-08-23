package io.github.pastdaking.rmblr.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ilight_history_prefs", Context.MODE_PRIVATE)

    private val _historyFlow = MutableStateFlow<List<DictationHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<DictationHistoryItem>> = _historyFlow.asStateFlow()

    private val _snippetsFlow = MutableStateFlow<List<SnippetItem>>(emptyList())
    val snippetsFlow: StateFlow<List<SnippetItem>> = _snippetsFlow.asStateFlow()

    init {
        loadHistory()
        loadSnippets()
    }

    private fun loadHistory() {
        val jsonStr = prefs.getString("history_items", null)
        val list = mutableListOf<DictationHistoryItem>()
        if (jsonStr != null) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        DictationHistoryItem(
                            id = obj.optString("id"),
                            timestamp = obj.optLong("timestamp"),
                            rawText = obj.optString("rawText"),
                            cleanedText = obj.optString("cleanedText"),
                            mode = try {
                                TranscriptionMode.valueOf(obj.optString("mode"))
                            } catch (e: Exception) {
                                TranscriptionMode.POST_PROCESS_CLEANUP
                            },
                            preset = try {
                                CleanupPreset.valueOf(obj.optString("preset"))
                            } catch (e: Exception) {
                                CleanupPreset.SMART_CLEAN
                            },
                            durationSeconds = obj.optInt("durationSeconds", 0)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _historyFlow.value = list
    }

    fun addHistoryItem(item: DictationHistoryItem) {
        val current = _historyFlow.value.toMutableList()
        current.add(0, item) // newest first
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        _historyFlow.value = current
        saveHistory(current)
    }

    fun clearHistory() {
        _historyFlow.value = emptyList()
        prefs.edit().remove("history_items").apply()
    }

    fun deleteHistoryItem(id: String) {
        val current = _historyFlow.value.filter { it.id != id }
        _historyFlow.value = current
        saveHistory(current)
    }

    private fun saveHistory(list: List<DictationHistoryItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("rawText", item.rawText)
                put("cleanedText", item.cleanedText)
                put("mode", item.mode.name)
                put("preset", item.preset.name)
                put("durationSeconds", item.durationSeconds)
            }
            array.put(obj)
        }
        prefs.edit().putString("history_items", array.toString()).apply()
    }

    private fun loadSnippets() {
        val jsonStr = prefs.getString("snippet_items", null)
        val list = mutableListOf<SnippetItem>()
        if (jsonStr != null) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SnippetItem(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            content = obj.optString("content"),
                            shortcut = obj.optString("shortcut")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Seed default handy templates
            list.addAll(
                listOf(
                    SnippetItem(title = "Meeting Follow-up", content = "Thanks for your time today! As discussed, here are the next steps:\n- Action item 1\n- Action item 2\nLooking forward to catching up soon.", shortcut = "/meet"),
                    SnippetItem(title = "Quick Ack", content = "Got it, thanks! Reviewing this right now and will get back to you shortly.", shortcut = "/ack"),
                    SnippetItem(title = "Gentle Check-in", content = "Hi there, just gently following up on our previous conversation to see if you have any updates. Thanks!", shortcut = "/ping")
                )
            )
            saveSnippets(list)
        }
        _snippetsFlow.value = list
    }

    fun addSnippet(snippet: SnippetItem) {
        val current = _snippetsFlow.value.toMutableList()
        current.add(0, snippet)
        _snippetsFlow.value = current
        saveSnippets(current)
    }

    fun deleteSnippet(id: String) {
        val current = _snippetsFlow.value.filter { it.id != id }
        _snippetsFlow.value = current
        saveSnippets(current)
    }

    private fun saveSnippets(list: List<SnippetItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("content", item.content)
                put("shortcut", item.shortcut)
            }
            array.put(obj)
        }
        prefs.edit().putString("snippet_items", array.toString()).apply()
    }

    companion object {
        @Volatile
        private var instance: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: HistoryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
