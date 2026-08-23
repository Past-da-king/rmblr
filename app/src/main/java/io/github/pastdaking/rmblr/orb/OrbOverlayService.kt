package io.github.pastdaking.rmblr.orb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.pastdaking.rmblr.MainActivity
import io.github.pastdaking.rmblr.R
import io.github.pastdaking.rmblr.ai.GeminiApiClient
import io.github.pastdaking.rmblr.audio.AudioRecorderManager
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The orb.
 *
 * Rules it exists to enforce: it is only on screen when you are actually in a text
 * field, a tap dictates, a hold offers the four actions, a flick fires one of them
 * straight away, and whatever comes back is written into the field for you rather
 * than left on the clipboard for you to paste.
 */
class OrbOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var prefs: PreferencesManager
    private lateinit var orbPrefs: OrbPrefs
    private lateinit var profiles: AppProfileStore
    private lateinit var tones: ToneStore
    private lateinit var history: HistoryRepository
    private lateinit var recorder: AudioRecorderManager
    private var vibrator: Vibrator? = null

    private var windowManager: WindowManager? = null
    private var rootView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams

    // Where the orb sits, in window pixels. Kept separately from [params] because the
    // window itself goes full screen while the menu is open.
    private var orbX = 0
    private var orbY = 0
    private var appliedSize = -1
    private var orbSize by mutableStateOf(52.dp)

    // Compose state, so the overlay redraws itself when the gesture handler changes
    // them; there is no other observer to push an update through.
    private var menuOpen by mutableStateOf(false)
    private var highlighted by mutableStateOf(-1)
    private var orbOffset by mutableStateOf(IntOffset(0, 0))
    private var fanItems by mutableStateOf<List<Tone>>(emptyList())
    private var fanOpensRight by mutableStateOf(false)
    private var dragging = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var downAtMs = 0L
    private var startX = 0
    private var startY = 0
    private var longPress: Runnable? = null
    private var activeTone: Tone? = null




    companion object {
        private const val TAG = "RmblrOrb"



    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        prefs = PreferencesManager.getInstance(this)
        orbPrefs = OrbPrefs(this)
        profiles = AppProfileStore(this)
        tones = ToneStore(this)
        history = HistoryRepository.getInstance(this)
        recorder = AudioRecorderManager(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        startForegroundNotification()
        addOverlay()
        watchVisibility()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { rootView?.let { windowManager?.removeView(it) } }
        rootView = null
        OrbState.setPhase(OrbPhase.IDLE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- overlay

    private fun addOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        applyPlacementFromPrefs()

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = orbX
            y = orbY
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OrbOverlayService)
            setViewTreeViewModelStoreOwner(this@OrbOverlayService)
            setViewTreeSavedStateRegistryOwner(this@OrbOverlayService)
            visibility = View.GONE

            setContent { OrbContent() }
        }

        view.setOnTouchListener { _, event -> onTouch(event) }

        rootView = view
        runCatching { windowManager?.addView(view, params) }
    }

    /** Puts the orb where the home screen says it should be, at the size it says. */
    private fun applyPlacementFromPrefs() {
        val diameter = dp(orbPrefs.sizeDp)
        orbX = if (orbPrefs.onLeftEdge) dp(8) else screenWidth() - diameter - dp(8)
        orbY = (screenHeight() * orbPrefs.verticalBias).toInt().coerceIn(dp(32), screenHeight() - diameter - dp(120))
    }

    /** Show it only while a field has the cursor, or while it is mid job. */
    private fun watchVisibility() {
        scope.launch {
            while (true) {
                val show = OrbState.shouldBeVisible() && prefs.isFloatingAssistantEnabled()

                // The home screen writes size and placement straight to prefs, so pick
                // up any change here rather than making the operator restart the orb.
                val wantSize = orbPrefs.sizeDp
                if (wantSize != appliedSize) {
                    appliedSize = wantSize
                    orbSize = wantSize.dp
                    applyPlacementFromPrefs()
                    applyOrbPosition()
                }
                val v = rootView
                if (v != null) {
                    val want = if (show) View.VISIBLE else View.GONE
                    if (v.visibility != want) v.visibility = want
                }
                delay(150)
            }
        }
    }

    // ---------------------------------------------------------------- gestures

    private fun onTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                downAtMs = System.currentTimeMillis()
                startX = orbX
                startY = orbY
                dragging = false
                highlighted = -1

                longPress = Runnable {
                    if (!dragging) {
                        haptic(18)
                        openMenu()
                    }
                }
                handler.postDelayed(longPress!!, 300)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                val travel = hypot(dx, dy)

                if (menuOpen) {
                    val next = if (travel > dp(46)) nearestFanItem(dx, dy) else -1
                    if (next != highlighted) {
                        highlighted = next
                        if (next >= 0) haptic(10)
                    }
                } else {
                    if (!dragging && travel > dp(12)) {
                        handler.removeCallbacks(longPress ?: Runnable {})
                        dragging = true
                    }
                    if (dragging) {
                        orbX = (startX + dx).toInt()
                        orbY = (startY + dy).toInt()
                        params.x = orbX
                        params.y = orbY
                        runCatching { windowManager?.updateViewLayout(rootView, params) }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress ?: Runnable {})
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                val travel = hypot(dx, dy)
                val elapsed = System.currentTimeMillis() - downAtMs

                if (menuOpen) {
                    val chosen = highlighted
                    val chosenPreset = fanItems.getOrNull(chosen)
                    closeMenu()
                    if (chosenPreset != null) beginDictation(chosenPreset)
                } else if (dragging) {
                    if (elapsed < 300 && travel > dp(90)) {
                        // A flick, not a move: put the orb back and run that direction.
                        orbX = startX
                        orbY = startY
                        applyOrbPosition()
                        beginDictation(tones.byId(directionSlot(directionOf(dx, dy))))
                    } else {
                        snapToEdge()
                    }
                } else {
                    haptic(12)
                    if (OrbState.phase.value == OrbPhase.RECORDING) finishDictation()
                    else beginDictation(tones.byId(activeProfile().tap))
                }
                dragging = false
            }
        }
        return true
    }

    /** The profile covering whatever app is in front right now. */
    private fun activeProfile(): AppProfile = profiles.profileFor(OrbState.currentPackage.value)

    private fun directionSlot(direction: OrbDirection): String {
        val p = activeProfile()
        return when (direction) {
            OrbDirection.UP -> p.up
            OrbDirection.DOWN -> p.down
            OrbDirection.LEFT -> p.left
            OrbDirection.RIGHT -> p.right
        }
    }

    private fun directionOf(dx: Float, dy: Float): OrbDirection = when {
        abs(dx) > abs(dy) -> if (dx < 0) OrbDirection.LEFT else OrbDirection.RIGHT
        else -> if (dy < 0) OrbDirection.UP else OrbDirection.DOWN
    }

    /**
     * Which chip on the arc the finger is pointing at.
     *
     * Compared by angle, so the exact distance does not matter once you are clear of the
     * orb: aim in the direction of the one you want and let go.
     */
    private fun nearestFanItem(dx: Float, dy: Float): Int {
        if (fanItems.isEmpty()) return -1
        val pointing = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        var best = -1
        var bestDelta = Float.MAX_VALUE
        for (i in fanItems.indices) {
            val target = fanAngleDegrees(i, fanItems.size, fanOpensRight)
            // Smallest angle between the two directions, 0 when they line up exactly.
            val delta = kotlin.math.abs(((pointing - target + 540f) % 360f) - 180f)
            if (delta < bestDelta) { bestDelta = delta; best = i }
        }
        // Anything pointing behind the fan is not a choice.
        return if (bestDelta <= 55f) best else -1
    }

    private fun openMenu() {
        orbOffset = IntOffset(orbX, orbY)

        // Open into the screen, away from whichever edge the orb is parked on, so no
        // option can end up off screen.
        fanOpensRight = orbX + dp(orbPrefs.sizeDp) / 2 < screenWidth() / 2

        // Exactly what this profile is for, nothing else. Seven chips on an arc was
        // clutter, and every extra one shrinks the angle you have to aim at.
        val profile = activeProfile()
        fanItems = listOf(profile.up, profile.left, profile.tap, profile.right, profile.down)
            .distinct()
            .take(5)
            .map { tones.byId(it) }

        menuOpen = true
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.x = 0
        params.y = 0
        runCatching { windowManager?.updateViewLayout(rootView, params) }
    }

    private fun closeMenu() {
        menuOpen = false
        highlighted = -1
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        applyOrbPosition()
    }

    private fun applyOrbPosition() {
        params.x = orbX
        params.y = orbY
        runCatching { windowManager?.updateViewLayout(rootView, params) }
    }

    private fun snapToEdge() {
        val diameter = dp(orbPrefs.sizeDp)
        orbPrefs.onLeftEdge = orbX + diameter / 2 < screenWidth() / 2
        orbPrefs.verticalBias = (orbY.toFloat() / screenHeight()).coerceIn(0.05f, 0.9f)
        applyPlacementFromPrefs()
        applyOrbPosition()
    }

    @Composable
    private fun OrbContent() {
        MyApplicationTheme {
            val phase by OrbState.phase.collectAsState()
            val amplitude by recorder.audioAmplitude.collectAsState()
            val density = resources.displayMetrics.density

            if (menuOpen) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OrbFan(
                        centre = DpOffset(
                            x = (orbOffset.x / density).dp + orbSize / 2,
                            y = (orbOffset.y / density).dp + orbSize / 2
                        ),
                        items = fanItems,
                        highlighted = highlighted,
                        openRight = fanOpensRight
                    )
                    Orb(
                        phase = phase,
                        amplitude = amplitude,
                        size = orbSize,
                        modifier = Modifier.offset(
                            x = (orbOffset.x / density).dp,
                            y = (orbOffset.y / density).dp
                        )
                    )
                }
            } else {
                Orb(phase = phase, amplitude = amplitude, size = orbSize)
            }
        }
    }

    // ---------------------------------------------------------------- dictation

    private fun beginDictation(tone: Tone?) {
        activeTone = tone
        OrbState.setPhase(OrbPhase.RECORDING)
        haptic(20)
        recorder.startRecording()
    }

    private fun finishDictation() {
        val tone = activeTone
        OrbState.setPhase(OrbPhase.WORKING)
        haptic(14)

        val wav = recorder.stopRecording()
        val live = recorder.getLiveTranscript()

        scope.launch {
            val apiKey = prefs.getEffectiveApiKey()
            val model = prefs.getSelectedModel()
            val custom = prefs.getCustomPrompt()

            val result = withContext(Dispatchers.IO) {
                if (wav.size > 200) {
                    GeminiApiClient.transcribeAudio(
                        audioBytes = wav,
                        mimeType = "audio/wav",
                        apiKey = apiKey,
                        model = model,
                        mode = if (tone == null) TranscriptionMode.DIRECT_VERBATIM
                               else TranscriptionMode.POST_PROCESS_CLEANUP,
                        preset = CleanupPreset.SMART_CLEAN,
                        customPrompt = custom,
                        instruction = tone?.prompt
                    )
                } else if (live.isNotBlank()) {
                    if (tone == null) Result.success(live to live)
                    else GeminiApiClient.postProcessText(live, apiKey, model, CleanupPreset.CUSTOM, tone.prompt)
                        .map { live to it }
                } else {
                    Result.failure(Exception("Nothing recorded"))
                }
            }

            result.onSuccess { (raw, cleaned) ->
                // Said nothing: do nothing. No paste, no error, no history entry.
                if (cleaned.isBlank()) {
                    OrbState.setPhase(OrbPhase.IDLE)
                    return@onSuccess
                }
                history.addHistoryItem(
                    DictationHistoryItem(
                        rawText = raw,
                        cleanedText = cleaned,
                        mode = TranscriptionMode.POST_PROCESS_CLEANUP,
                        preset = CleanupPreset.SMART_CLEAN
                    )
                )
                val written = FieldWatcherService.instance?.insertAtCursor(cleaned) == true
                OrbState.setPhase(
                    if (written) OrbPhase.DONE else OrbPhase.FAILED,
                    if (written) null else "Copied. Turn on iLight in Accessibility to paste for you."
                )
                haptic(if (written) 24 else 40)
            }.onFailure { err ->
                OrbState.setPhase(OrbPhase.FAILED, err.message)
                haptic(40)
            }

            delay(1200)
            OrbState.setPhase(OrbPhase.IDLE)
        }
    }

        // ---------------------------------------------------------------- plumbing

    private fun startForegroundNotification() {
        val channelId = "ilight_orb"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "RMBLR orb", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Keeps the dictation orb ready while you type"
                }
            )
        }

        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )


        val notification: Notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("RMBLR")
            .setContentText("Ready when you tap a text field")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .build()

        // The orb only holds an overlay; it never records from this service. Claiming the
        // microphone type while idle is what Android 16 kills the process for.
        val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE

        val ok = runCatching {
            ServiceCompat.startForeground(this, 101, notification, type)
        }.isSuccess

        if (!ok) {
            // Never let a refused type take the whole service down with it.
            Log.w(TAG, "foreground type $type refused; falling back")
            runCatching {
                ServiceCompat.startForeground(
                    this, 101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
        }
    }

    private fun haptic(ms: Long) {
        if (!prefs.isHapticEnabled()) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun screenWidth(): Int = resources.displayMetrics.widthPixels
    private fun screenHeight(): Int = resources.displayMetrics.heightPixels
}
