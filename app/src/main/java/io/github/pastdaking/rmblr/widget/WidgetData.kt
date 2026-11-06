package io.github.pastdaking.rmblr.widget

import android.content.Context
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.quick.QuickDictationState
import io.github.pastdaking.rmblr.quick.QuickPhase

data class WidgetRow(val text: String, val timestamp: Long)

/** Everything a widget draws, as plain values so the layouts can be rendered in tests. */
data class WidgetData(
    val phase: QuickPhase = QuickPhase.IDLE,
    val startedAt: Long = 0L,
    val levels: List<Float> = emptyList(),
    val rows: List<WidgetRow> = emptyList(),
    val toneName: String? = null,
    val now: Long = System.currentTimeMillis()
) {
    companion object {
        fun rowsFrom(items: List<DictationHistoryItem>, count: Int): List<WidgetRow> =
            items.asSequence()
                .filter { it.cleanedText.isNotBlank() }
                .take(count)
                .map { WidgetRow(it.cleanedText, it.timestamp) }
                .toList()

        fun live(context: Context): WidgetData {
            val snap = QuickDictationState.snapshot.value
            val items = HistoryRepository.getInstance(context).historyFlow.value
            return WidgetData(
                phase = snap.phase,
                startedAt = snap.startedAt,
                levels = QuickDictationState.levels.value,
                rows = rowsFrom(items, 2),
                toneName = snap.toneName
            )
        }
    }
}
