package io.github.pastdaking.rmblr.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Hand a link to whichever browser the phone prefers.
 *
 * No custom tab and no in-app WebView on purpose. A download has to land in the browser's
 * download manager for Android's install-from-unknown-sources flow to pick it up, and a
 * WebView is a worse place to be signed into GitHub than the browser someone already uses.
 *
 * Returns false if the phone has nothing that can open a link, which is rare but real on
 * a stripped ROM — the caller falls back to copying rather than doing nothing visible.
 */
fun openUrl(context: Context, url: String): Boolean = runCatching {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
    true
}.getOrDefault(false)

/** Put [text] on the clipboard and say so. */
fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    // Android 13 and up shows its own copy confirmation; a second toast on top of it is
    // one too many.
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
}
