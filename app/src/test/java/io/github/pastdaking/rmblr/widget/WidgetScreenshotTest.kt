package io.github.pastdaking.rmblr.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.runComposition
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.pastdaking.rmblr.quick.QuickPhase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real Glance layouts to PNGs, light and dark, in every state. The widgets
 * are drawn from RemoteViews exactly as a launcher would inflate them.
 * Record with: ./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@OptIn(androidx.glance.ExperimentalGlanceApi::class)
class WidgetScreenshotTest {

    private val now = 1_762_443_600_000L // 6 Nov 2026

    private val rows = listOf(
        WidgetRow("Sounds great, see you at seven.", now - 60_000L),
        WidgetRow("Could you send over the invoice when you get a chance", now - 3 * 3600_000L)
    )
    private val levels = listOf(0.1f, 0.2f, 0.5f, 0.3f, 0.7f, 0.9f, 0.6f, 0.4f, 0.8f, 1f, 0.7f, 0.5f, 0.3f, 0.6f, 0.9f, 0.5f, 0.2f, 0.3f, 0.15f, 0.1f)

    private fun idle() = WidgetData(QuickPhase.IDLE, rows = rows, now = now)
    private fun recording() = WidgetData(QuickPhase.RECORDING, startedAt = now - 7_000L, levels = levels, rows = rows, toneName = "Casual", now = now)
    private fun working() = WidgetData(QuickPhase.WORKING, rows = rows, now = now)
    private fun empty() = WidgetData(QuickPhase.IDLE, now = now)

    private fun render(widget: GlanceAppWidget, wDp: Int, hDp: Int, name: String, night: Boolean) = runBlocking {
        RuntimeEnvironment.setQualifiers(if (night) "+night" else "+notnight")
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hDp)
        }
        val views = widget.runComposition(ctx, options = options).first()
        val d = ctx.resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val w = (wDp * d).toInt()
        val h = (hDp * d).toInt()
        val wall = FrameLayout(ctx)
        wall.setBackgroundColor(if (night) Color.parseColor("#14121F") else Color.parseColor("#E4DFF5"))
        wall.setPadding(pad, pad, pad, pad)
        val view = views.apply(ctx, wall)
        wall.addView(view, FrameLayout.LayoutParams(w, h))
        val totalW = w + 2 * pad
        val totalH = h + 2 * pad
        wall.measure(View.MeasureSpec.makeMeasureSpec(totalW, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(totalH, View.MeasureSpec.EXACTLY))
        wall.layout(0, 0, totalW, totalH)
        val bmp = android.graphics.Bitmap.createBitmap(totalW, totalH, android.graphics.Bitmap.Config.ARGB_8888)
        wall.draw(android.graphics.Canvas(bmp))
        bmp.captureRoboImage("../docs/m3-build/proof/widget-$name-${if (night) "dark" else "light"}.png")
    }

    private fun both(name: String, w: Int, h: Int, make: () -> GlanceAppWidget) {
        render(make(), w, h, name, night = false)
        render(make(), w, h, name, night = true)
    }

    @Test fun orb1x1Idle() = both("1x1-idle", 76, 76) { RmblrOrbWidget({ idle() }, dynamic = false) }
    @Test fun orb1x1Recording() = both("1x1-recording", 76, 76) { RmblrOrbWidget({ recording() }, dynamic = false) }
    @Test fun dictate2x1Idle() = both("2x1-idle", 176, 76) { RmblrDictateWidget({ idle() }, dynamic = false) }
    @Test fun dictate2x1Recording() = both("2x1-recording", 176, 76) { RmblrDictateWidget({ recording() }, dynamic = false) }
    @Test fun history4x2Idle() = both("4x2-idle", 358, 176) { RmblrHistoryWidget({ idle() }, dynamic = false) }
    @Test fun history4x2Recording() = both("4x2-recording", 358, 176) { RmblrHistoryWidget({ recording() }, dynamic = false) }
    @Test fun history4x2Working() = both("4x2-working", 358, 176) { RmblrHistoryWidget({ working() }, dynamic = false) }
    @Test fun history4x2Empty() = both("4x2-empty", 358, 176) { RmblrHistoryWidget({ empty() }, dynamic = false) }
}
