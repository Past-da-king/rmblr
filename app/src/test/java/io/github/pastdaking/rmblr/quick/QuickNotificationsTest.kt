package io.github.pastdaking.rmblr.quick

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickNotificationsTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun Notification.titles() = actions.orEmpty().map { it.title.toString() }

    @Test fun recordingHasStopCancelAndALiveTimer() {
        val n = QuickNotifications.recording(context, startedAt = 1_000L, body = "Writing into Messages")
        assertEquals(listOf("Stop", "Cancel"), n.titles())
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(1_000L, n.`when`)
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("Recording", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }

    @Test fun resultHasCopyAndShareAndHidesTextOnTheLockScreen() {
        val secret = "My card number is 1234"
        val n = QuickNotifications.result(context, secret, written = true, targetLabel = "Messages")
        assertEquals(listOf("Copy", "Share"), n.titles())
        assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility)
        val public = n.publicVersion
        assertNotNull(public)
        val publicText = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT)
            .mapNotNull { public.extras.getCharSequence(it)?.toString() }.joinToString(" ")
        assertFalse(publicText.contains("1234"))
        assertEquals("Written into Messages", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }

    @Test fun resultWithoutAFieldSaysCopied() {
        val n = QuickNotifications.result(context, "Hello", written = false, targetLabel = null)
        assertEquals("Copied to your clipboard", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }

    @Test fun workingAndFailureCarryNoTranscript() {
        val w = QuickNotifications.working(context)
        assertEquals(Notification.VISIBILITY_PRIVATE, w.visibility)
        val f = QuickNotifications.failure(context, "No network")
        assertEquals("No network", f.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test fun readyNotificationIsSecretOnTheLockScreenAndHasSettings() {
        val n = QuickNotifications.ready(context, "ilight_orb")
        assertEquals(Notification.VISIBILITY_SECRET, n.visibility)
        assertEquals(listOf("Settings"), n.titles())
    }
}
