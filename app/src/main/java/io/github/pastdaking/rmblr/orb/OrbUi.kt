package io.github.pastdaking.rmblr.orb

import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer

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
import androidx.compose.ui.graphics.graphicsLayer
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
import io.github.pastdaking.rmblr.ui.theme.Scrim
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

/** How small the orb gets once it has been ignored for a while. */
private const val IDLE_SCALE = 0.72f

/**
 * The orb itself.
 *
 * Solid at every phase by default, because a control you cannot see is a control you
 * cannot hit — the half-transparent version was unusable on a light background, and that
 * is why [opacity] is a choice the owner makes rather than something the app decides.
 *
 * [faded] is the separate, temporary version of the same idea: it has been ignored for a
 * while, so it gets out of the way until it is touched. It only ever scales what is
 * drawn, never the window, so the thing stays exactly as easy to hit while it is small.
 */
@Composable
fun Orb(
    phase: OrbPhase,
    amplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = ORB_SIZE,
    /** Show the globe instead of the wave: a tap will translate, not dictate. */
    translate: Boolean = false,
    /** How solid it is at rest. 1f is the original, opaque orb. */
    opacity: Float = 1f,
    /** True once it has sat unused long enough to shrink back out of the way. */
    faded: Boolean = false,
    /** What it fades down to. Ignored unless [faded]. */
    idleOpacity: Float = 1f
) {
    val ring by animateFloatAsState(
        targetValue = if (phase == OrbPhase.RECORDING) 1f + amplitude.coerceIn(0f, 1f) * 0.35f else 1f,
        animationSpec = tween(90),
        label = "orb_ring"
    )

    // Slow out, quick back. Fading away should be something you half-notice; coming back
    // has to feel like it answered you, so it is over before you have finished tapping.
    val targetAlpha = (if (faded) minOf(opacity, idleOpacity) else opacity).coerceIn(0f, 1f)
    val orbAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(if (faded) 600 else 140),
        label = "orb_alpha"
    )
    val orbScale by animateFloatAsState(
        targetValue = if (faded) IDLE_SCALE else 1f,
        animationSpec = tween(if (faded) 600 else 140),
        label = "orb_scale"
    )

    val body = when (phase) {
        OrbPhase.RECORDING -> io.github.pastdaking.rmblr.ui.theme.RecRed
        OrbPhase.WORKING -> Accent
        OrbPhase.DONE -> Good
        OrbPhase.FAILED -> Alert
        else -> Accent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                alpha = orbAlpha
                scaleX = orbScale
                scaleY = orbScale
            }
    ) {
        // A soft halo around the orb: red and swelling with the voice while recording,
        // a still indigo ring the rest of the time.
        Canvas(modifier = Modifier.matchParentSize()) {
            val halo = if (phase == OrbPhase.RECORDING) io.github.pastdaking.rmblr.ui.theme.RecRed else body
            val alpha = if (phase == OrbPhase.RECORDING) 0.26f else 0.16f
            drawCircle(color = halo.copy(alpha = alpha), radius = (this.size.minDimension / 2f) * ring)
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
                        tint = OnAccent,
                        modifier = Modifier.size(size * 0.5f)
                    )
                } else {
                    WaveMark(
                        size = size * 0.46f,
                        tint = OnAccent,
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
    items: List<String>,
    highlighted: Int,
    openRight: Boolean,
    modifier: Modifier = Modifier,
    hint: String = "Flick to a tone, release to dictate",
    orbSize: Dp = ORB_SIZE
) {
    val radius = 128.dp
    val chipWidth = 136.dp
    val chipHeight = 48.dp
    val margin = 8.dp

    // Always a dark wash, whatever the theme (see [Scrim]): it pushes the page back so the
    // pills read clearly instead of fighting the text underneath.
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Scrim.copy(alpha = 0.5f))) {
        val maxX = maxWidth - chipWidth - margin
        val maxY = maxHeight - chipHeight - margin

        // The path the pills sit on, drawn as a faint dashed arc.
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = centre.x.toPx()
            val cy = centre.y.toPx()
            val r = radius.toPx()
            val start = if (openRight) -75f else 105f
            drawArc(
                color = Color.White.copy(alpha = 0.35f),
                startAngle = start,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 14f))
                )
            )
        }

        items.forEachIndexed { index, preset ->
            val angle = fanAngleDegrees(index, items.size, openRight)
            val radians = Math.toRadians(angle.toDouble())
            val dx = (radius.value * kotlin.math.cos(radians)).dp
            val dy = (radius.value * kotlin.math.sin(radians)).dp

            // Clamped so every label stays fully on screen, even next to an edge.
            val x = (centre.x + dx - chipWidth / 2).coerceIn(margin, maxOf(margin, maxX))
            val y = (centre.y + dy - chipHeight / 2).coerceIn(margin, maxOf(margin, maxY))

            ActionChip(
                label = preset,
                active = highlighted == index,
                modifier = Modifier
                    .width(chipWidth)
                    .height(chipHeight)
                    .offset(x = x, y = y)
            )
        }

        // The label is pinned to the top of the screen: that is the title bar of almost
        // every app, never the message being replied to, and it stays clear of the arc.
        val hintWidth = 240.dp
        OrbHint(
            hint,
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .width(hintWidth)
        )
    }
}

/** The one-line instruction under the orb while the arc is open. */
@Composable
fun OrbHint(text: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2E2F35))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFF1F0F7),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
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
internal fun ActionChip(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        animationSpec = androidx.compose.animation.core.spring(0.8f, 380f),
        label = "fan_chip"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp))
            .background(if (active) Accent else io.github.pastdaking.rmblr.ui.theme.RaisedHigh)
    ) {
        Text(
            text = label,
            color = if (active) OnAccent else TextHigh,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/** A one line note that fades in beside the orb: what it did, or why it could not. */
@Composable
fun OrbToast(text: String, tint: Color = TextHigh, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(io.github.pastdaking.rmblr.ui.theme.RaisedHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = tint, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, maxLines = 2)
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
            .background(Scrim.copy(alpha = 0.58f))
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
                .clip(RoundedCornerShape(28.dp))
                .background(io.github.pastdaking.rmblr.ui.theme.Surface)
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
        color = io.github.pastdaking.rmblr.ui.theme.OnAccentWash,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(io.github.pastdaking.rmblr.ui.theme.AccentWash)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}


/**
 * The words you are saying, shown above the orb while it records.
 *
 * It is a small bubble with a tail pointing down at the orb, capped at two lines and
 * trimmed to the newest words, so it never grows over the conversation you are replying
 * to. Settled words are solid, the engine's current guess is lighter. The host window is
 * not touchable, so it can never get in the way of the app underneath.
 *
 * @param tailFromEnd true when the orb is on the right edge, so the tail hangs from the
 *        bubble's right side; false for the left edge.
 */
@Composable
fun RecordingCaption(
    seconds: Int,
    text: String,
    tailFromEnd: Boolean,
    orbSize: Dp,
    modifier: Modifier = Modifier
) {
    val words = io.github.pastdaking.rmblr.ui.m3.splitLive(captionTail(text))
    val bg = io.github.pastdaking.rmblr.ui.theme.RaisedHighest
    Column(
        horizontalAlignment = if (tailFromEnd) Alignment.End else Alignment.Start,
        modifier = modifier.widthIn(max = 280.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(io.github.pastdaking.rmblr.ui.theme.RecRed))
                Spacer(Modifier.width(8.dp))
                Text(
                    io.github.pastdaking.rmblr.ui.m3.formatElapsed(seconds),
                    color = TextHigh,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                )
            }
            if (text.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = TextHigh)) { append(words.settled) }
                        withStyle(androidx.compose.ui.text.SpanStyle(color = TextMid.copy(alpha = 0.7f))) { append(words.guess) }
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        // The tail: a small triangle under the bubble, over the middle of the orb.
        androidx.compose.foundation.Canvas(
            Modifier
                .padding(
                    start = if (tailFromEnd) 0.dp else (orbSize / 2 - 8.dp).coerceAtLeast(8.dp),
                    end = if (tailFromEnd) (orbSize / 2 - 8.dp).coerceAtLeast(8.dp) else 0.dp
                )
                .size(width = 16.dp, height = 8.dp)
        ) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
            }
            drawPath(path, bg)
        }
    }
}

/**
 * The newest words that fit a two line caption: at most [max] characters, starting on a
 * word boundary so it never opens mid-word. Shorter text comes back untouched.
 */
fun captionTail(text: String, max: Int = 90): String {
    val t = text.trim()
    if (t.length <= max) return t
    val cut = t.takeLast(max)
    val space = cut.indexOf(' ')
    return if (space in 0 until cut.length - 1) cut.substring(space + 1) else cut
}
