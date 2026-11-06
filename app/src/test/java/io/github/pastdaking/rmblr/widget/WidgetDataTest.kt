package io.github.pastdaking.rmblr.widget

import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.TranscriptionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDataTest {

    private fun item(text: String, ts: Long) = DictationHistoryItem(
        timestamp = ts, rawText = text, cleanedText = text,
        mode = TranscriptionMode.POST_PROCESS_CLEANUP, preset = CleanupPreset.SMART_CLEAN
    )

    @Test fun rowsSkipBlankItemsAndKeepNewestFirst() {
        val rows = WidgetData.rowsFrom(listOf(item("one", 3), item("  ", 2), item("two", 1)), 2)
        assertEquals(listOf("one", "two"), rows.map { it.text })
    }

    @Test fun rowsAreLimited() {
        val rows = WidgetData.rowsFrom(List(10) { item("t$it", it.toLong()) }, 2)
        assertEquals(2, rows.size)
    }
}
