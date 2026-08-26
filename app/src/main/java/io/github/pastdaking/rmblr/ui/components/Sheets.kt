package io.github.pastdaking.rmblr.ui.components

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
        containerColor = Ink,
        contentColor = TextHigh,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl)
                .padding(top = Space.lg, bottom = Space.xxl)
        ) {
            // A short bar instead of a dialog title bar: it says "drag me" without
            // spending a whole row on a heading and a close button.
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(Line)
                        .padding(vertical = 2.dp)
                ) {}
            }

            Spacer(Modifier.height(Space.lg))
            Text(title, color = TextHigh, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.lg))
            content()
        }
    }
}

/** One choice inside a sheet. */
@Composable
fun SheetOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    supporting: String? = null
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.panel))
            .background(if (selected) AccentWash else Surface)
            .padding(Space.lg)
    ) {
        Column(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) Accent else Raised),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(Space.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (selected) Accent else TextHigh,
                style = MaterialTheme.typography.titleMedium
            )
            if (supporting != null) {
                Spacer(Modifier.height(2.dp))
                Text(supporting, color = TextMid, style = MaterialTheme.typography.bodySmall)
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
