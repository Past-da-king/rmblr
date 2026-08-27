package io.github.pastdaking.rmblr.orb

import android.content.Context
import io.github.pastdaking.rmblr.data.CleanupPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OrbPhase { IDLE, MENU, RECORDING, WORKING, DONE, FAILED }

/**
 * What the orb does when you use it.
 *
 * The gestures are identical in both — tap to speak, hold for the arc, flick to pick —
 * only the meaning of the arc changes. In [DICTATE] the arc offers tones; in [TRANSLATE]
 * it offers languages, and whatever you say comes out in the one you flicked towards.
 * Keeping the muscle memory the same across both was the point.
 */
enum class OrbMode { DICTATE, TRANSLATE }

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

    /**
     * When something was last copied, or 0 if nothing has been.
     *
     * The orb has no business sitting on screen all day. It earns its place for a minute
     * after you copy something, and then it goes away again. Two things set this: the
     * clipboard listener in the overlay service, and the accessibility service noticing
     * you select text — between them they catch a copy from a browser, a chat app or the
     * selection toolbar without RMBLR ever reading the clipboard in the background.
     */
    private val _copiedAt = MutableStateFlow(0L)
    val copiedAt: StateFlow<Long> = _copiedAt.asStateFlow()

    fun markCopied() {
        _copiedAt.value = System.currentTimeMillis()
    }

    fun clearCopied() {
        _copiedAt.value = 0L
    }

    /** True for [windowMs] after a copy, and false the rest of the time. */
    fun copiedRecently(windowMs: Long): Boolean {
        val at = _copiedAt.value
        return at > 0L && System.currentTimeMillis() - at < windowMs
    }

    private val _mode = MutableStateFlow(OrbMode.DICTATE)
    val mode: StateFlow<OrbMode> = _mode.asStateFlow()

    fun setMode(mode: OrbMode) {
        _mode.value = mode
    }

    fun toggleMode(): OrbMode {
        val next = if (_mode.value == OrbMode.DICTATE) OrbMode.TRANSLATE else OrbMode.DICTATE
        _mode.value = next
        return next
    }

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

    /**
     * Which mode the orb was left in, so a double tap survives the app being killed.
     */
    var mode: OrbMode
        get() = runCatching { OrbMode.valueOf(prefs.getString("mode", "DICTATE")!!) }
            .getOrDefault(OrbMode.DICTATE)
        set(value) = prefs.edit().putString("mode", value.name).apply()

    /**
     * The languages the arc offers in translate mode.
     *
     * Five at most, for the same reason the tone arc caps at five: every extra chip
     * narrows the angle you have to aim at, and the whole point of a flick is that you
     * do not have to look.
     */
    var fanLanguages: List<String>
        get() = (prefs.getString("fan_languages", null) ?: DEFAULT_FAN_LANGUAGES)
            .split('|').map { it.trim() }.filter { it.isNotEmpty() }.take(5)
        set(value) = prefs.edit()
            .putString("fan_languages", value.map { it.trim() }.filter { it.isNotEmpty() }.take(5).joinToString("|"))
            .apply()

    /**
     * Keep the orb on screen even when nothing has the cursor.
     *
     * Off by default, because an orb that is always there is in the way. On for the
     * people who would rather it never went anywhere.
     */
    var alwaysVisible: Boolean
        get() = prefs.getBoolean("always_visible", false)
        set(value) = prefs.edit().putBoolean("always_visible", value).apply()

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

    private companion object {
        const val DEFAULT_FAN_LANGUAGES = "English|Spanish|French|isiZulu|Japanese"
    }

    private fun default(d: OrbDirection) = when (d) {
        OrbDirection.UP -> CleanupPreset.FORMAL_EMAIL
        OrbDirection.DOWN -> CleanupPreset.BULLET_POINTS
        OrbDirection.LEFT -> CleanupPreset.CASUAL_CHAT
        OrbDirection.RIGHT -> CleanupPreset.SMART_CLEAN
    }
}
