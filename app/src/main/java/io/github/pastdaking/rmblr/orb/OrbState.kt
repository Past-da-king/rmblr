package io.github.pastdaking.rmblr.orb

import android.content.Context
import io.github.pastdaking.rmblr.data.CleanupPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OrbPhase { IDLE, MENU, RECORDING, WORKING, DONE, FAILED }

enum class OrbDirection { UP, DOWN, LEFT, RIGHT }

/**
 * The one place the accessibility watcher and the overlay agree on what is happening.
 *
 * They are separate Android services with no binding between them, so rather than
 * wiring an IPC channel for two booleans, both talk to this.
 */
object OrbState {

    private val _inputFocused = MutableStateFlow(false)
    val inputFocused: StateFlow<Boolean> = _inputFocused.asStateFlow()

    private val _phase = MutableStateFlow(OrbPhase.IDLE)
    val phase: StateFlow<OrbPhase> = _phase.asStateFlow()

    // null means "just transcribe it": a plain tap sends the audio straight through
    // with no rewriting pass at all.
    private val _action = MutableStateFlow<CleanupPreset?>(null)
    val action: StateFlow<CleanupPreset?> = _action.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Package of whatever you are typing in, so the orb can offer the right actions. */
    private val _currentPackage = MutableStateFlow<String?>(null)
    val currentPackage: StateFlow<String?> = _currentPackage.asStateFlow()

    fun setCurrentPackage(pkg: String?) {
        if (pkg != null && pkg.isNotBlank()) _currentPackage.value = pkg
    }

    /** True while a text field somewhere on screen has the cursor. */
    fun setInputFocused(focused: Boolean) {
        _inputFocused.value = focused
    }

    fun setPhase(phase: OrbPhase, message: String? = null) {
        _phase.value = phase
        _message.value = message
    }

    fun setAction(action: CleanupPreset?) {
        _action.value = action
    }

    /**
     * The orb is on screen while you are typing somewhere, or while it is busy with
     * something you started. It is never just sitting there over your home screen.
     */
    fun shouldBeVisible(): Boolean =
        _inputFocused.value || _phase.value != OrbPhase.IDLE
}

/**
 * Which action each flick fires, and where the orb was left. Stored separately from
 * the app's own preferences so the overlay can read it without pulling in the rest.
 */
class OrbPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("ilight_orb", Context.MODE_PRIVATE)

    fun actionFor(direction: OrbDirection): CleanupPreset {
        val stored = prefs.getString(key(direction), null)
        return stored?.let { runCatching { CleanupPreset.valueOf(it) }.getOrNull() } ?: default(direction)
    }

    fun setActionFor(direction: OrbDirection, preset: CleanupPreset) {
        prefs.edit().putString(key(direction), preset.name).apply()
    }

    /** The action a plain tap uses. */
    fun tapAction(): CleanupPreset {
        val stored = prefs.getString("tap", null)
        return stored?.let { runCatching { CleanupPreset.valueOf(it) }.getOrNull() } ?: CleanupPreset.SMART_CLEAN
    }

    fun setTapAction(preset: CleanupPreset) {
        prefs.edit().putString("tap", preset.name).apply()
    }

    var x: Int
        get() = prefs.getInt("pos_x", -1)
        set(value) = prefs.edit().putInt("pos_x", value).apply()

    var y: Int
        get() = prefs.getInt("pos_y", -1)
        set(value) = prefs.edit().putInt("pos_y", value).apply()

    /** Orb diameter in dp. Small enough to ignore, big enough to hit. */
    var sizeDp: Int
        get() = prefs.getInt("orb_size", 52).coerceIn(40, 80)
        set(value) = prefs.edit().putInt("orb_size", value.coerceIn(40, 80)).apply()

    /** Which edge it parks against when you let go. */
    var onLeftEdge: Boolean
        get() = prefs.getBoolean("orb_left", false)
        set(value) = prefs.edit().putBoolean("orb_left", value).apply()

    /** How far down the screen it sits, 0f top to 1f bottom. */
    var verticalBias: Float
        get() = prefs.getFloat("orb_bias", 0.5f).coerceIn(0.05f, 0.9f)
        set(value) = prefs.edit().putFloat("orb_bias", value.coerceIn(0.05f, 0.9f)).apply()

    private fun key(d: OrbDirection) = "dir_${d.name.lowercase()}"

    private fun default(d: OrbDirection) = when (d) {
        OrbDirection.UP -> CleanupPreset.FORMAL_EMAIL
        OrbDirection.DOWN -> CleanupPreset.BULLET_POINTS
        OrbDirection.LEFT -> CleanupPreset.CASUAL_CHAT
        OrbDirection.RIGHT -> CleanupPreset.SMART_CLEAN
    }
}
