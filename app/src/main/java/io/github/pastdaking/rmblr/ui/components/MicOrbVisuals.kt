package io.github.pastdaking.rmblr.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import kotlinx.coroutines.delay

/**
 * The dictation control. Three states, three readings, no decoration:
 * resting is a quiet disc, recording fills red with a ring that breathes on your voice,
 * working shows a thin ring turning.
 *
 * It sizes itself from [size] rather than filling its parent, because it is also
 * drawn inside a WRAP_CONTENT overlay window where "fill" means "the whole screen".
 */
@Composable
fun MicButton(
    isRecording: Boolean,
    isProcessing: Boolean = false,
    amplitude: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    idleAlpha: Float = 1f
) {
    val ring by animateFloatAsState(
        targetValue = if (isRecording) 1f + (amplitude.coerceIn(0f, 1f) * 0.26f) else 1f,
        animationSpec = tween(90),
        label = "mic_ring"
    )
    val disc = size * 0.82f
    val glyph = size * 0.42f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .alpha(if (isRecording || isProcessing) 1f else idleAlpha)
            .clickable { onClick() }
    ) {
        if (isRecording) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(color = Alert.copy(alpha = 0.20f), radius = (this.size.minDimension / 2f) * ring)
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(disc)
                .clip(CircleShape)
                .background(if (isRecording) Alert else Raised)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(glyph)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                    tint = if (isRecording) OnAccent else TextHigh,
                    modifier = Modifier.size(glyph)
                )
            }
        }
    }
}

/** Older call sites use this name. */
@Composable
fun PulsingStarMicButton(
    isRecording: Boolean,
    isProcessing: Boolean = false,
    amplitude: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = MicButton(isRecording, isProcessing, amplitude, onClick, modifier)

private const val BAR_COUNT = 44

/**
 * A rolling record of what was actually said: each bar is one 45ms slice of your
 * voice, oldest on the left, newest on the right. It scrolls the way speech does,
 * so it reads as a tape rather than a decorative equaliser.
 */
@Composable
fun LiveWaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val history = remember { mutableStateListOf<Float>() }
    val level = rememberUpdatedState(amplitude)

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            history.clear()
            return@LaunchedEffect
        }
        while (true) {
            history.add(level.value.coerceIn(0f, 1f))
            if (history.size > BAR_COUNT) history.removeAt(0)
            delay(45)
        }
    }

    val idle = rememberInfiniteTransition(label = "idle_wave")
    val drift by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "drift"
    )

    Box(
        modifier = modifier.fillMaxWidth().height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gap = 3.dp.toPx()
            val barWidth = ((size.width - gap * (BAR_COUNT - 1)) / BAR_COUNT).coerceAtLeast(1.5.dp.toPx())
            val midY = size.height / 2f
            val floor = 2.dp.toPx()
            val ceiling = size.height - 4.dp.toPx()

            for (i in 0 until BAR_COUNT) {
                val x = i * (barWidth + gap)
                val sample = history.getOrNull(history.size - BAR_COUNT + i)

                val barHeight: Float
                val color: Color
                if (sample == null) {
                    barHeight = if (isRecording) floor else floor * (0.8f + drift * 0.4f)
                    color = Line
                } else {
                    barHeight = (floor + sample * ceiling).coerceIn(floor, ceiling)
                    // The newest few bars are the live edge; the tail settles back.
                    val age = (history.size - 1 - (history.size - BAR_COUNT + i)).coerceAtLeast(0)
                    color = if (age < 3) Alert else Accent
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, midY - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
