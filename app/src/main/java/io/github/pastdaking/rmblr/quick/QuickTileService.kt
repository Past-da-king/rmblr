package io.github.pastdaking.rmblr.quick

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.pastdaking.rmblr.R
import io.github.pastdaking.rmblr.orb.OrbOverlayService
import io.github.pastdaking.rmblr.orb.OrbPhase
import io.github.pastdaking.rmblr.orb.OrbState

/**
 * Quick Settings tile. Off: the orb is not running. On: the orb is running. Recording:
 * a one-tap dictation is in progress, with the time. A tap starts a dictation, or stops
 * the one in progress.
 */
class QuickTileService : TileService() {

    private val handler = Handler(Looper.getMainLooper())
    private var listening = false
    private val tick = object : Runnable {
        override fun run() {
            if (!listening) return
            render()
            if (QuickDictationState.snapshot.value.phase == QuickPhase.RECORDING) handler.postDelayed(this, 1000L)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        listening = true
        render()
        handler.removeCallbacks(tick)
        if (QuickDictationState.snapshot.value.phase == QuickPhase.RECORDING) handler.postDelayed(tick, 1000L)
    }

    override fun onStopListening() {
        listening = false
        handler.removeCallbacks(tick)
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val orbBusy = OrbState.phase.value == OrbPhase.RECORDING || OrbState.phase.value == OrbPhase.WORKING
        when (QuickFormat.action(QuickDictationState.snapshot.value.phase, orbBusy)) {
            QuickAction.STOP -> startService(QuickDictationService.stopIntent(this))
            QuickAction.START -> launchDictation()
            QuickAction.IGNORE -> Unit
        }
        render()
    }

    private fun launchDictation() {
        val intent = Intent(this, QuickDictateActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(this, 20, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        val model = QuickFormat.tile(orbRunning, QuickDictationState.snapshot.value, System.currentTimeMillis())
        tile.label = "RMBLR"
        tile.icon = Icon.createWithResource(this, if (model.look == TileLook.RECORDING) R.drawable.ic_tile_wave else R.drawable.ic_tile_mic)
        tile.state = if (model.look == TileLook.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = model.subtitle
        tile.updateTile()
    }

    private val orbRunning: Boolean get() = OrbState.serviceRunning.value

    companion object {
        /** Ask the system to redraw the tile if it is on screen. Harmless otherwise. */
        fun requestRefresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, QuickTileService::class.java))
            }
        }
    }
}
