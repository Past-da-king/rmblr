package io.github.pastdaking.rmblr.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Redraws every placed RMBLR widget. Cheap when none are placed. */
object WidgetRefresher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching { RmblrOrbWidget().updateAll(app) }
            runCatching { RmblrDictateWidget().updateAll(app) }
            runCatching { RmblrHistoryWidget().updateAll(app) }
        }
    }
}
