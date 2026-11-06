package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ai.GeminiApiClient
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.orb.ToneStore
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.copyToClipboard
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.ConfirmSheet
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.SheetButton
import io.github.pastdaking.rmblr.ui.m3.ToneTag
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalIconButton
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.ui.m3.formatElapsed
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.AlertWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.OnAlertWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.SecondaryWash
import io.github.pastdaking.rmblr.ui.theme.OnSecondaryWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.launch

private fun filterIcon(f: HistoryFilter): ImageVector = when (f) {
    HistoryFilter.ALL -> Icons.Default.GridView
    HistoryFilter.CHAT -> Icons.Default.Chat
    HistoryFilter.MAIL -> Icons.Default.Mail
    HistoryFilter.NOTES -> Icons.Default.Notes
}

/** Connected filter group: the selected one is a pill with icon and label, the rest icon buttons. */
@Composable
private fun FilterGroup(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    val all = HistoryFilter.values()
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        all.forEachIndexed { i, f ->
            val on = f == selected
            val first = i == 0
            val last = i == all.lastIndex
            val shape = if (on) RoundedCornerShape(20.dp) else RoundedCornerShape(
                topStart = if (first) 20.dp else 8.dp, bottomStart = if (first) 20.dp else 8.dp,
                topEnd = if (last) 20.dp else 8.dp, bottomEnd = if (last) 20.dp else 8.dp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(40.dp)
                    .pressable(onClick = { onSelect(f) })
                    .clip(shape)
                    .background(if (on) SecondaryWash else RaisedHigh)
                    .padding(horizontal = if (on) 16.dp else 12.dp)
                    .then(if (on) Modifier else Modifier.width(24.dp))
            ) {
                Icon(
                    filterIcon(f), f.label,
                    tint = if (on) OnSecondaryWash else TextMid, modifier = Modifier.size(22.dp)
                )
                if (on) {
                    Spacer(Modifier.width(8.dp))
                    Text(f.label, color = OnSecondaryWash, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * The dictations list: search, kind filter, grouped by day, swipe left to delete.
 * Deleting is immediate and [onDelete] is expected to offer Undo.
 */
@Composable
fun DictationsTab(
    items: List<DictationHistoryItem>,
    query: String,
    onQuery: (String) -> Unit,
    filter: HistoryFilter,
    onFilter: (HistoryFilter) -> Unit,
    onOpen: (DictationHistoryItem) -> Unit,
    onDelete: (DictationHistoryItem) -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis()
) {
    val groups = remember(items, query, filter, now / 60000) {
        groupByDay(filterHistory(items, query, filter), now)
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = Space.navClear),
        modifier = modifier.fillMaxWidth()
    ) {
        if (items.isNotEmpty()) {
            item(key = "search") {
                TonalTextField(
                    query, onQuery,
                    placeholder = "Search your dictations",
                    leadingIcon = Icons.Default.Search
                )
            }
            item(key = "filters") {
                Box(Modifier.padding(top = 8.dp, bottom = 8.dp)) { FilterGroup(filter, onFilter) }
            }
        }
        if (groups.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(top = Space.xxl, start = 4.dp)) {
                    Text(
                        if (items.isEmpty()) "Nothing dictated yet" else "Nothing matches that",
                        color = TextHigh, style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        if (items.isEmpty()) "Tap Home, or tap the orb in any text box, and say something. It lands here."
                        else "Try a word you know you actually said.",
                        color = TextMid, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        groups.forEach { group ->
            item(key = "h_" + group.label) {
                Text(
                    group.label, color = TextMid, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp)
                )
            }
            itemsIndexed(group.items, key = { _, it -> it.id }) { index, item ->
                SwipeRow(onDelete = { onDelete(item) }) {
                    HistoryRow(item, index, group.items.size, onClick = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun SwipeRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
        positionalThreshold = { total -> total * 0.6f }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AlertWash)
                    .padding(end = 24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, null, tint = OnAlertWash)
                    Text("Delete", color = OnAlertWash, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    ) { content() }
}

private fun Modifier.dashedBorder(color: androidx.compose.ui.graphics.Color, radius: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        )
    }

/** One dictation in full: the rewritten text, what was said, facts, and what you can do with it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryDetail(
    item: DictationHistoryItem,
    historyRepo: HistoryRepository,
    onBack: () -> Unit,
    onDeleted: (DictationHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val tones = remember { ToneStore(context).load() }
    var showTones by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    HistoryDetailContent(
        item = item,
        busy = busy,
        note = note,
        onBack = onBack,
        onCopy = { copyToClipboard(context, "RMBLR dictation", item.cleanedText); note = "Copied to clipboard" },
        onShare = {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, item.cleanedText)
            }
            runCatching {
                context.startActivity(
                    android.content.Intent.createChooser(send, "Share").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        },
        onRewrite = { showTones = true },
        onDelete = { confirmDelete = true },
        modifier = modifier
    )

    if (showTones) {
        ToneSheet(
            tones = tones,
            selectedId = if (item.preset != CleanupPreset.CUSTOM) item.preset.name else null,
            subtitle = "Rewrites what you said",
            onNewTone = null,
            onDismiss = { showTones = false },
            onPick = { tone ->
                showTones = false
                busy = true
                scope.launch {
                    // Rewrites what was SAID, not what came back, so tones never compound.
                    val result = GeminiApiClient.postProcessText(
                        inputText = item.rawText.ifBlank { item.cleanedText },
                        apiKey = prefs.getEffectiveApiKey(),
                        preset = CleanupPreset.CUSTOM,
                        customPrompt = tone.prompt
                    )
                    busy = false
                    result.onSuccess { text ->
                        if (text.isNotBlank()) {
                            historyRepo.updateHistoryItem(item.copy(cleanedText = text, preset = presetOf(tone)))
                            note = null
                        }
                    }.onFailure { note = it.message ?: "Could not rewrite that." }
                }
            }
        )
    }
    if (confirmDelete) {
        ConfirmSheet(
            title = "Delete this dictation?",
            body = "It leaves your history. You can undo for a few seconds afterwards.",
            confirmLabel = "Delete",
            destructive = true,
            icon = Icons.Default.Delete,
            onConfirm = {
                confirmDelete = false
                historyRepo.deleteHistoryItem(item.id)
                onDeleted(item)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun HistoryDetailContent(
    item: DictationHistoryItem,
    busy: Boolean,
    note: String?,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRewrite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = categoryOf(item.preset)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg)
            .padding(top = Space.md, bottom = Space.xl)
            .testTag("history_detail")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TonalIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, container = Ink)
            Text(
                "Dictation", color = TextHigh, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            KindAvatar(item.preset, Modifier.size(48.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    when (kind) { HistoryFilter.CHAT -> "Chat"; HistoryFilter.MAIL -> "Mail"; else -> "Note" },
                    color = TextHigh, style = MaterialTheme.typography.titleLarge
                )
                Text(
                    dayLabel(item.timestamp, System.currentTimeMillis()) + ", " + clockLabel(item.timestamp),
                    color = TextMid, style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.height(Space.lg))
        TonalCard(
            container = RaisedHigh,
            padding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Eyebrow(if (item.rawText.isNotBlank() && item.rawText != item.cleanedText) "Rewritten" else "Text", modifier = Modifier.weight(1f))
                ToneTag(shortToneName(item.preset), icon = toneIcon(item.preset))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                item.cleanedText,
                color = if (busy) TextMid else TextHigh,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            )
        }
        if (item.rawText.isNotBlank() && item.rawText != item.cleanedText) {
            Spacer(Modifier.height(Space.md))
            Column(
                Modifier
                    .fillMaxWidth()
                    .dashedBorder(Line, 28.dp)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Eyebrow("As spoken")
                Spacer(Modifier.height(8.dp))
                Text(item.rawText, color = TextMid, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(Space.xl))
        val facts = listOf(
            Triple("Length", formatElapsed(item.durationSeconds), Icons.Default.Schedule),
            Triple("Words", wordCount(item.cleanedText).toString(), Icons.Default.Notes),
            Triple("Tone", shortToneName(item.preset), Icons.Default.AutoFixHigh),
            Triple(
                "Mode",
                if (item.mode == TranscriptionMode.DIRECT_VERBATIM) "As spoken" else "Polished",
                Icons.Default.RecordVoiceOver
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            facts.chunked(2).forEachIndexed { r, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (label, value, icon) ->
                        val top = if (r == 0) 20.dp else 8.dp
                        val bottom = if (r == 1) 20.dp else 8.dp
                        TonalCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(top, top, bottom, bottom),
                            padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = TextMid, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(label, color = TextMid, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(value, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SheetButton("Copy", onCopy, Accent, androidx.compose.ui.graphics.Color.White, Modifier.weight(1f))
            SheetButton("Share", onShare, AccentWash, OnAccentWash, Modifier.weight(1f))
            TonalIconButton(
                Icons.Default.AutoFixHigh, "Rewrite in another tone", onRewrite,
                size = 56.dp, container = AccentWash, tint = OnAccentWash
            )
        }
        if (note != null) {
            Text(
                note, color = TextMid, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.lg)
                .pressable(onClick = onDelete)
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Delete, null, tint = Alert, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Delete dictation", color = Alert, style = MaterialTheme.typography.titleMedium)
        }
    }
}
