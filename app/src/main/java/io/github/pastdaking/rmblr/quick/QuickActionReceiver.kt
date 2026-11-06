package io.github.pastdaking.rmblr.quick

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Handles the Copy button on the result notification. */
class QuickActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COPY) return
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("RMBLR", text))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_COPY = "io.github.pastdaking.rmblr.quick.COPY"
        const val EXTRA_TEXT = "text"
    }
}
