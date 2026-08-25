package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import io.github.pastdaking.rmblr.data.DictionaryEntry
import io.github.pastdaking.rmblr.data.DictionaryRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.SnippetItem
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.Radius
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyRepo: HistoryRepository,
    modifier: Modifier = Modifier
) {
    val historyItems by historyRepo.historyFlow.collectAsState()
    val snippetItems by historyRepo.snippetsFlow.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val dictionary = remember { DictionaryRepository.getInstance(context) }
    val words by dictionary.entries.collectAsState()

    // Recomputed only when one of its inputs actually changes: mining every dictation
    // on each recomposition would be wasteful for a list that barely moves.
    val suggestions = remember(historyItems, words) {
        DictionaryRepository.suggestionsFrom(historyItems, words)
    }
    var newWord by remember { mutableStateOf("") }
    var newSpelling by remember { mutableStateOf("") }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSnippet by remember { mutableStateOf(false) }

    var newSnippetTitle by remember { mutableStateOf("") }
    var newSnippetContent by remember { mutableStateOf("") }
    var newSnippetShortcut by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = Space.xl)
            .padding(top = Space.xl)
            .testTag("history_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("History", color = TextHigh, style = MaterialTheme.typography.headlineMedium)

            if (selectedTab == 0 && historyItems.isNotEmpty()) {
                IconButton(onClick = { historyRepo.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear history", tint = TextMid)
                }
            } else if (selectedTab == 2) {
                IconButton(onClick = { showAddSnippet = !showAddSnippet }) {
                    Icon(Icons.Default.Add, contentDescription = "New snippet", tint = Accent)
                }
            }
        }

        Spacer(Modifier.height(Space.lg))

        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            SegmentTab("Dictations", historyItems.size, selectedTab == 0) { selectedTab = 0 }
            SegmentTab("Dictionary", words.size, selectedTab == 1) { selectedTab = 1 }
            SegmentTab("Snippets", snippetItems.size, selectedTab == 2) { selectedTab = 2 }
        }

        Spacer(Modifier.height(Space.lg))

        if (selectedTab == 0) {
            if (historyItems.isEmpty()) {
                EmptyState(
                    headline = "Nothing dictated yet",
                    body = "Tap a text box in any app, tap the orb, and say something. It lands here."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Space.xxl),
                    modifier = Modifier.weight(1f)
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            onCopy = { text -> clipboardManager.setText(AnnotatedString(text)) },
                            onDelete = { historyRepo.deleteHistoryItem(item.id) }
                        )
                    }
                }
            }
        } else if (selectedTab == 1) {
            DictionaryTab(
                words = words,
                suggestions = suggestions,
                newWord = newWord,
                newSpelling = newSpelling,
                onNewWord = { newWord = it },
                onNewSpelling = { newSpelling = it },
                onAdd = {
                    if (dictionary.add(newWord, newSpelling)) {
                        newWord = ""
                        newSpelling = ""
                    }
                },
                onAddSuggestion = { dictionary.add(it) },
                onDelete = { dictionary.delete(it) },
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedVisibility(visible = showAddSnippet) {
                    Column(modifier = Modifier.padding(bottom = Space.md)) {
                        Panel {
                            Text(
                                "New snippet",
                                color = TextHigh,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(Space.md))
                            SnippetField(newSnippetTitle, { newSnippetTitle = it }, "Name, e.g. Work email")
                            Spacer(Modifier.height(Space.sm))
                            SnippetField(newSnippetShortcut, { newSnippetShortcut = it }, "Shortcut, e.g. /work")
                            Spacer(Modifier.height(Space.sm))
                            SnippetField(
                                value = newSnippetContent,
                                onValueChange = { newSnippetContent = it },
                                placeholder = "What it types out",
                                height = 88.dp
                            )
                            Spacer(Modifier.height(Space.md))
                            PrimaryAction(
                                text = "Save snippet",
                                enabled = newSnippetTitle.isNotBlank() && newSnippetContent.isNotBlank(),
                                onClick = {
                                    historyRepo.addSnippet(
                                        SnippetItem(
                                            title = newSnippetTitle,
                                            content = newSnippetContent,
                                            shortcut = newSnippetShortcut
                                        )
                                    )
                                    newSnippetTitle = ""
                                    newSnippetContent = ""
                                    newSnippetShortcut = ""
                                    showAddSnippet = false
                                }
                            )
                        }
                    }
                }

                if (snippetItems.isEmpty() && !showAddSnippet) {
                    EmptyState(
                        headline = "No snippets",
                        body = "Save the lines you retype often and drop them in from the keyboard."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Space.sm),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Space.xxl),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(snippetItems, key = { it.id }) { snippet ->
                            Panel(padding = androidx.compose.foundation.layout.PaddingValues(Space.lg)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = snippet.title,
                                        color = TextHigh,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (snippet.shortcut.isNotBlank()) {
                                        Text(
                                            text = snippet.shortcut,
                                            color = Accent,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AccentWash)
                                                .padding(horizontal = Space.sm, vertical = 2.dp)
                                        )
                                        Spacer(Modifier.width(Space.sm))
                                    }
                                    IconButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(snippet.content)) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMid, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { historyRepo.deleteSnippet(snippet.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextLow, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    text = snippet.content,
                                    color = TextMid,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.control))
            .background(if (selected) AccentWash else Surface)
            .clickable { onClick() }
            .padding(horizontal = Space.lg, vertical = Space.md)
    ) {
        Text(
            text = label,
            color = if (selected) Accent else TextMid,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            text = count.toString(),
            color = if (selected) Accent else TextLow,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SnippetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    height: androidx.compose.ui.unit.Dp? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier),
        shape = RoundedCornerShape(Radius.control),
        singleLine = height == null,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Line,
            focusedTextColor = TextHigh,
            unfocusedTextColor = TextHigh,
            cursorColor = Accent,
            focusedContainerColor = Raised,
            unfocusedContainerColor = Raised
        ),
        placeholder = {
            Text(placeholder, color = TextLow, style = MaterialTheme.typography.bodyMedium)
        }
    )
}

@Composable
private fun EmptyState(headline: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.xxl)
    ) {
        Text(headline, color = TextHigh, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Space.xs))
        Text(body, color = TextMid, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun HistoryCard(
    item: DictationHistoryItem,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val dateStr = formatter.format(Date(item.timestamp))
    val label = if (item.mode == TranscriptionMode.POST_PROCESS_CLEANUP) item.preset.displayName else "Verbatim"

    Panel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, color = TextMid, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(Space.sm))
            Text(dateStr, color = TextLow, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { onCopy(item.cleanedText) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMid, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = TextLow, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(Space.sm))

        Text(
            text = item.cleanedText,
            color = TextHigh,
            style = MaterialTheme.typography.bodyLarge
        )

        if (item.rawText.isNotBlank() && item.rawText != item.cleanedText) {
            Spacer(Modifier.height(Space.md))
            Hairline()
            Spacer(Modifier.height(Space.md))
            Text("As spoken", color = TextLow, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(2.dp))
            Text(item.rawText, color = TextMid, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * The dictionary: words RMBLR should stop guessing at.
 *
 * The list suggests itself. Nobody sits down and types out their own vocabulary, so
 * anything already said more than once — or said once with a capital letter, which is
 * usually a name — is offered as a chip to tap. Typing one in by hand is still there for
 * the word you know it will get wrong before you have ever dictated it.
 */
@Composable
private fun DictionaryTab(
    words: List<DictionaryEntry>,
    suggestions: List<String>,
    newWord: String,
    newSpelling: String,
    onNewWord: (String) -> Unit,
    onNewSpelling: (String) -> Unit,
    onAdd: () -> Unit,
    onAddSuggestion: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Space.sm),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Space.xxl),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            Panel {
                Text(
                    "Names, places and jargon get guessed at, and differently every time. " +
                        "Anything in here is handed to the transcriber before it hears you.",
                    color = TextMid,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(Space.md))
                SnippetField(newWord, onNewWord, "Word, e.g. Ntencane")
                Spacer(Modifier.height(Space.sm))
                SnippetField(newSpelling, onNewSpelling, "Spell it like this (optional)")
                Spacer(Modifier.height(Space.md))
                PrimaryAction(
                    text = "Add word",
                    enabled = newWord.isNotBlank(),
                    onClick = onAdd
                )
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.md))
                Text(
                    "From what you have dictated",
                    color = TextLow,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(Space.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    suggestions.forEach { candidate ->
                        Text(
                            text = "+ $candidate",
                            color = Accent,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.control))
                                .background(AccentWash)
                                .clickable { onAddSuggestion(candidate) }
                                .padding(horizontal = Space.md, vertical = Space.sm)
                        )
                    }
                }
            }
        }

        if (words.isEmpty()) {
            item {
                Spacer(Modifier.height(Space.lg))
                EmptyState(
                    headline = "No words yet",
                    body = "Add the names and terms that keep coming out wrong. Once you have " +
                        "dictated a few times, suggestions appear above."
                )
            }
        }

        items(words, key = { it.id }) { entry ->
            Panel(padding = androidx.compose.foundation.layout.PaddingValues(Space.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.word,
                            color = TextHigh,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (entry.spelling.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "written as ${entry.spelling}",
                                color = TextMid,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    IconButton(onClick = { onDelete(entry.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = TextLow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
