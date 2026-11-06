package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.LanguageRules
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.SUGGESTED_LANGUAGES
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.AssistChip
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.GroupedRow
import io.github.pastdaking.rmblr.ui.m3.M3Chip
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.OnSecondaryWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.SecondaryWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid

private enum class LanguageSheet { ADD_HINT, SINGLE, WRITE_IN, TRANSLATE_TO }

/**
 * The Languages part of Settings: say what you speak, or leave it to the model.
 *
 * Auto-detect is the default and the recommendation. Naming extra languages while it is on
 * does not narrow anything; it only tells the model which ones are likely to turn up, which
 * helps when two of them sound alike. Turn auto-detect off and it becomes one named
 * language, the way the old single field worked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguagesSection(prefs: PreferencesManager, modifier: Modifier = Modifier) {
    val state by prefs.languageStateFlow.collectAsState()
    val writeIn by prefs.dictationLanguageFlow.collectAsState()
    val target by prefs.translateTargetFlow.collectAsState()
    var sheet by remember { mutableStateOf<LanguageSheet?>(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        TonalCard(padding = PaddingValues(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.pressable(pressedScale = 0.99f) {
                prefs.setLanguageState(state.copy(auto = !state.auto))
            }) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-detect", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Works out the language, or a mix of them, as you speak",
                        color = TextMid,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.width(Space.md))
                TickSwitch(state.auto, { prefs.setLanguageState(state.copy(auto = it)) }, Modifier.testTag("switch_auto_detect"))
            }

            Spacer(Modifier.height(Space.md))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(Space.md))

            if (state.auto) {
                Text("Also expect", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    state.hints.forEach { name ->
                        RemovableChip(name, onRemove = { prefs.setLanguageState(LanguageRules.withoutHint(state, name)) })
                    }
                    if (state.hints.size < LanguageRules.MAX_HINTS) {
                        AssistChip(
                            label = "Add language",
                            icon = Icons.Default.Add,
                            onClick = { sheet = LanguageSheet.ADD_HINT },
                            modifier = Modifier.testTag("add_language")
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().pressable(pressedScale = 0.99f) { sheet = LanguageSheet.SINGLE }
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Language you speak", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.single.ifBlank { "Choose one" },
                            color = TextMid,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextMid)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LanguageRow(0, 2, "Write dictation in", writeIn.ifBlank { "Same as I spoke" }) { sheet = LanguageSheet.WRITE_IN }
            LanguageRow(1, 2, "Translate into", target.ifBlank { "English" }) { sheet = LanguageSheet.TRANSLATE_TO }
        }
    }

    when (sheet) {
        LanguageSheet.ADD_HINT -> LanguagePickerSheet(
            title = "Also expect",
            explanation = "Tell the model which other languages are likely to turn up. It still detects each word for itself.",
            current = state.hints,
            allowBlank = false,
            onPick = { prefs.setLanguageState(LanguageRules.withHint(state, it)) },
            onDismiss = { sheet = null },
            closeOnPick = false
        )
        LanguageSheet.SINGLE -> LanguagePickerSheet(
            title = "Language you speak",
            explanation = "Type any language. It is only a name passed to the model, so it does not have to be on a list.",
            current = listOf(state.single),
            allowBlank = false,
            onPick = { prefs.setLanguageState(state.copy(single = it)) },
            onDismiss = { sheet = null }
        )
        LanguageSheet.WRITE_IN -> LanguagePickerSheet(
            title = "Write dictation in",
            explanation = "Speak in one language and have it typed in another. Leave it on the first choice and your words go in exactly as spoken.",
            current = listOf(writeIn),
            allowBlank = true,
            blankLabel = "Same as I spoke",
            onPick = { prefs.setDictationLanguage(it) },
            onDismiss = { sheet = null }
        )
        LanguageSheet.TRANSLATE_TO -> LanguagePickerSheet(
            title = "Translate into",
            explanation = "Where translations land when you translate with the orb.",
            current = listOf(target),
            allowBlank = false,
            onPick = { prefs.setTranslateTarget(it) },
            onDismiss = { sheet = null }
        )
        null -> Unit
    }
}

@Composable
private fun LanguageRow(index: Int, count: Int, label: String, value: String, onClick: () -> Unit) {
    GroupedRow(index, count, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Text(value, color = TextMid, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMid)
        }
    }
}

/** A language the person added, with a cross to take it off again. */
@Composable
fun RemovableChip(label: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SecondaryWash)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Text(label, color = OnSecondaryWash, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(2.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp)).pressable(onClick = onRemove)
        ) {
            Icon(Icons.Default.Close, "Remove $label", tint = OnSecondaryWash, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Pick a language: type any name, or tap a suggestion.
 *
 * Used for the four language decisions in Settings. [closeOnPick] stays false where it
 * makes sense to add several in one go.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun LanguagePickerSheet(
    title: String,
    explanation: String,
    current: List<String>,
    allowBlank: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    blankLabel: String = "None",
    closeOnPick: Boolean = true
) {
    var draft by remember { mutableStateOf("") }
    val commit = {
        val cleaned = draft.trim()
        if (cleaned.isNotEmpty()) {
            onPick(cleaned)
            draft = ""
            if (closeOnPick) onDismiss()
        }
    }
    val shown = if (draft.isBlank()) SUGGESTED_LANGUAGES else SUGGESTED_LANGUAGES.filter { it.contains(draft.trim(), ignoreCase = true) }

    RmblrSheet(title = title, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Text(explanation, color = TextMid, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(Space.lg))
            TonalTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Type a language",
                leadingIcon = Icons.Default.Search,
                imeAction = ImeAction.Done,
                onImeAction = { commit() },
                modifier = Modifier.fillMaxWidth().testTag("language_input")
            )
            Spacer(Modifier.height(Space.lg))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                if (allowBlank) {
                    M3Chip(
                        label = blankLabel,
                        selected = current.all { it.isBlank() },
                        onClick = { onPick(""); if (closeOnPick) onDismiss() }
                    )
                }
                if (draft.isNotBlank() && shown.none { it.equals(draft.trim(), ignoreCase = true) }) {
                    AssistChip(label = "Use “${draft.trim()}”", icon = Icons.Default.Add, onClick = commit)
                }
                shown.forEach { name ->
                    M3Chip(
                        label = name,
                        selected = current.any { it.equals(name, ignoreCase = true) },
                        onClick = { onPick(name); if (closeOnPick) onDismiss() }
                    )
                }
            }
        }
    }
}

/** Removable chips and a field to add one: the arc languages, a short list you can see whole. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsEditor(
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    max: Int = 5
) {
    var draft by remember { mutableStateOf("") }
    val commit = {
        val cleaned = draft.trim()
        if (cleaned.isNotEmpty() && items.none { it.equals(cleaned, ignoreCase = true) }) {
            onItemsChange((items + cleaned).take(max))
        }
        draft = ""
    }
    Column(modifier.fillMaxWidth()) {
        if (items.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                items.forEach { item -> RemovableChip(item, onRemove = { onItemsChange(items - item) }) }
            }
            Spacer(Modifier.height(Space.md))
        }
        if (items.size < max) {
            TonalTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = placeholder,
                imeAction = ImeAction.Done,
                onImeAction = { commit() },
                trailing = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = TextMid,
                        modifier = Modifier.size(24.dp).pressable(onClick = { commit() })
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
