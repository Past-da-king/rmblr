package io.github.pastdaking.rmblr.orb

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

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

        /**
         * Labels on the text-selection toolbar that mean "this went to the clipboard".
         *
         * A backstop, not the main signal. Android will not deliver a clipboard change
         * to an app that is not in front, so on some versions the overlay's own listener
         * stays silent and this is what notices instead. Matching on a label is
         * unavoidably language-bound, which is why selecting text arms the orb too — the
         * two together cover the cases either one misses.
         */
        private val COPY_LABELS = setOf(
            "copy", "cut", "copy text", "copy link", "copy link address",
            "kopieer", "copier", "kopieren", "kopiëren", "copiar", "copia",
            "kopiraj", "kopiuj", "копировать", "복사", "コピー", "复制", "نسخ"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        OrbState.setInputFocused(currentFieldIsEditable())
        OrbState.setKeyboardVisible(keyboardIsUp())
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        OrbState.setInputFocused(false)
        // Nothing else can see the keyboard, so leave the answer as "no idea" rather
        // than a stale "yes" that would keep the orb pinned on screen.
        OrbState.setKeyboardVisible(false)
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
                OrbState.setKeyboardVisible(keyboardIsUp())
                noticeCopy(event)
            }

            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // The keyboard going up or down is a window change and nothing else.
                // None of the focus work above applies, so this branch is deliberately
                // the cheap one.
                OrbState.setKeyboardVisible(keyboardIsUp())
            }
        }
    }

    /**
     * Is the on-screen keyboard actually up right now?
     *
     * The only honest way to ask on modern Android. There is no public "is the IME
     * showing" call for an app that is not the one being typed into: WindowInsets answer
     * for your own window, and the orb's window deliberately never takes focus, so it
     * would always be told no. Listing the windows and looking for the input-method one
     * is what is left, and it needs nothing RMBLR does not already hold — the
     * accessibility service is declared with flagRetrieveInteractiveWindows already,
     * because findFocus needs it.
     *
     * A zero-height input-method window does not count: several keyboards, Samsung's
     * included, keep theirs in the list while it is collapsed.
     */
    private fun keyboardIsUp(): Boolean = runCatching {
        windows.any { w ->
            w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                Rect().also { w.getBoundsInScreen(it) }.height() > 0
        }
    }.getOrDefault(false)

    /**
     * Spot the moment text goes to the clipboard, so the orb can appear then and only
     * then. Nothing here reads the clipboard — it only decides whether to offer.
     */
    private fun noticeCopy(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                // A real selection in something you are reading rather than writing. A
                // caret moving in a text box is also a "selection change" and must not
                // count, or the orb would appear every time you tapped mid-sentence.
                val selected = event.toIndex - event.fromIndex
                if (selected > 0 && !sourceIsEditable(event)) OrbState.markCopied()
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val label = (event.text.joinToString(" ").ifBlank {
                    event.contentDescription?.toString().orEmpty()
                }).trim().lowercase()
                if (label.isNotEmpty() && label in COPY_LABELS) OrbState.markCopied()
            }
        }
    }

    private fun sourceIsEditable(event: AccessibilityEvent): Boolean {
        val node = runCatching { event.source }.getOrNull() ?: return false
        val editable = node.isEditable || node.className?.contains("EditText") == true
        node.recycle()
        return editable
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
