package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import java.util.Calendar
import java.util.TimeZone

/** The short name a tone goes by on chips and tags ("Clean up", not "Clean & Smooth"). */
fun shortToneName(preset: CleanupPreset): String = when (preset) {
    CleanupPreset.SMART_CLEAN -> "Clean up"
    CleanupPreset.FORMAL_EMAIL -> "Professional"
    CleanupPreset.CASUAL_CHAT -> "Casual"
    CleanupPreset.BULLET_POINTS -> "Bullets"
    CleanupPreset.FIX_GRAMMAR -> "Grammar"
    CleanupPreset.CONCISE_SUMMARY -> "Summary"
    CleanupPreset.CUSTOM -> "Custom"
}

fun toneIcon(preset: CleanupPreset): ImageVector = when (preset) {
    CleanupPreset.SMART_CLEAN -> Icons.Default.AutoFixHigh
    CleanupPreset.FORMAL_EMAIL -> Icons.Default.Work
    CleanupPreset.CASUAL_CHAT -> Icons.Default.Mood
    CleanupPreset.BULLET_POINTS -> Icons.Default.FormatListBulleted
    CleanupPreset.FIX_GRAMMAR -> Icons.Default.Spellcheck
    CleanupPreset.CONCISE_SUMMARY -> Icons.Default.ShortText
    CleanupPreset.CUSTOM -> Icons.Default.EditNote
}

/** Filters on the history list. A dictation's tone decides which kind of writing it was. */
enum class HistoryFilter(val label: String) { ALL("All"), CHAT("Chat"), MAIL("Mail"), NOTES("Notes") }

fun categoryOf(preset: CleanupPreset): HistoryFilter = when (preset) {
    CleanupPreset.CASUAL_CHAT -> HistoryFilter.CHAT
    CleanupPreset.FORMAL_EMAIL -> HistoryFilter.MAIL
    else -> HistoryFilter.NOTES
}

/** Search raw and tidied text (you remember what you said better than what came back), then filter. */
fun filterHistory(
    items: List<DictationHistoryItem>,
    query: String,
    filter: HistoryFilter
): List<DictationHistoryItem> {
    val q = query.trim()
    return items.filter { item ->
        (filter == HistoryFilter.ALL || categoryOf(item.preset) == filter) &&
            (q.isEmpty() ||
                item.rawText.contains(q, ignoreCase = true) ||
                item.cleanedText.contains(q, ignoreCase = true))
    }
}

data class DayGroup(val label: String, val items: List<DictationHistoryItem>)

private val DAYS = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Today", "Yesterday", "Mon 3 Nov", or "3 Nov 2025" for another year. */
fun dayLabel(timestamp: Long, now: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val t = Calendar.getInstance(zone).apply { timeInMillis = timestamp }
    val today = Calendar.getInstance(zone).apply { timeInMillis = now }
    fun same(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (same(t, today)) return "Today"
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (same(t, yesterday)) return "Yesterday"
    val date = "${t.get(Calendar.DAY_OF_MONTH)} ${MONTHS[t.get(Calendar.MONTH)]}"
    return if (t.get(Calendar.YEAR) == today.get(Calendar.YEAR)) "${DAYS[t.get(Calendar.DAY_OF_WEEK) - 1]} $date"
    else "$date ${t.get(Calendar.YEAR)}"
}

/** Newest first, split into day groups in the order they appear. */
fun groupByDay(
    items: List<DictationHistoryItem>,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault()
): List<DayGroup> {
    val out = LinkedHashMap<String, MutableList<DictationHistoryItem>>()
    items.sortedByDescending { it.timestamp }.forEach {
        out.getOrPut(dayLabel(it.timestamp, now, zone)) { mutableListOf() }.add(it)
    }
    return out.map { DayGroup(it.key, it.value) }
}

fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

/** "14:32" in the given zone. */
fun clockLabel(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(zone).apply { timeInMillis = timestamp }
    return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}
