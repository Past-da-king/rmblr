package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.Radial
import io.github.pastdaking.rmblr.ui.m3.RadialShape
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.RaisedHighest
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid

/**
 * Pieces the settings-side screens share: a glyph in an expressive shape, a section
 * header, a switch with the tick in its thumb, a slider with a handle you can find, and
 * the segmented chip the design uses for choices that live inside a card.
 */

/** An icon sitting in a soft-edged expressive shape (cookie, clover, gem, flower...). */
@Composable
fun ShapedIcon(
    icon: ImageVector,
    shape: Radial,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    rotation: Float = 0f
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(container, RadialShape(shape, rotation))
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** A shaped icon with a title and a line under it, introducing a card or a group of cards. */
@Composable
fun SectionHeader(
    icon: ImageVector,
    shape: Radial,
    container: Color,
    tint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.sm)
    ) {
        ShapedIcon(icon, shape, container, tint)
        Spacer(Modifier.width(Space.lg))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHigh, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(subtitle, color = TextMid, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** The M3 switch, with a tick in the thumb while it is on. */
@Composable
fun TickSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        thumbContent = if (checked) {
            { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = OnAccent,
            checkedTrackColor = Accent,
            checkedIconColor = Accent,
            uncheckedThumbColor = TextMid,
            uncheckedTrackColor = RaisedHighest,
            uncheckedBorderColor = TextLow
        )
    )
}

/** A card that is one line of words and a switch. */
@Composable
fun ToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    corner: Dp = 28.dp
) {
    TonalCard(
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        padding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.lg, vertical = Space.md),
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                if (supporting != null) {
                    Text(supporting, color = TextMid, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.width(Space.md))
            TickSwitch(checked, onCheckedChange, enabled = enabled)
        }
    }
}

/**
 * A choice that lives inside a card. Selected is the brand fill with a tick, the rest are
 * tonal; the selected corner opens up a little, as in the design.
 */
@Composable
fun SegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner by animateFloatAsState(if (selected) 28f else 16f, spring(0.8f, 380f), label = "segment_corner")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(corner.dp))
            .background(if (selected) Accent else RaisedHigh)
            .padding(horizontal = 12.dp)
    ) {
        if (selected) {
            Icon(Icons.Default.Check, null, tint = OnAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            color = if (selected) OnAccent else TextMid,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
    }
}

/**
 * The expressive slider: a fat rounded track and a tall, narrow handle that reads as a
 * grab point rather than a dot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    startLabel: String? = null,
    endLabel: String? = null
) {
    val colors = SliderDefaults.colors(
        thumbColor = Accent,
        activeTrackColor = Accent,
        inactiveTrackColor = AccentWash,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent
    )
    Column(modifier) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = colors,
            thumb = {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Accent)
                )
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = colors,
                    modifier = Modifier.height(32.dp),
                    thumbTrackGapSize = 6.dp,
                    trackInsideCornerSize = 4.dp,
                    drawStopIndicator = null
                )
            }
        )
        if (startLabel != null || endLabel != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.sm)) {
                Text(startLabel.orEmpty(), color = TextMid, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(endLabel.orEmpty(), color = TextMid, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Small tonal pill for a count or a state: "3 selected", "Streams". */
@Composable
fun InfoPill(
    label: String,
    modifier: Modifier = Modifier,
    container: Color = AccentWash,
    content: Color = OnAccentWash,
    icon: ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = content, style = MaterialTheme.typography.labelLarge)
    }
}
