package io.github.pastdaking.rmblr.quick

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.pastdaking.rmblr.ai.DictationController
import io.github.pastdaking.rmblr.audio.AudioRecorderManager
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictionaryRepository
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.orb.FieldWatcherService
import io.github.pastdaking.rmblr.orb.OrbPhase
import io.github.pastdaking.rmblr.orb.OrbState
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.orb.ToneStore
import io.github.pastdaking.rmblr.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One-tap dictation for places that are not the orb: the home screen widgets, the Quick
 * Settings tile and the recording notification.
 *
 * It records, transcribes with whatever engine is set in Settings, adds the result to
 * history and puts it on the clipboard. When the accessibility service is on and a field
 * has the cursor, it writes into that field exactly as the orb does. It is self contained:
 * nothing here needs the app, the orb or an activity to be running.
 */
class QuickDictationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: DictationController? = null
    private var recorder: AudioRecorderManager? = null
    private var ticker: Job? = null
    private var tone: Tone? = null
    private var target: String? = null
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAndWrite()
            ACTION_CANCEL -> cancelRecording()
            else -> start(wantsTone = intent?.getBooleanExtra(EXTRA_CLEAN, false) == true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        if (QuickDictationState.snapshot.value.phase == QuickPhase.RECORDING) {
            controller?.cancel()
            QuickDictationState.set(QuickSnapshot())
            WidgetRefresher.refresh(applicationContext)
        }
        scope.cancel()
        QuickTileService.requestRefresh(applicationContext)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- start

    private fun start(wantsTone: Boolean) {
        QuickNotifications.ensureChannels(this)
        val current = QuickDictationState.snapshot.value

        // Android insists a service started with startForegroundService() calls
        // startForeground() promptly, whatever happens next.
        val prefs = PreferencesManager.getInstance(this)
        val selected = if (wantsTone && prefs.getEngine().supportsTone) {
            runCatching { ToneStore(this).byId(CleanupPreset.SMART_CLEAN.name) }.getOrNull()
        } else null
        val label = targetLabel()
        val placeholder = QuickNotifications.recording(
            this, current.startedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            QuickFormat.recordingBody(label, selected?.name)
        )
        if (!enterForeground(placeholder)) {
            fail("Android would not let RMBLR record from here. Open the app and try again.")
            return
        }

        val orbBusy = OrbState.phase.value == OrbPhase.RECORDING || OrbState.phase.value == OrbPhase.WORKING
        if (QuickFormat.action(current.phase, orbBusy) != QuickAction.START) {
            // Already recording or busy: keep whatever is running, drop this extra start.
            if (current.phase == QuickPhase.IDLE || current.phase == QuickPhase.DONE || current.phase == QuickPhase.FAILED) {
                leaveForeground()
                stopSelf()
            }
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail("Microphone permission is off. Allow it in RMBLR first.")
            return
        }

        tone = selected
        target = label
        val rec = AudioRecorderManager(this).also { recorder = it }
        controller = DictationController(
            prefs, rec, DictionaryRepository.getInstance(this), HistoryRepository.getInstance(this)
        )
        val startedAt = System.currentTimeMillis()
        controller?.begin()
        QuickDictationState.set(QuickSnapshot(QuickPhase.RECORDING, startedAt, selected?.name))
        notify(
            QuickNotifications.ID_RECORDING,
            QuickNotifications.recording(this, startedAt, QuickFormat.recordingBody(label, selected?.name))
        )
        QuickTileService.requestRefresh(applicationContext)
        WidgetRefresher.refresh(applicationContext)
        startTicker(rec)
    }

    private fun startTicker(rec: AudioRecorderManager) {
        ticker?.cancel()
        ticker = scope.launch {
            var sinceRefresh = 0
            while (isActive) {
                delay(TICK_MS)
                QuickDictationState.pushLevel(rec.audioAmplitude.value)
                sinceRefresh += 1
                if (sinceRefresh >= REFRESH_EVERY_TICKS) {
                    sinceRefresh = 0
                    WidgetRefresher.refresh(applicationContext)
                }
            }
        }
    }

    // ---------------------------------------------------------------- stop and write

    private fun stopAndWrite() {
        val current = QuickDictationState.snapshot.value
        if (current.phase != QuickPhase.RECORDING || controller == null) {
            if (!inForeground) stopSelf()
            return
        }
        ticker?.cancel()
        QuickDictationState.set(current.copy(phase = QuickPhase.WORKING))
        notify(QuickNotifications.ID_RECORDING, QuickNotifications.working(this))
        QuickTileService.requestRefresh(applicationContext)
        WidgetRefresher.refresh(applicationContext)

        val engineTone = tone
        scope.launch {
            val result = withContext(Dispatchers.IO) { controller!!.finish(engineTone?.prompt) }
            result.onSuccess { (raw, cleaned) ->
                if (cleaned.isBlank()) {
                    finishQuietly()
                } else {
                    deliver(raw, cleaned)
                }
            }.onFailure { err ->
                fail(err.message?.takeIf { it.isNotBlank() } ?: "Something went wrong while transcribing.")
            }
        }
    }

    private suspend fun deliver(raw: String, cleaned: String) {
        HistoryRepository.getInstance(this).addHistoryItem(
            DictationHistoryItem(
                rawText = raw,
                cleanedText = cleaned,
                mode = TranscriptionMode.POST_PROCESS_CLEANUP,
                preset = CleanupPreset.SMART_CLEAN,
                durationSeconds = ((System.currentTimeMillis() - QuickDictationState.snapshot.value.startedAt) / 1000L)
                    .toInt().coerceAtLeast(0)
            )
        )
        val watcher = FieldWatcherService.instance
        val written = watcher?.insertAtCursor(cleaned) == true
        if (!written) {
            runCatching {
                (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                    ?.setPrimaryClip(ClipData.newPlainText("RMBLR", cleaned))
            }
        }
        leaveForeground()
        notify(QuickNotifications.ID_RESULT, QuickNotifications.result(this, cleaned, written, target))
        QuickDictationState.set(QuickSnapshot(QuickPhase.DONE))
        QuickTileService.requestRefresh(applicationContext)
        WidgetRefresher.refresh(applicationContext)
        delay(DONE_HOLD_MS)
        QuickDictationState.set(QuickSnapshot())
        QuickTileService.requestRefresh(applicationContext)
        WidgetRefresher.refresh(applicationContext)
        stopSelf()
    }

    private fun finishQuietly() {
        leaveForeground()
        QuickDictationState.set(QuickSnapshot())
        QuickTileService.requestRefresh(applicationContext)
        WidgetRefresher.refresh(applicationContext)
        stopSelf()
    }

    private fun cancelRecording() {
        ticker?.cancel()
        controller?.cancel()
        leaveForeground()
        NotificationManagerCompat.from(this).cancel(QuickNotifications.ID_RECORDING)
        QuickDictationState.set(QuickSnapshot())
        QuickTileService.requestRefresh(applicationContext)
        WidgetRefresher.refresh(applicationContext)
        stopSelf()
    }

    private fun fail(message: String) {
        ticker?.cancel()
        runCatching { controller?.cancel() }
        leaveForeground()
        notify(QuickNotifications.ID_RESULT, QuickNotifications.failure(this, message))
        QuickDictationState.set(QuickSnapshot(QuickPhase.FAILED, message = message))
        QuickTileService.requestRefresh(applicationContext)
        WidgetRefresher.refresh(applicationContext)
        scope.launch {
            delay(DONE_HOLD_MS)
            QuickDictationState.set(QuickSnapshot())
            QuickTileService.requestRefresh(applicationContext)
            WidgetRefresher.refresh(applicationContext)
            stopSelf()
        }
    }

    // ---------------------------------------------------------------- plumbing

    private fun enterForeground(notification: Notification): Boolean {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        val ok = runCatching {
            ServiceCompat.startForeground(this, QuickNotifications.ID_RECORDING, notification, type)
        }.isSuccess
        if (ok) inForeground = true
        return ok
    }

    private fun leaveForeground() {
        if (inForeground) {
            runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
            inForeground = false
        }
    }

    private fun notify(id: Int, notification: Notification) {
        runCatching {
            // Without the notification permission this is simply dropped, which is fine:
            // the text is already in history and on the clipboard.
            NotificationManagerCompat.from(this).notify(id, notification)
        }
    }

    private fun targetLabel(): String? {
        if (FieldWatcherService.instance == null) return null
        val pkg = OrbState.currentPackage.value ?: return null
        if (pkg == packageName) return null
        return runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
    }

    companion object {
        const val ACTION_START = "io.github.pastdaking.rmblr.quick.START"
        const val ACTION_STOP = "io.github.pastdaking.rmblr.quick.STOP"
        const val ACTION_CANCEL = "io.github.pastdaking.rmblr.quick.CANCEL"
        const val EXTRA_CLEAN = "clean"

        private const val TICK_MS = 250L
        private const val REFRESH_EVERY_TICKS = 4
        private const val DONE_HOLD_MS = 1500L

        fun startIntent(context: Context, clean: Boolean = false): Intent =
            Intent(context, QuickDictationService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_CLEAN, clean)

        fun stopIntent(context: Context): Intent =
            Intent(context, QuickDictationService::class.java).setAction(ACTION_STOP)
    }
}
