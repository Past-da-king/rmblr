package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.m3.AssistChip
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.LiveTranscript
import io.github.pastdaking.rmblr.ui.m3.RecordButton
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalIconButton
import io.github.pastdaking.rmblr.ui.m3.WaveformBars
import io.github.pastdaking.rmblr.ui.m3.formatElapsed
import io.github.pastdaking.rmblr.ui.m3.splitLive
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.RecRed
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import io.github.pastdaking.rmblr.ui.theme.TimerStyle

/**
 * The live recording screen. Settled words are solid and the engine's current guess is
 * lighter with a caret. On an engine that does not stream, the card says so plainly and
 * the words arrive when you stop.
 */
@Composable
fun RecordingContent(
    working: Boolean,
    elapsedSeconds: Int,
    toneName: String,
    languageLabel: String,
    levels: List<Float>,
    liveText: String,
    streaming: Boolean,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onTone: () -> Unit,
    onLanguage: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = Space.lg)
            .padding(top = Space.md, bottom = Space.xl)
            .testTag("recording_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TonalIconButton(Icons.Default.Close, "Discard", onCancel, container = Ink)
            Text(
                if (working) "Writing it up" else "Listening",
                color = TextHigh,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            AssistChip(
                languageLabel, onClick = onLanguage, icon = Icons.Default.Translate,
                container = AccentWash, content = OnAccentWash
            )
        }
        Spacer(Modifier.height(Space.lg))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(RecRed))
                Spacer(Modifier.width(12.dp))
                Text(formatElapsed(elapsedSeconds), color = TextHigh, style = TimerStyle)
            }
            Eyebrow("Tone: $toneName", modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(Space.xl))
        WaveformBars(levels, color = io.github.pastdaking.rmblr.ui.theme.Accent, height = 96.dp)
        Spacer(Modifier.height(Space.xl))

        TonalCard(
            modifier = Modifier.weight(1f, fill = false).heightIn(min = 160.dp),
            padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Eyebrow("Live transcript")
            Spacer(Modifier.height(12.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val words = splitLive(liveText)
                if (liveText.isBlank()) {
                    Text(
                        if (streaming) "Start talking. Your words show up here as you say them."
                        else "Say what you want to write. Your words appear when you stop.",
                        color = TextMid,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    LiveTranscript(words)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TonalIconButton(Icons.Default.Close, "Discard recording", onCancel, size = 48.dp, container = AccentWash, tint = OnAccentWash)
                Spacer(Modifier.size(1.dp))
                TonalIconButton(Icons.Default.AutoFixHigh, "Change tone", onTone, size = 48.dp, container = AccentWash, tint = OnAccentWash)
            }
            RecordButton(
                recording = true,
                onClick = { if (!working) onStop() },
                level = levels.lastOrNull() ?: 0f,
                size = 104.dp,
                animate = animate
            )
        }
        Text(
            if (working) "Writing it up" else "Tap to stop and write",
            color = TextMid,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
