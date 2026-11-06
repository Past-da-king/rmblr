package io.github.pastdaking.rmblr.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import io.github.pastdaking.rmblr.ui.m3.ConfirmSheet
import io.github.pastdaking.rmblr.ui.m3.GroupedRow
import io.github.pastdaking.rmblr.ui.m3.M3Chip
import io.github.pastdaking.rmblr.ui.m3.RmblrSnackbarHost
import io.github.pastdaking.rmblr.ui.m3.TonalIconButton
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import io.github.pastdaking.rmblr.data.DictionaryEntry
import io.github.pastdaking.rmblr.data.DictionaryRepository
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.runtime.rememberCoroutineScope
import io.github.pastdaking.rmblr.ai.GeminiApiClient
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.orb.FieldWatcherService
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.orb.ToneStore
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.SheetOption
import io.github.pastdaking.rmblr.ui.components.SheetOptions
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier,
    openItemId: String? = null,
    onOpenConsumed: () -> Unit = {}
) {
    val historyItems by historyRepo.historyFlow.collectAsState()
    val snippetItems by historyRepo.snippetsFlow.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    val dictionary = remember { DictionaryRepository.getInstance(context) }
    val words by dictionary.entries.collectAsState()

    // Recomputed only when one of its inputs actually changes: mining every dictation
    // on each recomposition would be wasteful for a list that barely moves.
    val suggestions = remember(historyItems, words) {
        DictionaryRepository.suggestionsFrom(historyItems, words)
    }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    // The Home screen can ask for a particular dictation to be opened.
    LaunchedEffect(openItemId) {
        if (openItemId != null) { detailId = openItemId; onOpenConsumed() }
    }

    var newWord by remember { mutableStateOf("") }
    var newSpelling by remember { mutableStateOf("") }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSnippet by remember { mutableStateOf(false) }

    var newSnippetTitle by remember { mutableStateOf("") }
    var newSnippetContent by remember { mutableStateOf("") }
    var newSnippetShortcut by remember { mutableStateOf("") }

    val detail = detailId?.let { id -> historyItems.firstOrNull { it.id == id } }
    if (detail != null) {
        BackHandler { detailId = null }
        HistoryDetail(
            item = detail,
            historyRepo = historyRepo,
            onBack = { detailId = null },
            onDeleted = { removed ->
                detailId = null
                scope.launch {
                    val r = snack.showSnackbar("Dictation deleted", actionLabel = "Undo")
                    if (r == SnackbarResult.ActionPerformed) historyRepo.restoreHistoryItem(removed)
                }
            },
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(Ink)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.lg)
            .padding(top = Space.lg)
            .testTag("history_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
        ) {
            Text("History", color = TextHigh, style = MaterialTheme.typography.displaySmall)

            if (selectedTab == 0 && historyItems.isNotEmpty()) {
                TonalIconButton(Icons.Default.DeleteSweep, "Clear history", { confirmClear = true })
            } else if (selectedTab == 2) {
                TonalIconButton(Icons.Default.Add, "New snippet", { showAddSnippet = !showAddSnippet })
            }
        }

        Spacer(Modifier.height(Space.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            M3Chip("Dictations ${historyItems.size}", selectedTab == 0, { selectedTab = 0 }, showCheck = false)
            M3Chip("Dictionary ${words.size}", selectedTab == 1, { selectedTab = 1 }, showCheck = false)
            M3Chip("Snippets ${snippetItems.size}", selectedTab == 2, { selectedTab = 2 }, showCheck = false)
        }

        Spacer(Modifier.height(Space.md))

        if (selectedTab == 0) {
            DictationsTab(
                items = historyItems,
                query = query,
                onQuery = { query = it },
                filter = filter,
                onFilter = { filter = it },
                onOpen = { detailId = it.id },
                onDelete = { item ->
                    historyRepo.deleteHistoryItem(item.id)
                    scope.launch {
                        val r = snack.showSnackbar("Dictation deleted", actionLabel = "Undo")
                        if (r == SnackbarResult.ActionPerformed) historyRepo.restoreHistoryItem(item)
                    }
                },
                modifier = Modifier.weight(1f)
            )
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
            val clipboardManager = LocalClipboardManager.current
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
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Space.navClear),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(snippetItems, key = { _, s -> s.id }) { index, snippet ->
                            GroupedRow(index, snippetItems.size) {
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
                                            color = OnAccentWash,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AccentWash)
                                                .padding(horizontal = Space.sm, vertical = 2.dp)
                                        )
                                        Spacer(Modifier.width(Space.sm))
                                    }
                                    IconButton(
                                        onClick = {
                                            // Straight into whatever has the cursor, if
                                            // the accessibility service is on.
                                            val typed = FieldWatcherService.instance
                                                ?.insertAtCursor(snippet.content) == true
                                            if (!typed) {
                                                clipboardManager.setText(AnnotatedString(snippet.content))
                                            }
                                        },
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
    RmblrSnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp))
    }

    if (confirmClear) {
        ConfirmSheet(
            title = "Clear all dictations?",
            body = "This removes all ${historyItems.size} dictations from this phone. It cannot be undone.",
            confirmLabel = "Clear all",
            destructive = true,
            icon = Icons.Default.DeleteSweep,
            onConfirm = { historyRepo.clearHistory(); confirmClear = false },
            onDismiss = { confirmClear = false }
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Space.navClear),
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
                                .clip(RoundedCornerShape(Radius.pill))
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
