package io.github.pastdaking.rmblr.orb

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * The only Android API that can answer "is the cursor in a text box right now" and
 * "put this text where the cursor is" for an app you are not.
 *
 * Without it the orb has to sit on screen permanently and hand you the clipboard to
 * paste yourself, which is exactly the behaviour we are getting rid of.
 */
class FieldWatcherService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: FieldWatcherService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        OrbState.setInputFocused(currentFieldIsEditable())
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        OrbState.setInputFocused(false)
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Knowing the app is free here and it is what per-app profiles run on.
                OrbState.setCurrentPackage(event.packageName?.toString())
                OrbState.setInputFocused(currentFieldIsEditable())
            }
        }
    }

    private fun currentFieldIsEditable(): Boolean {
        val node = runCatching { findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull() ?: return false
        val editable = node.isEditable || node.className?.contains("EditText") == true
        node.recycle()
        return editable
    }

    /**
     * Drop [text] in at the cursor.
     *
     * Paste first: it respects the caret and the app's own input handling. Only if the
     * field refuses to paste do we rewrite its whole contents, because that is the
     * version that can lose what was already typed.
     */
    fun insertAtCursor(text: String): Boolean {
        val node = runCatching { findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull() ?: return false
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("RMBLR", text))

            if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return true

            val existing = node.text?.toString().orEmpty()
            val start = node.textSelectionStart.takeIf { it >= 0 } ?: existing.length
            val end = node.textSelectionEnd.takeIf { it >= 0 } ?: existing.length
            val lo = minOf(start, end).coerceIn(0, existing.length)
            val hi = maxOf(start, end).coerceIn(0, existing.length)
            val merged = existing.substring(0, lo) + text + existing.substring(hi)

            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, merged)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } finally {
            node.recycle()
        }
    }

    /** Whatever is already in the field, so an action can rewrite it rather than append. */
    fun currentFieldText(): String? {
        val node = runCatching { findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull() ?: return null
        val text = node.text?.toString()
        node.recycle()
        return text
    }
}
