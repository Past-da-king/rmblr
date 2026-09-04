package io.github.pastdaking.rmblr.orb

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

        /**
         * Class names that mean "you type in this" even when the node forgets to say so.
         *
         * A last resort behind [AccessibilityNodeInfo.isEditable] and the input type,
         * for the widgets that report neither.
         */
        private val EDITABLE_CLASS_HINTS = listOf(
            "EditText", "AutoCompleteTextView", "SearchView", "TextInput", "TextField"
        )

        /** How long after the last event we keep re-asking quickly. */
        private const val BURST_MS = 1_500L

        /** Gap between the quick re-asks inside that window. */
        private const val BURST_STEP_MS = 150L

        /** Gap between re-asks once things have settled and the orb is up. */
        private const val STEADY_STEP_MS = 500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastEventAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wake()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
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
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Knowing the app is free here and it is what per-app profiles run on.
                OrbState.setCurrentPackage(event.packageName?.toString())
                wake()
                noticeCopy(event)
            }
        }
    }

    /**
     * Re-ask now, and keep re-asking for a moment.
     *
     * The event that tells us a text box took the cursor routinely arrives *before* the
     * node tree agrees: ask at that instant and [findFocus] says nothing is focused, and
     * because nothing else was going to happen, the orb then stayed away until the next
     * keystroke shook another event loose. That is the "it only turns up once I start
     * typing" bug. So an event no longer answers the question once — it starts a short
     * burst of re-asks, and while a field really is focused the burst settles into a
     * slow tick that notices the field going away again.
     */
    private fun wake() {
        lastEventAt = SystemClock.uptimeMillis()
        handler.removeCallbacks(recheck)
        handler.post(recheck)
    }

    private val recheck = object : Runnable {
        override fun run() {
            val keyboard = keyboardIsUp()
            OrbState.setKeyboardVisible(keyboard)

            val focused = fieldIsFocused(keyboard)
            OrbState.setInputFocused(focused)

            val settled = SystemClock.uptimeMillis() - lastEventAt > BURST_MS
            // Idle at the home screen there is nothing to watch, so this stops dead
            // rather than ticking in the background for the sake of it.
            if (!settled) handler.postDelayed(this, BURST_STEP_MS)
            else if (focused || keyboard) handler.postDelayed(this, STEADY_STEP_MS)
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
     * Whether the orb should consider you to be writing something.
     *
     * A focused editable node is the good answer. The keyboard being up is the honest
     * fallback: plenty of fields — Compose ones, anything inside a WebView, a launcher's
     * own search box — never surface as a focused editable node to an outside service,
     * and an orb that refuses to appear in those is worse than one that appears while
     * the keyboard is up for some other reason. The keyboard is only ever up because
     * something, somewhere, is taking text.
     */
    private fun fieldIsFocused(keyboardUp: Boolean): Boolean {
        val node = focusedEditableNode()
        if (node != null) {
            node.recycle()
            return true
        }
        return keyboardUp
    }

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
        val editable = node.looksEditable()
        node.recycle()
        return editable
    }

    /**
     * Does anything you can type into hold the cursor, anywhere on screen?
     *
     * [findFocus] alone is documented to search every window and in practice does not:
     * it goes quiet for windows that are not the active one, which is how a launcher's
     * search box or a panel over another app ends up looking unfocused. So the search
     * falls back to the active window's own tree and then to every window in the list.
     * The caller owns the returned node and must recycle it.
     */
    private fun focusedEditableNode(): AccessibilityNodeInfo? {
        runCatching { findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()?.let { node ->
            if (node.looksEditable()) return node
            node.recycle()
        }

        runCatching { rootInActiveWindow }.getOrNull()?.let { root ->
            val node = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
            root.recycle()
            if (node != null) {
                if (node.looksEditable()) return node
                node.recycle()
            }
        }

        runCatching { windows }.getOrNull().orEmpty().forEach { window ->
            if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) return@forEach
            val root = runCatching { window.root }.getOrNull() ?: return@forEach
            val node = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
            root.recycle()
            if (node != null) {
                if (node.looksEditable()) return node
                node.recycle()
            }
        }

        return null
    }

    /**
     * Four ways of asking "can I type in this", because one is not enough.
     *
     * `isEditable` is the right question and plenty of widgets answer it wrongly — most
     * of Compose's own text fields on older versions, and much of what a WebView
     * exposes. A non-zero input type and an offered SET_TEXT action both mean the same
     * thing from the other direction, and the class name catches the rest.
     */
    private fun AccessibilityNodeInfo.looksEditable(): Boolean {
        if (isEditable) return true
        if (inputType != 0) return true
        val name = className?.toString().orEmpty()
        if (EDITABLE_CLASS_HINTS.any { name.contains(it, ignoreCase = true) }) return true
        return runCatching {
            actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT.id }
        }.getOrDefault(false)
    }

    /**
     * Drop [text] in at the cursor.
     *
     * Paste first: it respects the caret and the app's own input handling. Only if the
     * field refuses to paste do we rewrite its whole contents, because that is the
     * version that can lose what was already typed.
     *
     * The clipboard is loaded before anything is attempted and deliberately left loaded
     * whatever happens, including when no field can be found at all — the failure the
     * user sees says "Copied", and it has to be true or a whole dictation is gone.
     */
    fun insertAtCursor(text: String): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        runCatching { clipboard?.setPrimaryClip(ClipData.newPlainText("RMBLR", text)) }

        val node = focusedEditableNode() ?: return false
        try {
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
        val node = focusedEditableNode() ?: return null
        val text = node.text?.toString()
        node.recycle()
        return text
    }
}
