package io.github.pastdaking.rmblr.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.LocalContext
import androidx.compose.runtime.Composable

/**
 * The three widgets. Each reads its data once per update; the dictation service and the
 * history repository call [WidgetRefresher] whenever something they show changes.
 * [source] is a seam for tests, which render the layouts with fixed data.
 */
class RmblrOrbWidget(
    private val source: (Context) -> WidgetData = WidgetData::live,
    private val dynamic: Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = source(context)
        provideContent { RmblrGlanceTheme(dynamic) { OrbWidgetContent(data) } }
    }
}

class RmblrDictateWidget(
    private val source: (Context) -> WidgetData = WidgetData::live,
    private val dynamic: Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = source(context)
        provideContent { RmblrGlanceTheme(dynamic) { DictateWidgetContent(data, LocalSize.current.width.value) } }
    }
}

class RmblrHistoryWidget(
    private val source: (Context) -> WidgetData = WidgetData::live,
    private val dynamic: Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = source(context)
        provideContent { RmblrGlanceTheme(dynamic) { HistoryWidgetContent(data, LocalSize.current.width.value) } }
    }
}

class RmblrOrbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RmblrOrbWidget()
}

class RmblrDictateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RmblrDictateWidget()
}

class RmblrHistoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RmblrHistoryWidget()
}
