package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentText
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.AlertWash
import io.github.pastdaking.rmblr.ui.theme.EyebrowStyle
import io.github.pastdaking.rmblr.ui.theme.Good
import io.github.pastdaking.rmblr.ui.theme.GoodWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.OnAlertWash
import io.github.pastdaking.rmblr.ui.theme.OnGoodWash
import io.github.pastdaking.rmblr.ui.theme.OnSecondaryWash
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.RecRed
import io.github.pastdaking.rmblr.ui.theme.SecondaryWash
import io.github.pastdaking.rmblr.ui.theme.Skin
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// ---------------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------------

/** Tracked-out caps label: "TONE", "LIVE TRANSCRIPT", "FRI 6 NOV". */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = TextMid) {
    Text(text.uppercase(), color = color, style = EyebrowStyle, modifier = modifier)
}

// ---------------------------------------------------------------------------------
// Cards and groups
// ---------------------------------------------------------------------------------

/**
 * A tonal card: filled with a surface container, 28dp corners, no outline. Pass
 * [onClick] to make the whole card the target.
 */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    container: Color = Raised,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(Space.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.985f, onClick = onClick) else Modifier)
            .clip(shape)
            .background(container)
            .padding(padding),
        content = content
    )
}

/**
 * One row of a grouped list: 28dp on the outer corners, 8dp where it meets a neighbour.
 * Put [Arrangement.spacedBy] 4dp between rows.
 */
@Composable
fun GroupedRow(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    container: Color = Raised,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val (top, bottom) = groupedCorners(index, count)
    TonalCard(
        modifier = modifier,
        container = container,
        shape = RoundedCornerShape(top.dp, top.dp, bottom.dp, bottom.dp),
        padding = PaddingValues(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        onClick = onClick,
        content = content
    )
}

// ---------------------------------------------------------------------------------
// Fields
// ---------------------------------------------------------------------------------

/**
 * The one text field: a filled tonal container, 28dp radius, a primary ring on focus.
 * Used for search, tone names, prompts. Single line by default; pass [minLines] for a
 * multi-line prompt box (the radius drops to 20dp so a tall box stays sensible).
 */
@Composable
fun TonalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(if (minLines > 1) 20.dp else 28.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = maxLines == 1,
        minLines = minLines,
        maxLines = maxLines,
        interactionSource = interaction,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextHigh),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .background(RaisedHigh)
                    .then(if (focused) Modifier.border(2.dp, AccentText, shape) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, tint = TextMid, modifier = Modifier.size(24.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = TextMid, style = MaterialTheme.typography.bodyLarge)
                    }
                    inner()
                }
                trailing?.invoke()
            }
        }
    )
}

// ---------------------------------------------------------------------------------
// Chips
// ---------------------------------------------------------------------------------

/**
 * Filter or choice chip. Unselected: outlined, 12dp corners. Selected: filled tonal with a
 * check, corners open to 18dp.
 */
@Composable
fun M3Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    showCheck: Boolean = true
) {
    val corner by animateFloatAsState(if (selected) 18f else 12f, spring(0.8f, 380f), label = "chip_corner")
    val shape = RoundedCornerShape(corner.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(min = 36.dp)
            .pressable(onClick = onClick)
            .clip(shape)
            .background(if (selected) SecondaryWash else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, Line, shape))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val icon = if (selected && showCheck) Icons.Default.Check else leadingIcon
        if (icon != null) {
            Icon(icon, null, tint = if (selected) OnSecondaryWash else TextMid, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            color = if (selected) OnSecondaryWash else TextHigh,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

/** A tonal assist chip: an action, not a selection ("Clean up", "Translate"). */
@Composable
fun AssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = RaisedHigh,
    content: Color = TextHigh
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(min = 40.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** The small tone tag on a history row: primary container, 8dp corners. */
@Composable
fun ToneTag(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AccentWash)
            .padding(horizontal = 8.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = OnAccentWash, modifier = Modifier.size(13.dp))
        Text(label, color = OnAccentWash, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** What a status chip is telling you. Colour carries meaning and only meaning. */
enum class Status { RECORDING, DONE, WORKING, ERROR, INFO }

/** A compact state chip: a dot and a word. Red is recording, green is done. */
@Composable
fun StatusChip(label: String, status: Status, modifier: Modifier = Modifier) {
    val (bg, fg, dot) = when (status) {
        Status.RECORDING -> Triple(AlertWash, OnAlertWash, RecRed)
        Status.DONE -> Triple(GoodWash, OnGoodWash, Good)
        Status.ERROR -> Triple(AlertWash, OnAlertWash, Alert)
        Status.WORKING -> Triple(AccentWash, OnAccentWash, AccentText)
        Status.INFO -> Triple(RaisedHigh, TextHigh, TextMid)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 12.dp)
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(dot))
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Round tonal icon button (help, close, discard, rewrite). */
@Composable
fun TonalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    container: Color = RaisedHigh,
    tint: Color = TextHigh
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(size / 2))
            .background(container)
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// ---------------------------------------------------------------------------------
// Record button
// ---------------------------------------------------------------------------------

/**
 * The expressive record button. Idle it is an indigo cookie with two soft halos that
 * breathe; recording it morphs into a red rounded-square stop button whose halos swell
 * with the live input [level] (0 to 1).
 *
 * @param animate false freezes the idle breathing, for screenshot tests.
 */
@Composable
fun RecordButton(
    recording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    level: Float = 0f,
    size: Dp = 152.dp,
    animate: Boolean = true,
    micIcon: ImageVector = Icons.Default.Mic
) {
    val morphTo by animateFloatAsState(
        if (recording) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 380f), label = "rec_morph"
    )
    val infinite = rememberInfiniteTransition(label = "rec_breath")
    val breathe by if (animate) infinite.animateFloat(
        1f, 1.03f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "breath"
    ) else remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val drift by if (animate) infinite.animateFloat(
        0f, 12f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "drift"
    ) else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val pulse by animateFloatAsState(level.coerceIn(0f, 1f), spring(stiffness = 300f), label = "rec_level")

    val brand = if (recording) RecRed else Accent
    val haloColor = if (recording) RecRed else Accent
    val coreShape = remember(morphTo) {
        RadialShape(morph(ExpressiveShapes.Cookie9, ExpressiveShapes.Squircle, morphTo))
    }
    val outer = size * 1.65f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(outer)
            .scale(breathe)
    ) {
        // Halos: cookies when idle (calm), swelling soft circles when recording.
        val h2 = outer * (0.80f + 0.20f * pulse * morphTo)
        val h1 = size * (1.30f + 0.12f * pulse * morphTo)
        Box(
            Modifier
                .size(h2)
                .rotate(drift)
                .background(haloColor.copy(alpha = if (recording) 0.10f else 0.13f), RadialShape(morph(ExpressiveShapes.SoftBurst, ExpressiveShapes.Circle, morphTo)))
        )
        Box(
            Modifier
                .size(h1)
                .rotate(-drift)
                .background(haloColor.copy(alpha = if (recording) 0.20f else 0.18f), RadialShape(morph(ExpressiveShapes.Cookie9, ExpressiveShapes.Circle, morphTo)))
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .pressable(pressedScale = 0.94f, onClick = onClick)
                .background(brand, coreShape)
        ) {
            val onBrand = if (recording) Color.White else if (Skin.dark) Color(0xFF0A0B0D) else Color.White
            if (morphTo > 0.5f) {
                Box(Modifier.size(size * 0.22f).clip(RoundedCornerShape(6.dp)).background(Color.White))
            } else {
                Icon(micIcon, null, tint = onBrand, modifier = Modifier.size(size * 0.38f))
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Waveform
// ---------------------------------------------------------------------------------

/**
 * Rolling history of input levels, newest last. Push the recorder's amplitude in on every
 * tick; older bars scroll left.
 */
class WaveformBuffer(private val capacity: Int = 48) {
    private val values = ArrayDeque<Float>()
    init { repeat(capacity) { values.addLast(0f) } }

    fun push(level: Float) {
        values.addLast(level.coerceIn(0f, 1f))
        while (values.size > capacity) values.removeFirst()
    }

    fun snapshot(): List<Float> = values.toList()
}

/** Map a raw 16-bit amplitude (0..32767) to a 0..1 bar level with a gentle curve. */
fun amplitudeToLevel(amplitude: Int): Float {
    val a = (abs(amplitude) / 32767f).coerceIn(0f, 1f)
    return Math.sqrt(a.toDouble()).toFloat().coerceIn(0f, 1f)
}

/**
 * Vertical rounded bars, taller where the input was louder, fading in toward the newest
 * bar on the right. [levels] are 0..1.
 */
@Composable
fun WaveformBars(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    height: Dp = 96.dp
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val n = levels.size
        if (n == 0) return@Canvas
        val slot = size.width / n
        val bar = (slot * 0.52f).coerceAtLeast(2f)
        val mid = size.height / 2
        levels.forEachIndexed { i, lv ->
            val h = (size.height * (0.10f + 0.90f * lv)).coerceAtMost(size.height)
            val alpha = 0.35f + 0.65f * (i / (n - 1).coerceAtLeast(1).toFloat())
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(i * slot + (slot - bar) / 2, mid - h / 2),
                size = Size(bar, h),
                cornerRadius = CornerRadius(bar / 2, bar / 2)
            )
        }
    }
}

/** A static, plausible bar shape for previews and screenshot tests. */
fun sampleWaveform(count: Int = 48): List<Float> = List(count) { i ->
    val x = i / (count - 1f)
    val envelope = sin(PI * x).toFloat()
    (0.15f + 0.85f * envelope * (0.55f + 0.45f * sin(i * 1.7f).let { abs(it) })).coerceIn(0f, 1f)
}

// ---------------------------------------------------------------------------------
// Live text
// ---------------------------------------------------------------------------------

/** The live transcript split into words that are settled and the in-progress tail. */
data class LiveWords(val settled: String, val guess: String)

private val SENTENCE_END = charArrayOf('.', '!', '?', '\n')

/**
 * Split a running transcript. Everything up to and including the last finished sentence is
 * settled; whatever follows is the engine's current guess. When the whole text is one
 * unfinished sentence, the last 3 words are the guess so there is always a lighter tail.
 */
fun splitLive(text: String): LiveWords {
    if (text.isBlank()) return LiveWords("", "")
    val cut = text.indexOfLast { it in SENTENCE_END }
    if (cut >= 0 && cut < text.length - 1) {
        return LiveWords(text.substring(0, cut + 1), text.substring(cut + 1))
    }
    if (cut == text.length - 1) return LiveWords(text, "")
    val words = text.trimEnd().split(' ')
    if (words.size <= 3) return LiveWords("", text)
    val settled = words.dropLast(3).joinToString(" ") + " "
    return LiveWords(settled, text.removePrefix(settled))
}

/** The live transcript, settled words in solid ink and the guess lighter with a caret. */
@Composable
fun LiveTranscript(
    words: LiveWords,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
) {
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = TextHigh)) { append(words.settled) }
        withStyle(SpanStyle(color = TextMid.copy(alpha = 0.62f))) { append(words.guess) }
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text, style = style, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(2.dp))
        Box(
            Modifier
                .padding(bottom = 4.dp)
                .width(3.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Accent)
        )
    }
}

/** 14 seconds becomes "0:14", 754 seconds "12:34". */
fun formatElapsed(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

// ---------------------------------------------------------------------------------
// Snackbar
// ---------------------------------------------------------------------------------

/** A snackbar host drawn in the app's own style: inverse surface, 12dp corners. */
@Composable
fun RmblrSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier = modifier) { data: SnackbarData ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = Space.lg)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (Skin.dark) Color(0xFFE6E6EE) else Color(0xFF2E2F35))
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Text(
                data.visuals.message,
                color = if (Skin.dark) Color(0xFF2E2F35) else Color(0xFFF1F0F7),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp)
            )
            val action = data.visuals.actionLabel
            if (action != null) {
                Text(
                    action,
                    color = if (Skin.dark) Color(0xFF4A3EE0) else Color(0xFFB8AEFF),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .pressable { data.performAction() }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    }
}
