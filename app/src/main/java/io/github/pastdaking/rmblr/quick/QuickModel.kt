package io.github.pastdaking.rmblr.quick

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a one-tap dictation (widget, tile or notification) is. */
enum class QuickPhase { IDLE, RECORDING, WORKING, DONE, FAILED }

data class QuickSnapshot(
    val phase: QuickPhase = QuickPhase.IDLE,
    /** Wall clock time the recording began, 0 when not recording. */
    val startedAt: Long = 0L,
    /** Name of the tone being applied, null for a plain transcript. */
    val toneName: String? = null,
    /** Short text for failures. */
    val message: String? = null
)

/**
 * The one place the widgets, the tile and the notifications agree on what a one-tap
 * dictation is doing. The foreground service writes it; everything else only reads.
 */
object QuickDictationState {
    private val _snapshot = MutableStateFlow(QuickSnapshot())
    val snapshot: StateFlow<QuickSnapshot> = _snapshot.asStateFlow()

    /** Recent input levels, 0f..1f, newest last. Drives the widget waveform. */
    private val _levels = MutableStateFlow<List<Float>>(emptyList())
    val levels: StateFlow<List<Float>> = _levels.asStateFlow()

    fun set(snapshot: QuickSnapshot) {
        _snapshot.value = snapshot
        if (snapshot.phase != QuickPhase.RECORDING) _levels.value = emptyList()
    }

    fun pushLevel(level: Float) {
        _levels.value = QuickFormat.appendLevel(_levels.value, level)
    }
}

/** What a tap on the widget orb, the tile or a notification should do right now. */
enum class QuickAction { START, STOP, IGNORE }

/** How the Quick Settings tile looks. */
enum class TileLook { OFF, ON, RECORDING, WORKING }

data class TileModel(val look: TileLook, val subtitle: String)

object QuickFormat {

    const val MAX_LEVELS = 32

    /** 0:07, 12:03, 1:02:03. */
    fun elapsed(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) / 1000L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, s) else "%d:%02d".format(Locale.ROOT, m, s)
    }

    /** One line, whitespace collapsed, cut at [max] characters with an ellipsis. */
    fun snippet(text: String, max: Int): String {
        val flat = text.trim().replace(Regex("\\s+"), " ")
        if (flat.length <= max) return flat
        return flat.take((max - 1).coerceAtLeast(1)).trimEnd() + "…"
    }

    /** 14:32 for today, "Thu" within the week, otherwise "6 Nov". */
    fun stamp(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val sameDay = then.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        val dayMs = 24L * 3600L * 1000L
        return when {
            sameDay -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            now - timestamp < 6 * dayMs && now > timestamp -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun appendLevel(levels: List<Float>, level: Float): List<Float> =
        (levels + level.coerceIn(0f, 1f)).takeLast(MAX_LEVELS)

    /**
     * Bar heights for a fixed-width waveform: newest on the right, older bars to the
     * left, padded with the idle dot height so the row is always [count] long.
     */
    fun waveBars(levels: List<Float>, count: Int, minFraction: Float = 0.12f): List<Float> {
        val tail = levels.takeLast(count)
        val pad = List(count - tail.size) { 0f }
        return (pad + tail).map { it.coerceIn(0f, 1f).coerceAtLeast(minFraction) }
    }

    /**
     * The lock screen must not show what was dictated. A widget on the keyguard host,
     * with the device locked, shows no transcript text.
     */
    fun showTranscripts(onKeyguardHost: Boolean, deviceLocked: Boolean): Boolean =
        !(onKeyguardHost && deviceLocked)

    fun tile(orbOn: Boolean, snapshot: QuickSnapshot, now: Long): TileModel = when (snapshot.phase) {
        QuickPhase.RECORDING -> TileModel(TileLook.RECORDING, "Recording " + elapsed(now - snapshot.startedAt))
        QuickPhase.WORKING -> TileModel(TileLook.WORKING, "Writing it up")
        else -> if (orbOn) TileModel(TileLook.ON, "Orb on") else TileModel(TileLook.OFF, "Off")
    }

    /**
     * @param orbBusy true when the floating orb is recording or transcribing, so a second
     *        recording would fight it for the microphone.
     */
    fun action(phase: QuickPhase, orbBusy: Boolean): QuickAction = when (phase) {
        QuickPhase.RECORDING -> QuickAction.STOP
        QuickPhase.WORKING -> QuickAction.IGNORE
        else -> if (orbBusy) QuickAction.IGNORE else QuickAction.START
    }

    /** The title line of the recording notification. */
    fun recordingTitle(): String = "Recording"

    /** Second line of the recording notification. */
    fun recordingBody(targetLabel: String?, toneName: String?): String {
        val where = if (targetLabel.isNullOrBlank()) "Copies to your clipboard when you stop" else "Writing into $targetLabel"
        return if (toneName.isNullOrBlank()) where else "$where · $toneName"
    }
}
