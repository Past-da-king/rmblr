package io.github.pastdaking.rmblr.quick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.pastdaking.rmblr.MainActivity
import io.github.pastdaking.rmblr.R

/**
 * Every notification a one-tap dictation shows. Kept as plain builders so the content,
 * the actions and the lock screen behaviour can be checked without a running service.
 *
 * Lock screen: all of them are VISIBILITY_PRIVATE with a public version that says
 * nothing about what was dictated, so a transcript never shows on a secure lock screen
 * unless the person has chosen to show all notification content.
 */
object QuickNotifications {

    const val CHANNEL_RECORDING = "rmblr_recording"
    const val CHANNEL_RESULT = "rmblr_result"
    const val ID_RECORDING = 201
    const val ID_RESULT = 202

    private const val BRAND = 0xFF5A46F0.toInt()
    private const val RED = 0xFFD6362F.toInt()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RECORDING, "Recording", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while RMBLR is listening, with Stop and Cancel"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RESULT, "Dictation results", NotificationManager.IMPORTANCE_LOW).apply {
                description = "What you just dictated, with Copy and Share"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
        )
    }

    fun recording(context: Context, startedAt: Long, body: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_rmblr)
            .setColor(RED)
            .setContentTitle(QuickFormat.recordingTitle())
            .setContentText(body)
            .setUsesChronometer(true)
            .setWhen(startedAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(context, CHANNEL_RECORDING, "Recording"))
            .setContentIntent(openApp(context))
            .addAction(0, "Stop", servicePending(context, QuickDictationService.ACTION_STOP, 11))
            .addAction(0, "Cancel", servicePending(context, QuickDictationService.ACTION_CANCEL, 12))
            .build()

    fun working(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_rmblr)
            .setColor(BRAND)
            .setContentTitle("Writing it up")
            .setContentText("Turning what you said into text")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(context, CHANNEL_RECORDING, "Writing it up"))
            .build()

    /**
     * @param written true when the text went into a field, false when it only reached
     *        the clipboard.
     */
    fun result(context: Context, text: String, written: Boolean, targetLabel: String?): Notification {
        val title = when {
            written && !targetLabel.isNullOrBlank() -> "Written into $targetLabel"
            written -> "Written"
            else -> "Copied to your clipboard"
        }
        return NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_stat_rmblr)
            .setColor(BRAND)
            .setContentTitle(title)
            .setContentText(QuickFormat.snippet(text, 140))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(RESULT_TIMEOUT_MS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(context, CHANNEL_RESULT, "Dictation ready"))
            .setContentIntent(openApp(context))
            .addAction(0, "Copy", copyPending(context, text))
            .addAction(0, "Share", sharePending(context, text))
            .build()
    }

    fun failure(context: Context, message: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_stat_rmblr)
            .setColor(RED)
            .setContentTitle("Dictation did not finish")
            .setContentText(message)
            .setAutoCancel(true)
            .setTimeoutAfter(RESULT_TIMEOUT_MS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(context, CHANNEL_RESULT, "Dictation did not finish"))
            .setContentIntent(openApp(context))
            .build()

    /**
     * The persistent notification the orb service already keeps. Same text as before,
     * plus a Settings shortcut.
     */
    fun ready(context: Context, channelId: String): Notification =
        NotificationCompat.Builder(context, channelId)
            .setContentTitle("RMBLR")
            .setContentText("Ready when you tap a text field")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openApp(context))
            .addAction(0, "Settings", openApp(context, 13))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

    private fun publicVersion(context: Context, channel: String, title: String): Notification =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_rmblr)
            .setContentTitle(title)
            .build()

    private fun openApp(context: Context, request: Int = 10): PendingIntent =
        PendingIntent.getActivity(
            context, request, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun servicePending(context: Context, action: String, request: Int): PendingIntent =
        PendingIntent.getService(
            context, request,
            Intent(context, QuickDictationService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun copyPending(context: Context, text: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, 14,
            Intent(context, QuickActionReceiver::class.java)
                .setAction(QuickActionReceiver.ACTION_COPY)
                .putExtra(QuickActionReceiver.EXTRA_TEXT, text),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun sharePending(context: Context, text: String): PendingIntent {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(send, null)
        return PendingIntent.getActivity(
            context, 15, chooser,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private const val RESULT_TIMEOUT_MS = 5L * 60L * 1000L
}
