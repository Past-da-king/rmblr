package io.github.pastdaking.rmblr.ui.components

import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import io.github.pastdaking.rmblr.ui.theme.AccentText
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid

/**
 * A settings row: what it is on the left, what it is currently set to on the right.
 *
 * This replaced pages built out of stacked option cards, which do not survive contact
 * with a growing list — six transcription engines already filled a phone screen, and the
 * answer to "what if there are a hundred providers" cannot be "then the page is a
 * hundred cards long". A row states the current answer in one line and hides the other
 * ninety-nine until you ask for them.
 */
@Composable
fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    valueTint: androidx.compose.ui.graphics.Color = Accent
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.panel))
            .background(Surface)
            .padding(Space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = TextHigh, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Space.md))
            Text(value, color = valueTint, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(Space.xs))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextLow, modifier = Modifier.size(18.dp))
        }
        if (supporting != null) {
            Spacer(Modifier.height(Space.xs))
            Text(supporting, color = TextMid, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * The one sheet shape in the app.
 *
 * Slides up from the bottom rather than appearing in the middle: a sheet keeps the page
 * you came from visible behind it, and your thumb is already at the bottom of the phone.
 * Centre-screen dialogs do neither.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RmblrSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Surface,
        contentColor = TextHigh,
        scrimColor = androidx.compose.ui.graphics.Color(0x66000000),
        dragHandle = {
            // Slim 32 x 4 grab bar, as drawn in the design.
            Box(
                modifier = Modifier
                    .padding(top = Space.md, bottom = Space.sm)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextLow.copy(alpha = 0.6f))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.lg)
                .padding(bottom = Space.xxl)
        ) {
            if (title.isNotBlank()) {
                Text(title, color = TextHigh, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Space.lg))
            }
            content()
        }
    }
}

/**
 * One choice inside a sheet: 12dp row, and the selected one morphs to a 24dp pill in
 * primary container with a check.
 */
@Composable
fun SheetOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    supporting: String? = null
) {
    val corner by androidx.compose.animation.core.animateFloatAsState(
        if (selected) 24f else 12f,
        androidx.compose.animation.core.spring(0.8f, 380f),
        label = "sheet_option"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressable(pressedScale = 0.985f, onClick = onClick)
            .clip(RoundedCornerShape(corner.dp))
            .background(if (selected) AccentWash else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = Space.lg, vertical = Space.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (selected) OnAccentWash else TextHigh,
                style = MaterialTheme.typography.titleMedium
            )
            if (supporting != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    supporting,
                    color = if (selected) OnAccentWash.copy(alpha = 0.8f) else TextMid,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.width(Space.lg))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) AccentText else androidx.compose.ui.graphics.Color.Transparent)
                .then(if (selected) Modifier else Modifier.border(2.dp, TextLow, RoundedCornerShape(12.dp)))
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** A scrolling column of options, capped so a long list cannot take the whole screen. */
@Composable
fun SheetOptions(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
    ) { content() }
}

/**
 * Type a language in rather than hunt for it.
 *
 * A horizontal strip of chips is fine for the five languages whoever built it happens to
 * speak and useless for the sixth. A language is only ever a name handed to the model, so
 * the field accepts anything; the suggestions underneath are a shortcut, not the list.
 */
@Composable
fun LanguageEntry(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    placeholder: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.control),
            singleLine = true,
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
            placeholder = { Text(placeholder, color = TextLow, style = MaterialTheme.typography.bodyMedium) }
        )

        Spacer(Modifier.height(Space.lg))

        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            suggestions.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { name ->
                        Text(
                            text = name,
                            color = if (value.equals(name, ignoreCase = true)) Accent else TextMid,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .weight(1f)
                                .pressable(onClick = { onValueChange(name) })
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(if (value.equals(name, ignoreCase = true)) AccentWash else Raised)
                                .padding(horizontal = Space.lg, vertical = Space.md)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * A list you build out of chips rather than out of a paragraph.
 *
 * The first version of this was a multi-line text field with one item per line, which is
 * a developer's idea of a list: to remove the third language you had to select exactly
 * the right run of characters and the newline with it. A chip is a thing you can see the
 * edges of, and an X on it removes exactly one.
 */
@Composable
fun ChipEditor(
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

    Column(modifier = modifier.fillMaxWidth()) {
        if (items.isNotEmpty()) {
            // Two to a row rather than a horizontal scroller: a list you are editing has
            // to show all of itself, or you delete the wrong one.
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                items.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(Radius.pill))
                                    .background(AccentWash)
                                    .padding(start = Space.lg, end = Space.sm, top = Space.sm, bottom = Space.sm)
                            ) {
                                Text(
                                    text = item,
                                    color = Accent,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $item",
                                    tint = Accent,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .pressable(onClick = { onItemsChange(items - item) })
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(Space.lg))
        }

        if (items.size < max) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Radius.control),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Line,
                        focusedTextColor = TextHigh,
                        unfocusedTextColor = TextHigh,
                        cursorColor = Accent,
                        focusedContainerColor = Raised,
                        unfocusedContainerColor = Raised
                    ),
                    placeholder = { Text(placeholder, color = TextLow, style = MaterialTheme.typography.bodyMedium) }
                )

                Spacer(Modifier.width(Space.sm))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .pressable(enabled = draft.isNotBlank(), onClick = commit)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(if (draft.isNotBlank()) Accent else Raised)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = if (draft.isNotBlank()) Ink else TextLow,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
