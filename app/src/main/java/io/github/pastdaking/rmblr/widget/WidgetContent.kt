package io.github.pastdaking.rmblr.widget

import android.app.KeyguardManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.LocalAppWidgetOptions
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.pastdaking.rmblr.MainActivity
import io.github.pastdaking.rmblr.R
import io.github.pastdaking.rmblr.quick.QuickDictateActivity
import io.github.pastdaking.rmblr.quick.QuickDictationService
import io.github.pastdaking.rmblr.quick.QuickFormat
import io.github.pastdaking.rmblr.quick.QuickPhase

/** What a tap on the orb does in the current state. */
enum class OrbTap { START, STOP, NONE }

fun orbTapFor(phase: QuickPhase): OrbTap = when (phase) {
    QuickPhase.RECORDING -> OrbTap.STOP
    QuickPhase.WORKING -> OrbTap.NONE
    else -> OrbTap.START
}

@Composable
private fun orbClick(context: Context, phase: QuickPhase, clean: Boolean = false): GlanceModifier = when (orbTapFor(phase)) {
    OrbTap.START -> GlanceModifier.clickable(
        actionStartActivity(
            Intent(context, QuickDictateActivity::class.java)
                .setAction(QuickDictationService.ACTION_START)
                .putExtra(QuickDictationService.EXTRA_CLEAN, clean)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
    )
    OrbTap.STOP -> GlanceModifier.clickable(stopAction(context))
    OrbTap.NONE -> GlanceModifier
}

private fun stopAction(context: Context): Action =
    actionStartService(QuickDictationService.stopIntent(context), isForegroundService = false)

private fun openAppAction(context: Context): Action =
    actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

/** Recording shows red, everything else indigo. */
@Composable
fun Orb(size: Dp, phase: QuickPhase, modifier: GlanceModifier = GlanceModifier) {
    val recording = phase == QuickPhase.RECORDING
    val halo = if (recording) GlanceTheme.colors.errorContainer else GlanceTheme.colors.primaryContainer
    val core = if (recording) GlanceTheme.colors.error else GlanceTheme.colors.primary
    val ink = if (recording) GlanceTheme.colors.onError else GlanceTheme.colors.onPrimary
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(R.drawable.w_circle), contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(halo),
            contentScale = ContentScale.Fit
        )
        Image(
            provider = ImageProvider(R.drawable.w_circle), contentDescription = null,
            modifier = GlanceModifier.size(size * 0.72f), colorFilter = ColorFilter.tint(core),
            contentScale = ContentScale.Fit
        )
        Image(
            provider = ImageProvider(R.drawable.w_icon_wave),
            contentDescription = if (recording) "Stop dictation" else "Start dictation",
            modifier = GlanceModifier.size(size * 0.36f), colorFilter = ColorFilter.tint(ink),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Glance containers hold at most ten children, so the bars sit in groups of eight
 * inside an outer row of at most ten groups.
 */
@Composable
private fun Waveform(levels: List<Float>, bars: Int) {
    val fractions = QuickFormat.waveBars(levels, bars)
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        fractions.chunked(WAVE_GROUP).forEach { group ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                group.forEach { f ->
                    Box(modifier = GlanceModifier.width(6.dp).height(28.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = GlanceModifier.width(3.dp).height((4f + f * 24f).dp)
                                .background(GlanceTheme.colors.error).cornerRadius(2.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

private const val WAVE_GROUP = 8

@Composable
private fun HistoryLine(row: WidgetRow, maxChars: Int, now: Long) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp).clickable(openAppAction(context)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = GlanceModifier.size(24.dp), contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(R.drawable.w_circle), contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer)
            )
            Image(
                provider = ImageProvider(R.drawable.w_icon_wave), contentDescription = null,
                modifier = GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = QuickFormat.snippet(row.text, maxChars),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp)
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = QuickFormat.stamp(row.timestamp, now), maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        )
    }
}

@Composable
private fun HiddenLine() {
    Text(
        text = "Unlock to see your dictations",
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
    )
}

@Composable
private fun transcriptsVisible(): Boolean {
    val context = LocalContext.current
    val host = LocalAppWidgetOptions.current.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
    val locked = (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceLocked == true
    return QuickFormat.showTranscripts(host == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD, locked)
}

// ---------------------------------------------------------------- 1x1

@Composable
fun OrbWidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val recording = data.phase == QuickPhase.RECORDING
    Box(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(if (recording) GlanceTheme.colors.errorContainer else GlanceTheme.colors.primaryContainer)
            .cornerRadius(28.dp)
            .then(orbClick(context, data.phase)),
        contentAlignment = Alignment.Center
    ) {
        Orb(52.dp, data.phase)
    }
}

// ---------------------------------------------------------------- 2x1

@Composable
fun DictateWidgetContent(data: WidgetData, widthDp: Float = 200f) {
    val context = LocalContext.current
    val recording = data.phase == QuickPhase.RECORDING
    val visible = transcriptsVisible()
    val chars = (((widthDp - 100f) / 7.2f).toInt()).coerceIn(8, 60)
    Row(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.surface).cornerRadius(28.dp)
            .padding(horizontal = 12.dp)
            .then(orbClick(context, data.phase)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Orb(52.dp, data.phase)
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = when (data.phase) {
                    QuickPhase.RECORDING -> "Recording"
                    QuickPhase.WORKING -> "Writing it up"
                    else -> "Dictate"
                },
                maxLines = 1,
                style = TextStyle(
                    color = if (recording) GlanceTheme.colors.error else GlanceTheme.colors.onSurface,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
            )
            val sub = when {
                recording -> QuickFormat.elapsed(data.now - data.startedAt)
                !visible -> "Tap the orb to start"
                data.rows.isNotEmpty() -> QuickFormat.snippet(data.rows.first().text, chars)
                else -> "Tap the orb to start"
            }
            Text(
                text = sub, maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
            )
        }
    }
}

// ---------------------------------------------------------------- 4x2

@Composable
fun HistoryWidgetContent(data: WidgetData, widthDp: Float = 320f) {
    val context = LocalContext.current
    val recording = data.phase == QuickPhase.RECORDING
    val working = data.phase == QuickPhase.WORKING
    val visible = transcriptsVisible()
    val chars = (((widthDp - 120f) / 7.2f).toInt()).coerceIn(10, 70)
    Column(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.surface).cornerRadius(28.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Orb(52.dp, data.phase, orbClick(context, data.phase))
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = when {
                        recording -> "Recording"
                        working -> "Writing it up"
                        else -> "Tap to dictate"
                    },
                    maxLines = 1,
                    style = TextStyle(
                        color = if (recording) GlanceTheme.colors.error else GlanceTheme.colors.onSurface,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = when {
                        recording -> QuickFormat.elapsed(data.now - data.startedAt) + (data.toneName?.let { " · $it" } ?: "")
                        working -> "One moment"
                        else -> "Writes into the app you are in"
                    },
                    maxLines = 2,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            if (recording) {
                Box(
                    modifier = GlanceModifier.size(48.dp).background(GlanceTheme.colors.error).cornerRadius(16.dp)
                        .clickable(stopAction(context)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.w_icon_stop), contentDescription = "Stop",
                        modifier = GlanceModifier.size(22.dp), colorFilter = ColorFilter.tint(GlanceTheme.colors.onError)
                    )
                }
            } else if (!working) {
                Row(
                    modifier = GlanceModifier.height(40.dp).background(GlanceTheme.colors.secondaryContainer).cornerRadius(20.dp)
                        .padding(horizontal = 14.dp).then(orbClick(context, data.phase, clean = true)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.w_icon_wand), contentDescription = null,
                        modifier = GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = "Clean up", maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        if (recording) {
            Waveform(data.levels, bars = (((widthDp - 28f) / 6f).toInt() / WAVE_GROUP * WAVE_GROUP).coerceIn(16, 80))
            Spacer(GlanceModifier.height(2.dp))
        } else {
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.surfaceVariant)) {}
            Spacer(GlanceModifier.height(2.dp))
        }
        if (!visible) {
            HiddenLine()
        } else if (data.rows.isEmpty()) {
            Text(
                text = "Your dictations show up here",
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
            )
        } else {
            data.rows.take(2).forEach { HistoryLine(it, chars, data.now) }
        }
    }
}
