package io.github.pastdaking.rmblr.ui.screens

import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.LanguageSelection
import io.github.pastdaking.rmblr.data.TranscriptionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ScreenLogicTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun ts(day: Int, hour: Int = 12): Long = Calendar.getInstance(utc).apply {
        clear(); set(2026, Calendar.NOVEMBER, day, hour, 0)
    }.timeInMillis

    private fun item(id: String, t: Long, preset: CleanupPreset = CleanupPreset.SMART_CLEAN, raw: String = "raw", clean: String = "clean") =
        DictationHistoryItem(id = id, timestamp = t, rawText = raw, cleanedText = clean,
            mode = TranscriptionMode.POST_PROCESS_CLEANUP, preset = preset)

    @Test fun dayLabels() {
        val now = ts(6)
        assertEquals("Today", dayLabel(ts(6, 1), now, utc))
        assertEquals("Yesterday", dayLabel(ts(5, 23), now, utc))
        assertEquals("Mon 2 Nov", dayLabel(ts(2), now, utc))
    }

    @Test fun groupsNewestFirstByDay() {
        val g = groupByDay(listOf(item("a", ts(5)), item("b", ts(6, 9)), item("c", ts(6, 15))), ts(6), utc)
        assertEquals(listOf("Today", "Yesterday"), g.map { it.label })
        assertEquals(listOf("c", "b"), g[0].items.map { it.id })
    }

    @Test fun filterSearchesRawAndCleanedAndKind() {
        val items = listOf(
            item("1", 1, CleanupPreset.CASUAL_CHAT, raw = "umm lunch", clean = "Lunch?"),
            item("2", 2, CleanupPreset.FORMAL_EMAIL, raw = "invoice", clean = "Please send the invoice.")
        )
        assertEquals(listOf("1"), filterHistory(items, "umm", HistoryFilter.ALL).map { it.id })
        assertEquals(listOf("2"), filterHistory(items, "", HistoryFilter.MAIL).map { it.id })
        assertTrue(filterHistory(items, "nothing", HistoryFilter.ALL).isEmpty())
    }

    @Test fun languageSelectionKeepsLegacyBehaviour() {
        assertEquals("", LanguageSelection.fromLegacy("").effectiveName)
        assertEquals("isiZulu", LanguageSelection.fromLegacy("isiZulu").effectiveName)
        assertTrue(LanguageSelection.fromLegacy("isiZulu").isSinglePinned)
    }

    @Test fun languageSelectionModes() {
        val mixed = LanguageSelection(false, listOf("English", "Spanish"))
        assertEquals("English and Spanish", mixed.effectiveName)
        assertEquals("English or Spanish", mixed.copy(auto = true).effectiveName)
        assertTrue(!mixed.isSinglePinned)
        assertEquals(LanguageSelection.AUTO, LanguageSelection(false, listOf("English")).tapChip("english"))
        assertEquals(listOf("French"), LanguageSelection.AUTO.tapChip("French").names)
        assertTrue(mixed.toggle("English").toggle("Spanish").auto)
    }

    @Test fun languageSelectionRoundTrips() {
        val s = LanguageSelection(false, listOf("English", "Spanish"))
        assertEquals(s, LanguageSelection.decode(s.encode()))
        assertNull(LanguageSelection.decode(""))
    }

    @Test fun instructionRules() {
        assertNull(instructionFor(TranscriptionMode.DIRECT_VERBATIM, CleanupPreset.SMART_CLEAN, "x"))
        assertEquals("mine", instructionFor(TranscriptionMode.POST_PROCESS_CLEANUP, CleanupPreset.CUSTOM, "mine"))
        assertNull(instructionFor(TranscriptionMode.POST_PROCESS_CLEANUP, CleanupPreset.CUSTOM, " "))
        assertEquals(CleanupPreset.SMART_CLEAN.systemPrompt,
            instructionFor(TranscriptionMode.POST_PROCESS_CLEANUP, CleanupPreset.SMART_CLEAN, ""))
    }

    @Test fun languageMatchesAllowsAnyTypedLanguage() {
        assertTrue(languageMatches("Klingon", emptyList()).contains("Klingon"))
        assertTrue(languageMatches("span", emptyList()).contains("Spanish"))
    }
}
