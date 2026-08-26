package io.github.pastdaking.rmblr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.unit.Dp
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid

/** Spacing scale. Everything in the app steps through these, nothing in between. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /**
     * Room at the bottom of a scrolling screen for the floating navigation.
     *
     * The bar floats, so content is supposed to run underneath it and be visible around
     * it — reserving space OUTSIDE the scroll instead put a hard edge across the screen
     * with the bar sitting on a strip of nothing, which is the one thing a floating bar
     * must not look like. So nothing is reserved: each screen simply scrolls far enough
     * that its last row can clear the bar.
     */
    val navClear = 112.dp
}

/**
 * Corner radii, opened right up for Material 3 Expressive.
 *
 * Expressive's most immediately visible move is shape: cautious 10dp corners are what a
 * settings dialog from 2014 looks like, and generous ones are what a 2026 phone looks
 * like. Controls are near-pills, containers are properly round, and [pill] exists for
 * the things that should be fully round however tall they end up.
 */
object Radius {
    val control = 20.dp
    val panel = 28.dp
    val key = 12.dp
    val pill = 999.dp
}

/**
 * The press feel of the whole app, in one modifier.
 *
 * Expressive motion is spring-based rather than eased: things overshoot slightly and
 * settle, instead of sliding to a stop on a curve. One shared spring means every
 * tappable surface in RMBLR reacts identically, which is most of what makes an
 * interface feel built rather than assembled.
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press_scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactions,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * The heading above a group.
 *
 * Set in tracked-out caps rather than sentence case. It is the one place the app raises
 * its voice, and doing it with letterspacing instead of colour keeps colour meaning what
 * it means everywhere else.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TextMid,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.padding(bottom = Space.md)
    )
}

/**
 * The screen title.
 *
 * Every screen used to open with a 26sp line and then get straight to work, which is
 * what a form does. A screen that opens with a large, confident title and room to
 * breathe reads as a place you have arrived at. This is the single cheapest thing that
 * separates the two.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextHigh,
                style = MaterialTheme.typography.displaySmall
            )
            if (subtitle != null) {
                Spacer(Modifier.height(Space.sm))
                Text(subtitle, color = TextMid, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.md))
            trailing()
        }
    }
    Spacer(Modifier.height(Space.xl))
}

/** How loudly a container should speak. */
enum class PanelTone { PLAIN, HERO }

/**
 * The one container in the app. There is no nested version on purpose.
 *
 * The hairline border is not decoration: the floating navigation has one, and without it
 * on the containers the bar looked like it came from a different app. [PanelTone.HERO]
 * exists so a screen can have exactly one thing that is obviously the point of it —
 * expressive layouts get their rhythm from containers that differ, not from six
 * identical cards in a column.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Space.lg),
    tone: PanelTone = PanelTone.PLAIN,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Radius.panel)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (tone == PanelTone.HERO) AccentWash else Surface)
            .then(
                if (tone == PanelTone.HERO) Modifier
                else Modifier.border(1.dp, Line, shape)
            )
            .padding(padding),
        content = content
    )
}

/**
 * An icon in a filled round chip.
 *
 * A bare 20dp glyph floating next to a label is the look of a list someone had to build.
 * Sitting it in a tinted circle gives every row an anchor of the same size and weight,
 * which is what makes a column of them scan as a designed set.
 */
@Composable
fun IconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = Accent,
    container: androidx.compose.ui.graphics.Color = AccentWash,
    size: Dp = 42.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(Radius.pill))
            .background(container)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Hairline between rows inside a panel. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Line)
    )
}

/**
 * A settings-style row: icon, label, optional supporting line, trailing control.
 * The whole row is the target when [onClick] is given.
 */
@Composable
fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = TextMid,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Space.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
            if (supporting != null) {
                Text(supporting, color = TextMid, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.md))
            trailing()
        }
    }
}

/** Solid amber action. One per screen at most. */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (enabled) Accent else Raised)
            .padding(horizontal = Space.xl)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) OnAccent else TextLow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(
            text = text,
            color = if (enabled) OnAccent else TextLow,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Everything that is not the primary action. */
@Composable
fun QuietAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.pill))
            .background(Raised)
            .padding(horizontal = Space.xl)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextMid, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, color = TextHigh, style = MaterialTheme.typography.labelLarge)
    }
}

/** Small status word. Colour carries meaning, so callers pass it deliberately. */
@Composable
fun StatusText(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Text(text, color = color, style = MaterialTheme.typography.labelSmall, modifier = modifier)
}
