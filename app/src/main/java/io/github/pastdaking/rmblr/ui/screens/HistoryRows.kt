package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.ui.m3.ExpressiveShapes
import io.github.pastdaking.rmblr.ui.m3.GroupedRow
import io.github.pastdaking.rmblr.ui.m3.RadialShape
import io.github.pastdaking.rmblr.ui.m3.ToneTag
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.GoodWash
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.OnGoodWash
import io.github.pastdaking.rmblr.ui.theme.OnSecondaryWash
import io.github.pastdaking.rmblr.ui.theme.TealWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import io.github.pastdaking.rmblr.ui.theme.Teal
import io.github.pastdaking.rmblr.ui.m3.formatElapsed

/** A 40dp expressive-shape avatar telling you what kind of writing a dictation was. */
@Composable
fun KindAvatar(preset: CleanupPreset, modifier: Modifier = Modifier) {
    val kind = categoryOf(preset)
    val (shape, bg, fg, icon) = when (kind) {
        HistoryFilter.CHAT -> Quad(ExpressiveShapes.Flower, AccentWash, OnAccentWash, Icons.Default.Chat)
        HistoryFilter.MAIL -> Quad(ExpressiveShapes.Clover4, TealWash, OnSecondaryWash, Icons.Default.Mail)
        else -> Quad(ExpressiveShapes.SoftBurst, GoodWash, OnGoodWash, Icons.Default.StickyNote2)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(40.dp).background(bg, RadialShape(shape))
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/**
 * One dictation in a list: avatar, two lines of the tidied text, time and length, and the
 * tone tag. Corners follow its place in the group.
 */
@Composable
fun HistoryRow(
    item: DictationHistoryItem,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GroupedRow(index, count, modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            KindAvatar(item.preset)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.cleanedText.ifBlank { item.rawText },
                    color = TextHigh,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        clockLabel(item.timestamp) + "  •  " + formatElapsed(item.durationSeconds),
                        color = TextMid,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    ToneTag(shortToneName(item.preset), icon = toneIcon(item.preset))
                }
            }
        }
    }
}
