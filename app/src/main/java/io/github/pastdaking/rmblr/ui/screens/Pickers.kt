package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.LanguageSelection
import io.github.pastdaking.rmblr.data.SPOKEN_LANGUAGES
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.SheetButton
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentText
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.SecondaryWash
import io.github.pastdaking.rmblr.ui.theme.OnSecondaryWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid

/** The built-in tone a [Tone] came from, or CUSTOM for the user's own. */
fun presetOf(tone: Tone): CleanupPreset =
    CleanupPreset.values().firstOrNull { it.name == tone.id && it != CleanupPreset.CUSTOM }
        ?: CleanupPreset.CUSTOM

/** A short, friendly line for each built-in tone (the stored description is longer). */
fun toneBlurb(tone: Tone): String = when (tone.id) {
    "SMART_CLEAN" -> "Removes filler words and false starts"
    "FORMAL_EMAIL" -> "Polite, concise, ready to send"
    "CASUAL_CHAT" -> "Relaxed, for chats and messages"
    "BULLET_POINTS" -> "Key points as a short list"
    "FIX_GRAMMAR" -> "Spelling and punctuation only"
    "CONCISE_SUMMARY" -> "One or two sentences"
    else -> tone.description
}

/** Short display name, matching the chips ("Clean up" rather than "Clean & Smooth"). */
fun toneShortName(tone: Tone): String {
    val p = presetOf(tone)
    return if (p == CleanupPreset.CUSTOM) tone.name else shortToneName(p)
}

/**
 * Pick a tone. Built-ins first, then the user's own under "Yours", then "New tone".
 * [selectedId] is the tone's id; [onNewTone] opens whatever creates one (may be null).
 */
@Composable
fun ToneSheet(
    tones: List<Tone>,
    selectedId: String?,
    onPick: (Tone) -> Unit,
    onNewTone: (() -> Unit)?,
    onDismiss: () -> Unit,
    subtitle: String = "How your words are written back"
) {
    RmblrSheet("Pick a tone", onDismiss) {
        Text(subtitle, color = TextMid, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(Space.md))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())
        ) {
            val builtIn = tones.filter { it.builtIn }
            val yours = tones.filterNot { it.builtIn }
            builtIn.forEach { ToneRow(it, it.id == selectedId) { onPick(it) } }
            if (yours.isNotEmpty()) {
                Text(
                    "YOURS",
                    color = TextMid,
                    style = io.github.pastdaking.rmblr.ui.theme.EyebrowStyle,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
                yours.forEach { ToneRow(it, it.id == selectedId) { onPick(it) } }
            }
            if (onNewTone != null) {
                Spacer(Modifier.height(8.dp))
                NewToneRow(onNewTone)
            }
        }
    }
}

@Composable
private fun ToneRow(tone: Tone, selected: Boolean, onClick: () -> Unit) {
    val preset = presetOf(tone)
    val shape = RoundedCornerShape(if (selected) 24.dp else 12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressable(pressedScale = 0.985f, onClick = onClick)
            .clip(shape)
            .background(if (selected) AccentWash else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(if (selected) 20.dp else 12.dp))
                .background(if (selected) Accent else RaisedHigh)
        ) {
            Icon(
                toneIcon(preset), null,
                tint = if (selected) Color.White else TextHigh,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                toneShortName(tone),
                color = if (selected) OnAccentWash else TextHigh,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                toneBlurb(tone),
                color = if (selected) OnAccentWash.copy(alpha = 0.8f) else TextMid,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(OnAccentWash)
            ) { Icon(Icons.Default.Check, null, tint = AccentWash, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun NewToneRow(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressable(pressedScale = 0.985f, onClick = onClick)
            .clip(shape)
            .border(1.dp, Line, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(AccentWash)
        ) { Icon(Icons.Default.Add, null, tint = OnAccentWash, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(16.dp))
        Text("New tone", color = TextHigh, style = MaterialTheme.typography.titleMedium)
    }
}

/** Create a tone: a name and the instruction that rewrites your words. */
@Composable
fun NewToneSheet(onSave: (name: String, prompt: String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    RmblrSheet("New tone", onDismiss) {
        TonalTextField(name, { name = it }, placeholder = "Name, e.g. LinkedIn post", leadingIcon = Icons.Default.EditNote)
        Spacer(Modifier.height(Space.md))
        TonalTextField(
            prompt, { prompt = it },
            placeholder = "How should it be written?",
            minLines = 4, maxLines = 8
        )
        Spacer(Modifier.height(Space.lg))
        val ok = name.isNotBlank() && prompt.isNotBlank()
        SheetButton(
            "Save tone",
            onClick = { if (ok) onSave(name, prompt) },
            container = if (ok) Accent else RaisedHigh,
            content = if (ok) Color.White else TextLow,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Every language the sheet offers, alphabetical, without the legacy "Auto" entry. */
val PICKABLE_LANGUAGES: List<String> =
    SPOKEN_LANGUAGES.filter { it.code.isNotBlank() }.map { it.label }.sorted()

/** The names shown for a search: matches first, plus the typed text as its own entry if new. */
fun languageMatches(query: String, selected: List<String>): List<String> {
    val q = query.trim()
    val pool = (PICKABLE_LANGUAGES + selected.filterNot { s -> PICKABLE_LANGUAGES.any { it.equals(s, true) } }).sorted()
    if (q.isEmpty()) return pool
    val hits = pool.filter { it.contains(q, ignoreCase = true) }
    return if (pool.any { it.equals(q, ignoreCase = true) }) hits else hits + q
}

/**
 * Language picker. Auto-detect works the language out; picking languages with it on makes
 * them hints, picking several with it off is mixed speech. Any language can be typed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSheet(
    selection: LanguageSelection,
    onChange: (LanguageSelection) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    RmblrSheet("Languages", onDismiss) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(RaisedHigh)
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Accent)
            ) { Icon(Icons.Default.Language, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Auto-detect", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Works out the language as you speak",
                    color = TextMid, style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = selection.auto,
                onCheckedChange = { onChange(selection.withAuto(it)) },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Accent, checkedThumbColor = Color.White,
                    uncheckedTrackColor = RaisedHigh, uncheckedThumbColor = TextMid
                )
            )
        }
        Spacer(Modifier.height(Space.md))
        Text(
            "Mixing is supported. Pick every language you switch between and RMBLR keeps each in its own script.",
            color = TextMid, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(Space.md))
        TonalTextField(
            query, { query = it },
            placeholder = "Search or type a language",
            leadingIcon = Icons.Default.Search
        )
        if (selection.names.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selection.names.forEach { n ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .pressable { onChange(selection.toggle(n)) }
                            .clip(RoundedCornerShape(18.dp))
                            .background(SecondaryWash)
                            .padding(start = 14.dp, end = 10.dp)
                    ) {
                        Text(n, color = OnSecondaryWash, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Close, "Remove $n", tint = OnSecondaryWash, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Column(
            modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())
        ) {
            languageMatches(query, selection.names).forEach { name ->
                val on = selection.names.any { it.equals(name, true) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .pressable(pressedScale = 0.985f) {
                            onChange(selection.toggle(name)); query = ""
                        }
                        .clip(RoundedCornerShape(if (on) 16.dp else 12.dp))
                        .background(if (on) AccentWash else Color.Transparent)
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (on) AccentText else Color.Transparent)
                            .then(if (on) Modifier else Modifier.border(2.dp, TextLow, RoundedCornerShape(7.dp)))
                    ) {
                        if (on) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(name, color = if (on) OnAccentWash else TextHigh, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        SheetButton("Done", onDismiss, Accent, Color.White, Modifier.fillMaxWidth())
    }
}

/** What the language chip on the recording screen says. */
fun languageChipLabel(selection: LanguageSelection): String = when {
    selection.names.isEmpty() -> "Auto"
    selection.auto -> "Auto + ${selection.names.size}"
    selection.names.size == 1 -> selection.names[0]
    else -> selection.names.joinToString(", ")
}
