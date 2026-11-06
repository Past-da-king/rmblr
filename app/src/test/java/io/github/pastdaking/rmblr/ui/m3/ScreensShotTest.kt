package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.LanguageSelection
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.ui.screens.HistoryDetailContent
import io.github.pastdaking.rmblr.ui.screens.HistoryFilter
import io.github.pastdaking.rmblr.ui.screens.DictationsTab
import io.github.pastdaking.rmblr.ui.screens.HomeContent
import io.github.pastdaking.rmblr.ui.screens.LanguageSheet
import io.github.pastdaking.rmblr.ui.screens.RecordingContent
import io.github.pastdaking.rmblr.ui.screens.ResultContent
import io.github.pastdaking.rmblr.ui.screens.ToneSheet
import io.github.pastdaking.rmblr.ui.components.Space
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import org.junit.Test
import java.util.Calendar

/** One screenshot per screen built in the redesign, light and dark. */
class ScreensShotTest : ShotTest() {

    private val now: Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 6, 16, 20, 0)
    }.timeInMillis

    private fun at(h: Int, m: Int, daysAgo: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = now; add(Calendar.DAY_OF_YEAR, -daysAgo); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
        }.timeInMillis

    private val items = listOf(
        DictationHistoryItem(
            timestamp = at(14, 32), rawText = "running ten minutes late start without me and i will grab us a table",
            cleanedText = "Running ten minutes late, start without me and I will grab us a table.",
            mode = TranscriptionMode.POST_PROCESS_CLEANUP, preset = CleanupPreset.CASUAL_CHAT, durationSeconds = 8
        ),
        DictationHistoryItem(
            timestamp = at(11, 5), rawText = "hi dana um thanks for your time today as we discussed i will send the the revised timeline by thursday and uh loop in the design team",
            cleanedText = "Hi Dana, thank you for your time today. As discussed, I will send the revised timeline by Thursday and loop in the design team.",
            mode = TranscriptionMode.POST_PROCESS_CLEANUP, preset = CleanupPreset.FORMAL_EMAIL, durationSeconds = 21
        ),
        DictationHistoryItem(
            timestamp = at(9, 18), rawText = "milk oat flakes tomatoes lemons dish soap bin bags and something for dinner on friday",
            cleanedText = "Milk, oat flakes, tomatoes, lemons, dish soap, bin bags, and something for dinner on Friday.",
            mode = TranscriptionMode.POST_PROCESS_CLEANUP, preset = CleanupPreset.BULLET_POINTS, durationSeconds = 12
        ),
        DictationHistoryItem(
            timestamp = at(17, 52, 1), rawText = "could you send over the invoice when you get a chance thanks so much",
            cleanedText = "Could you send over the invoice when you get a chance? Thanks so much.",
            mode = TranscriptionMode.POST_PROCESS_CLEANUP, preset = CleanupPreset.CASUAL_CHAT, durationSeconds = 7
        )
    )

    private val tones = CleanupPreset.values().filter { it != CleanupPreset.CUSTOM }.map {
        Tone(it.name, it.displayName, it.description, it.systemPrompt, true)
    } + Tone("custom_1", "Meeting notes", "Yours", "Decisions and who does what", false)

    private val navItems = listOf(
        FloatingNavItem("Home", Icons.Default.Mic, "a"), FloatingNavItem("Profiles", Icons.Default.Layers, "b"),
        FloatingNavItem("History", Icons.Default.History, "c"), FloatingNavItem("Settings", Icons.Default.Settings, "d")
    )

    @Test fun home() = shot("01-home") {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            HomeContent(
                dateLabel = "FRI 6 NOV", setupDone = true, selection = LanguageSelection.AUTO,
                toneName = "Clean up", todayCount = 7, recent = items.take(2), error = null,
                onRecord = {}, onHelp = {}, onAuto = {}, onLanguageChip = {}, onMoreLanguages = {},
                onTone = {}, onSeeAll = {}, onOpenItem = {}, animate = false
            )
            androidx.compose.foundation.layout.Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
                FloatingNav(navItems, 0, {})
            }
        }
    }

    @Test fun recording() = shot("02-recording") {
        RecordingContent(
            working = false, elapsedSeconds = 14, toneName = "Clean up", languageLabel = "Auto",
            levels = sampleWaveform(), liveText = "Hi Dana, thanks for your time today. As discussed, I will send the revised timeline by Thursday and loop in the",
            streaming = true, onStop = {}, onCancel = {}, onTone = {}, onLanguage = {}, animate = false
        )
    }

    @Test fun result() = shot("03-result") {
        val snack = remember { SnackbarHostState() }
        androidx.compose.runtime.LaunchedEffect(Unit) { snack.showSnackbar("Copied to clipboard") }
        ResultContent(
            item = items[1], tones = tones, activeToneId = "FORMAL_EMAIL", rewriting = false,
            languageLabel = "English", snackbar = snack, onBack = {}, onTone = {}, onNewTone = {},
            onCopy = {}, onShare = {}, onTranslate = {}, startWithRawOpen = true
        )
    }

    @Test fun history() = shot("04-history") {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.lg).padding(top = Space.lg)) {
            Text("History", color = TextHigh, style = MaterialTheme.typography.displaySmall)
            DictationsTab(
                items = items, query = "", onQuery = {}, filter = HistoryFilter.ALL, onFilter = {},
                onOpen = {}, onDelete = {}, now = now, modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    @Test fun historyDetail() = shot("05-history-detail") {
        HistoryDetailContent(
            item = items[1], busy = false, note = null, onBack = {}, onCopy = {}, onShare = {},
            onRewrite = {}, onDelete = {}
        )
    }

    @Test fun toneSheet() = shot("06-tone-sheet") {
        ToneSheet(tones, "SMART_CLEAN", {}, {}, {})
    }

    @Test fun languageSheet() = shot("08-language-sheet") {
        LanguageSheet(LanguageSelection(auto = true, names = listOf("English", "Spanish")), {}, {})
    }

    @Test fun deleteSheet() = shot("09-delete-confirm-sheet") {
        ConfirmSheet(
            title = "Delete this dictation?",
            body = "It leaves your history. You can undo for a few seconds afterwards.",
            confirmLabel = "Delete", destructive = true, icon = Icons.Default.Delete,
            onConfirm = {}, onDismiss = {}
        )
    }
}
