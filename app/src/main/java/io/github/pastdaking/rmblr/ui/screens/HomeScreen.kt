package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.LanguageSelection
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.orb.ToneStore
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.M3Chip
import io.github.pastdaking.rmblr.ui.m3.RecordButton
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalIconButton
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import java.util.Calendar

/** The tone id the Home card shows as selected: the stored pick when it still matches. */
fun currentToneId(prefs: PreferencesManager): String {
    val preset = prefs.getCleanupPreset()
    val stored = prefs.getHomeToneId()
    return if (preset == io.github.pastdaking.rmblr.data.CleanupPreset.CUSTOM &&
        stored != null && stored.startsWith("custom_")
    ) stored else preset.name
}

/** Make a tone the one new dictations use. */
fun applyTone(prefs: PreferencesManager, tone: Tone) {
    val preset = presetOf(tone)
    prefs.setCleanupPreset(preset)
    if (preset == io.github.pastdaking.rmblr.data.CleanupPreset.CUSTOM) prefs.setCustomPrompt(tone.prompt)
    prefs.setHomeToneId(tone.id)
}

/** Whether the overlay and accessibility permissions the orb needs are both granted. */
fun orbSetupDone(context: Context): Boolean {
    val overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    return overlay && accessibilityEnabled(context)
}

private val MONTHS_UP = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
private val DAYS_UP = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

/** "FRI 6 NOV" */
fun eyebrowDate(now: Long = System.currentTimeMillis()): String {
    val c = Calendar.getInstance().apply { timeInMillis = now }
    return "${DAYS_UP[c.get(Calendar.DAY_OF_WEEK) - 1]} ${c.get(Calendar.DAY_OF_MONTH)} ${MONTHS_UP[c.get(Calendar.MONTH)]}"
}

fun todayCount(items: List<DictationHistoryItem>, now: Long = System.currentTimeMillis()): Int =
    items.count { dayLabel(it.timestamp, now) == "Today" }

/**
 * Home, Recording and Result as one flow driven by [HomeSession]. Recording and Result
 * are full-screen; [onFullScreen] tells the host to hide the floating navigation.
 */
@Composable
fun HomeScreen(
    session: HomeSession,
    prefs: PreferencesManager,
    history: HistoryRepository,
    onOpenSetup: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenItem: (DictationHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by history.historyFlow.collectAsState()
    val selection by prefs.languageSelectionFlow.collectAsState()
    val preset by prefs.presetFlow.collectAsState()
    val toneStore = remember { ToneStore(context) }
    var tones by remember { mutableStateOf(toneStore.load()) }
    var toneVersion by remember { mutableStateOf(0) }
    var showTones by remember { mutableStateOf(false) }
    var showNewTone by remember { mutableStateOf(false) }
    var showLanguages by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ready = orbSetupDone(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val selectedToneId = remember(preset, toneVersion) { currentToneId(prefs) }
    val selectedTone = tones.firstOrNull { it.id == selectedToneId } ?: tones.first()

    when (val phase = session.phase) {
        HomePhase.Idle -> HomeContent(
            dateLabel = eyebrowDate(),
            setupDone = ready,
            selection = selection,
            toneName = toneShortName(selectedTone),
            todayCount = todayCount(items),
            recent = items.take(3),
            error = session.error,
            onRecord = { session.start() },
            onHelp = onOpenSetup,
            onAuto = { prefs.setLanguageSelection(LanguageSelection.AUTO) },
            onLanguageChip = { prefs.setLanguageSelection(selection.toggle(it)) },
            onMoreLanguages = { showLanguages = true },
            onTone = { showTones = true },
            onSeeAll = onSeeAll,
            onOpenItem = onOpenItem,
            modifier = modifier
        )
        HomePhase.Recording, HomePhase.Working -> {
            BackHandler { session.cancel() }
            RecordingContent(
                working = phase == HomePhase.Working,
                elapsedSeconds = session.elapsedSeconds,
                toneName = toneShortName(selectedTone),
                languageLabel = languageChipLabel(selection),
                levels = session.levels,
                liveText = session.liveText,
                streaming = session.streaming,
                onStop = { session.stop() },
                onCancel = { session.cancel() },
                onTone = { showTones = true },
                onLanguage = { showLanguages = true },
                modifier = modifier
            )
        }
        is HomePhase.Result -> {
            BackHandler { session.reset() }
            ResultRoute(
                item = phase.item,
                session = session,
                prefs = prefs,
                tones = tones,
                onNewTone = { showNewTone = true },
                modifier = modifier
            )
        }
    }

    if (showTones) {
        ToneSheet(
            tones = tones,
            selectedId = selectedToneId,
            onPick = { applyTone(prefs, it); toneVersion++; showTones = false },
            onNewTone = { showTones = false; showNewTone = true },
            onDismiss = { showTones = false }
        )
    }
    if (showNewTone) {
        NewToneSheet(
            onSave = { n, p ->
                val tone = toneStore.add(n, p)
                tones = toneStore.load()
                applyTone(prefs, tone); toneVersion++
                showNewTone = false
            },
            onDismiss = { showNewTone = false }
        )
    }
    if (showLanguages) {
        LanguageSheet(selection, { prefs.setLanguageSelection(it) }, { showLanguages = false })
    }
}

/** The Home screen drawn from plain values, so it can be shown in tests and previews. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeContent(
    dateLabel: String,
    setupDone: Boolean,
    selection: LanguageSelection,
    toneName: String,
    todayCount: Int,
    recent: List<DictationHistoryItem>,
    error: String?,
    onRecord: () -> Unit,
    onHelp: () -> Unit,
    onAuto: () -> Unit,
    onLanguageChip: (String) -> Unit,
    onMoreLanguages: () -> Unit,
    onTone: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenItem: (DictationHistoryItem) -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg)
            .padding(top = Space.lg, bottom = Space.navClear)
            .testTag("home_screen_content")
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Eyebrow(dateLabel)
                Text(
                    if (setupDone) "Ready" else "Almost there",
                    color = TextHigh,
                    style = MaterialTheme.typography.displaySmall
                )
            }
            TonalIconButton(Icons.Default.HelpOutline, "Setup and help", onHelp)
        }

        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            RecordButton(recording = false, onClick = onRecord, size = 152.dp, animate = animate)
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tap to dictate", color = TextHigh, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (error != null) error
                else if (setupDone) "Or hold the orb in any app for tones"
                else "Finish setup to get the orb in every app",
                color = if (error != null) Alert else TextMid,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(Modifier.height(Space.lg))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            M3Chip("Auto", selected = selection.auto, onClick = onAuto)
            selection.names.forEach { n ->
                M3Chip(n, selected = true, onClick = { onLanguageChip(n) })
            }
            M3Chip("More", selected = false, onClick = onMoreLanguages, leadingIcon = Icons.Default.Add)
        }
        Spacer(Modifier.height(Space.md))

        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
            TonalCard(
                modifier = Modifier.weight(1.7f),
                container = AccentWash,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                onClick = onTone
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = OnAccentWash, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Tone", color = OnAccentWash)
                        Text(toneName, color = OnAccentWash, style = MaterialTheme.typography.titleLarge)
                    }
                    Icon(Icons.Default.ExpandMore, null, tint = OnAccentWash)
                }
            }
            TonalCard(
                modifier = Modifier.weight(1f),
                container = RaisedHigh,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Eyebrow("Today")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$todayCount", color = TextHigh, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (todayCount == 1) "note" else "notes",
                        color = TextMid, style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(Space.xl))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Recent", color = TextHigh, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "See all",
                    color = io.github.pastdaking.rmblr.ui.theme.AccentText,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(4.dp)
                        .pressable(onClick = onSeeAll)
                )
            }
            Spacer(Modifier.height(Space.md))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                recent.forEachIndexed { i, item ->
                    HistoryRow(item, i, recent.size, onClick = { onOpenItem(item) })
                }
            }
        }
    }
}
