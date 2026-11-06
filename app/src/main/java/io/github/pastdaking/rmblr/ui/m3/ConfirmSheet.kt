package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.RecRed
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid

/**
 * The only way to confirm something on the phone: a slide-up sheet, never a centred
 * dialog. [destructive] makes the confirm button the one red element on screen.
 */
@Composable
fun ConfirmSheet(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = "Cancel",
    destructive: Boolean = false,
    icon: ImageVector? = null
) {
    RmblrSheet(title = "", onDismiss = onDismiss) {
        Column(modifier = modifier.fillMaxWidth()) {
            if (icon != null) {
                Icon(icon, null, tint = if (destructive) Alert else TextHigh, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(Space.md))
            }
            Text(title, color = TextHigh, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Space.sm))
            Text(body, color = TextMid, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(Space.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), modifier = Modifier.fillMaxWidth()) {
                SheetButton(cancelLabel, onDismiss, RaisedHigh, TextHigh, Modifier.weight(1f))
                SheetButton(
                    confirmLabel, onConfirm,
                    if (destructive) RecRed else io.github.pastdaking.rmblr.ui.theme.Accent,
                    Color.White, Modifier.weight(1f)
                )
            }
        }
    }
}

/** A 56dp pill button for the foot of a sheet. */
@Composable
fun SheetButton(
    label: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 56.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(28.dp))
            .background(container)
    ) {
        Text(label, color = content, style = MaterialTheme.typography.titleMedium)
    }
}
