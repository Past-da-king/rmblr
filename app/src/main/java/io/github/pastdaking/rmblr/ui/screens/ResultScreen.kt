package io.github.pastdaking.rmblr.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ai.GeminiApiClient
import io.github.pastdaking.rmblr.ai.Translator
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.copyToClipboard
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.M3Chip
import io.github.pastdaking.rmblr.ui.m3.RmblrSnackbarHost
import io.github.pastdaking.rmblr.ui.m3.Status
import io.github.pastdaking.rmblr.ui.m3.StatusChip
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalIconButton
import io.github.pastdaking.rmblr.ui.m3.formatElapsed
import io.github.pastdaking.rmblr.ui.theme.AccentText
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.launch

/** Wires the Result screen to the rewrite path, the clipboard and the share sheet. */
@Composable
fun ResultRoute(
    item: DictationHistoryItem,
    session: HomeSession,
    prefs: PreferencesManager,
    tones: List<Tone>,
    onNewTone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var rewriting by remember { mutableStateOf(false) }
    var activeToneId by remember(item.id) { mutableStateOf<String?>(null) }
    var languageLabel by remember(item.id) { mutableStateOf<String?>(null) }

    ResultContent(
        item = item,
        tones = tones,
        activeToneId = activeToneId ?: if (item.preset != CleanupPreset.CUSTOM) item.preset.name else null,
        rewriting = rewriting,
        languageLabel = languageLabel,
        snackbar = snack,
        onBack = { session.reset() },
        onTone = { tone ->
            if (rewriting) return@ResultContent
            rewriting = true
            scope.launch {
                // Rewrites what was SAID, not what came back, so tones never compound.
                val result = GeminiApiClient.postProcessText(
                    inputText = item.rawText.ifBlank { item.cleanedText },
                    apiKey = prefs.getEffectiveApiKey(),
                    preset = CleanupPreset.CUSTOM,
                    customPrompt = tone.prompt
                )
                rewriting = false
                result.onSuccess { text ->
                    if (text.isNotBlank()) {
                        activeToneId = tone.id
                        session.replaceResult(item.copy(cleanedText = text, preset = presetOf(tone)))
                    }
                }.onFailure { snack.showSnackbar(it.message ?: "Could not rewrite that.") }
            }
        },
        onNewTone = onNewTone,
        onCopy = {
            copyToClipboard(context, "RMBLR dictation", item.cleanedText)
            scope.launch { snack.showSnackbar("Copied to clipboard") }
        },
        onShare = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.cleanedText)
            }
            runCatching {
                context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        },
        onTranslate = {
            if (rewriting) return@ResultContent
            rewriting = true
            val target = prefs.getTranslateTarget()
            scope.launch {
                val before = item
                val result = Translator.translate(item.cleanedText, prefs)
                rewriting = false
                result.onSuccess { text ->
                    languageLabel = target
                    session.replaceResult(item.copy(cleanedText = text))
                    val r = snack.showSnackbar("Translated to $target", actionLabel = "Undo")
                    if (r == SnackbarResult.ActionPerformed) {
                        languageLabel = null
                        session.replaceResult(before)
                    }
                }.onFailure { snack.showSnackbar(it.message ?: "Could not translate that.") }
            }
        },
        modifier = modifier
    )
}

/** The result of a dictation: tidied text, the raw words on demand, tones to rewrite it, and actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultContent(
    item: DictationHistoryItem,
    tones: List<Tone>,
    activeToneId: String?,
    rewriting: Boolean,
    languageLabel: String?,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onTone: (Tone) -> Unit,
    onNewTone: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onTranslate: () -> Unit,
    modifier: Modifier = Modifier,
    startWithRawOpen: Boolean = false
) {
    var rawOpen by remember { mutableStateOf(startWithRawOpen) }
    Box(modifier = modifier.fillMaxSize().background(Ink).testTag("result_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg)
                .padding(top = Space.md, bottom = 96.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TonalIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, container = Ink)
                Text(
                    "Result", color = TextHigh, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(Space.md))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(if (rewriting) "Working" else "Done", if (rewriting) Status.WORKING else Status.DONE)
                if (item.durationSeconds > 0) {
                    InfoChip(Icons.Default.Schedule, formatElapsed(item.durationSeconds))
                }
                if (languageLabel != null) InfoChip(Icons.Default.Translate, languageLabel)
            }
            Spacer(Modifier.height(Space.md))

            TonalCard(
                container = RaisedHigh,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Eyebrow(shortToneName(item.preset))
                Spacer(Modifier.height(12.dp))
                Text(
                    item.cleanedText,
                    color = if (rewriting) TextMid else TextHigh,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                )
                if (item.rawText.isNotBlank() && item.rawText != item.cleanedText) {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(pressedScale = 0.99f) { rawOpen = !rawOpen }
                            .padding(top = 12.dp)
                    ) {
                        Icon(Icons.Default.Undo, null, tint = AccentText, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "was: raw transcript", color = AccentText,
                            style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (rawOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (rawOpen) "Hide raw transcript" else "Show raw transcript",
                            tint = AccentText
                        )
                    }
                }
            }
            AnimatedVisibility(visible = rawOpen) {
                val shape = RoundedCornerShape(20.dp)
                Text(
                    item.rawText,
                    color = TextMid,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .clip(shape)
                        .border(1.dp, Line, shape)
                        .padding(16.dp)
                )
            }

            Spacer(Modifier.height(Space.xl))
            Eyebrow("Write it as")
            Spacer(Modifier.height(Space.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tones.forEach { tone ->
                    M3Chip(
                        toneShortName(tone),
                        selected = tone.id == activeToneId,
                        onClick = { onTone(tone) },
                        leadingIcon = toneIcon(presetOf(tone))
                    )
                }
                M3Chip("New", selected = false, onClick = onNewTone, leadingIcon = Icons.Default.Add)
            }

            Spacer(Modifier.height(Space.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                ActionPill("Copy", Icons.Default.ContentCopy, onCopy, Modifier.weight(1f), 28.dp, 8.dp)
                ActionPill("Share", Icons.Default.Share, onShare, Modifier.weight(1f), 8.dp, 8.dp)
                ActionPill("Translate", Icons.Default.Translate, onTranslate, Modifier.weight(1f), 8.dp, 28.dp)
            }
        }
        RmblrSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RaisedHigh)
            .padding(horizontal = 12.dp)
    ) {
        Icon(icon, null, tint = TextHigh, modifier = Modifier.size(16.dp))
        Text(label, color = TextHigh, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    startCorner: androidx.compose.ui.unit.Dp,
    endCorner: androidx.compose.ui.unit.Dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(startCorner, endCorner, endCorner, startCorner))
            .background(AccentWash)
    ) {
        Icon(icon, null, tint = OnAccentWash, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = OnAccentWash, style = MaterialTheme.typography.labelLarge)
    }
}
