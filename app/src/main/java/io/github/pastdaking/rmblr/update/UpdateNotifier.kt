package io.github.pastdaking.rmblr.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.pastdaking.rmblr.MainActivity
import io.github.pastdaking.rmblr.R

/**
 * The "there is a new RMBLR" notification.
 *
 * Deliberately two destinations rather than one. The body of the notification opens the
 * app on the sheet that says what changed, because installing a build without knowing
 * what is in it is not a decision anybody should be asked to make blind. The **Download**
 * action skips all of that and hands the APK link straight to the browser, for the far
 * more common case of someone who has already decided.
 *
 * Its own channel, separate from the orb's silent foreground one, so it can be turned off
 * in Android's settings without also killing the orb's notification — which cannot be
 * turned off, because a foreground service must have one.
 */
object UpdateNotifier {

    private const val CHANNEL_ID = "rmblr_updates"
    private const val NOTIFICATION_ID = 202

    /** Set on the launch intent so MainActivity knows to open the update sheet. */
    const val EXTRA_SHOW_UPDATE = "io.github.pastdaking.rmblr.SHOW_UPDATE"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "App updates",
                // DEFAULT rather than HIGH: this should appear in the shade and wait
                // there. A new version of a dictation app is not worth interrupting
                // whatever someone is doing with a heads-up banner.
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tells you when a new version of RMBLR has been published"
            }
        )
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Post the notification for [release]. A no-op if notifications are not permitted,
     * so the background check never needs to care whether the permission was granted.
     */
    // canPost() is the permission check, one line below. Lint cannot follow a guard that
    // lives behind a helper call, so it reports the notify() as unguarded; it is not.
    @SuppressLint("MissingPermission")
    fun notify(context: Context, release: ReleaseInfo) {
        if (!canPost(context)) return
        ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_SHOW_UPDATE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val download = PendingIntent.getActivity(
            context,
            2,
            Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summary = release.notes.firstMeaningfulLine()
            ?: "Tap to see what changed."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RMBLR ${release.version} is out")
            .setContentText(summary)
            // The long form so the whole first paragraph is readable once expanded,
            // rather than a single truncated line.
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setContentIntent(open)
            .addAction(0, "Download", download)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    /** Clear it once the update has been seen inside the app. */
    fun dismiss(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }
}

/**
 * The first line of a release body that is actually prose.
 *
 * Release notes open with a sentence about as often as they open with a heading, and
 * "**Updates**" is a useless thing to put in a notification. Skips headings, bullets and
 * blank lines, and gives up rather than inventing something.
 */
private fun String.firstMeaningfulLine(): String? =
    lineSequence()
        .map { it.trim() }
        .firstOrNull { line ->
            // "*" catches both bullets and a bolded "**Heading**" opener, which is how
            // every release body in this repo starts its sections.
            line.isNotEmpty() &&
                !line.startsWith("#") &&
                !line.startsWith("-") &&
                !line.startsWith("*")
        }
        ?.replace("**", "")
        ?.take(240)
