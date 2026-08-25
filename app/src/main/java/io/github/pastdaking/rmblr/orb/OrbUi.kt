package io.github.pastdaking.rmblr.orb

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.Good
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid

val ORB_SIZE: Dp = 52.dp

/**
 * The RMBLR mark: five bars rising to the middle, the shape of a spoken phrase.
 *
 * Used instead of a microphone glyph everywhere. A mic says "recording"; this says which
 * app you are looking at, and it is the same shape as the launcher icon.
 */
@Composable
fun WaveMark(
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    level: Float = 0f
) {
    Canvas(modifier = modifier.size(size)) {
        val bars = 5
        val gap = this.size.width / (bars * 2.2f)
        val barWidth = gap
        val heights = listOf(0.34f, 0.66f, 1f, 0.72f, 0.42f)
        for (i in 0 until bars) {
            val grow = 1f + level.coerceIn(0f, 1f) * (if (i == 2) 0.15f else 0.3f)
            val h = (this.size.height * heights[i] * grow).coerceAtMost(this.size.height)
            val x = gap * 0.6f + i * (barWidth + gap)
            drawLine(
                color = tint,
                start = Offset(x, this.size.height / 2 - h / 2),
                end = Offset(x, this.size.height / 2 + h / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * The orb itself. Solid at every phase, because a control you cannot see is a control
 * you cannot hit: the half-transparent version was unusable on a light background.
 */
@Composable
fun Orb(
    phase: OrbPhase,
    amplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = ORB_SIZE,
    /** Show the globe instead of the wave: a tap will translate, not dictate. */
    translate: Boolean = false
) {
    val ring by animateFloatAsState(
        targetValue = if (phase == OrbPhase.RECORDING) 1f + amplitude.coerceIn(0f, 1f) * 0.35f else 1f,
        animationSpec = tween(90),
        label = "orb_ring"
    )

    val body = when (phase) {
        OrbPhase.RECORDING -> Alert
        OrbPhase.WORKING -> Accent
        OrbPhase.DONE -> Good
        OrbPhase.FAILED -> Alert
        else -> Surface
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        if (phase == OrbPhase.RECORDING) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(color = Alert.copy(alpha = 0.22f), radius = (this.size.minDimension / 2f) * ring)
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.84f)
                .clip(CircleShape)
                .background(body)
        ) {
            when (phase) {
                OrbPhase.WORKING -> WorkingRing(size)
                OrbPhase.RECORDING -> WaveMark(
                    size = size * 0.46f,
                    tint = OnAccent,
                    level = amplitude
                )
                else -> if (translate) {
                    // The 文A glyph, which is what every translate button on a phone
                    // looks like. A globe means "the internet", not "translate this".
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate what you copied",
                        tint = if (phase == OrbPhase.DONE) OnAccent else TextHigh,
                        modifier = Modifier.size(size * 0.5f)
                    )
                } else {
                    WaveMark(
                        size = size * 0.46f,
                        tint = if (phase == OrbPhase.DONE) OnAccent else TextHigh,
                        level = amplitude
                    )
                }
            }
        }
    }
}

/**
 * The working indicator.
 *
 * It used to be a bare arc with no animation at all, which read as frozen at exactly the
 * moment you most need to know something is happening. It turns now, and the gap sweeps,
 * so a slow request looks slow rather than broken.
 */
@Composable
private fun WorkingRing(size: Dp = ORB_SIZE) {
    val spin = rememberInfiniteTransition(label = "orb_working")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_working_angle"
    )
    val sweep by spin.animateFloat(
        initialValue = 40f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_working_sweep"
    )

    Canvas(modifier = Modifier.size(size * 0.46f)) {
        drawArc(
            color = OnAccent,
            startAngle = angle,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * The action fan.
 *
 * The old layout put one chip in each compass direction, which breaks the moment the orb
 * is parked against an edge: the chip on that side runs off screen and the option becomes
 * unreachable. This opens as a half circle pointing INTO the screen, so every option is
 * on screen no matter which edge the orb lives on, and it scales past four without
 * running out of directions.
 *
 * Selection is by angle rather than by quadrant: slide toward the one you want.
 */
@Composable
fun OrbFan(
    centre: DpOffset,
    items: List<Tone>,
    highlighted: Int,
    openRight: Boolean,
    modifier: Modifier = Modifier
) {
    val radius = 116.dp
    val chipWidth = 128.dp
    val chipHeight = 34.dp
    val margin = 8.dp

    // Always a dark wash, whatever the theme. In light mode the scrim used to be the light
    // Ink, which dimmed nothing and left white chips floating unreadably over the keyboard.
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Ink.copy(alpha = 0.55f))) {
        val maxX = maxWidth - chipWidth - margin
        val maxY = maxHeight - chipHeight - margin

        items.forEachIndexed { index, preset ->
            val angle = fanAngleDegrees(index, items.size, openRight)
            val radians = Math.toRadians(angle.toDouble())
            val dx = (radius.value * kotlin.math.cos(radians)).dp
            val dy = (radius.value * kotlin.math.sin(radians)).dp

            // The arc points away from the parked edge, but a chip centred near the orb's own
            // x still hangs off the screen. Clamping keeps every label fully readable, which
            // matters more than the chip sitting exactly on the arc.
            val x = (centre.x + dx - chipWidth / 2).coerceIn(margin, maxOf(margin, maxX))
            val y = (centre.y + dy - chipHeight / 2).coerceIn(margin, maxOf(margin, maxY))

            ActionChip(
                label = preset.name,
                active = highlighted == index,
                modifier = Modifier
                    .width(chipWidth)
                    .height(chipHeight)
                    .offset(x = x, y = y)
            )
        }
    }
}

/**
 * Where item [index] sits on the arc, in degrees, 0 pointing right and growing clockwise
 * to match screen coordinates. The fan always points away from the edge the orb is on.
 */
fun fanAngleDegrees(index: Int, count: Int, openRight: Boolean): Float {
    val spread = 150f
    val step = if (count <= 1) 0f else spread / (count - 1)
    val offset = -spread / 2f + step * index
    return if (openRight) offset else 180f - offset
}

@Composable
private fun ActionChip(label: String, active: Boolean, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(if (active) Accent else Surface)
    ) {
        Text(
            text = label,
            color = if (active) OnAccent else TextMid,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/** A one line note that fades in beside the orb: what it did, or why it could not. */
@Composable
fun OrbToast(text: String, tint: Color = TextHigh, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = tint, fontSize = 12.sp, maxLines = 2)
    }
}

/**
 * The translation bubble.
 *
 * Deliberately a card floating over a scrim rather than a toast or a notification: you
 * are reading a paragraph in a language you do not speak, so it has to sit still, be
 * scrollable, and let you take the text with you. Tapping anywhere outside dismisses it,
 * because the one thing worse than no translation is a translation you cannot get rid of.
 */
@Composable
fun TranslationBubble(
    text: String?,
    error: String?,
    working: Boolean,
    targetLanguage: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.72f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Surface)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}   // swallow taps on the card itself
                )
                .padding(18.dp)
        ) {
            Text(
                text = if (error != null) "Couldn't translate" else "In $targetLanguage",
                color = if (error != null) Alert else TextMid,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Box(modifier = Modifier.height(10.dp))

            when {
                working -> Text("Translating…", color = TextMid, fontSize = 15.sp)
                error != null -> Text(error, color = TextHigh, fontSize = 15.sp)
                else -> Text(
                    text = text.orEmpty(),
                    color = TextHigh,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            if (!working && error == null && !text.isNullOrBlank()) {
                Box(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    BubbleAction("Copy", onCopy)
                    Box(modifier = Modifier.width(8.dp))
                    BubbleAction("Close", onDismiss)
                }
            }
        }
    }
}

@Composable
private fun BubbleAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Accent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
