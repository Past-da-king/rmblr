package io.github.pastdaking.rmblr.quick

import io.github.pastdaking.rmblr.widget.OrbTap
import io.github.pastdaking.rmblr.widget.orbTapFor
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickFormatTest {

    @Test fun elapsedFormatsMinutesAndHours() {
        assertEquals("0:00", QuickFormat.elapsed(0))
        assertEquals("0:07", QuickFormat.elapsed(7_400))
        assertEquals("12:03", QuickFormat.elapsed(723_000))
        assertEquals("1:02:03", QuickFormat.elapsed(3_723_000))
        assertEquals("0:00", QuickFormat.elapsed(-5_000))
    }

    @Test fun snippetCollapsesWhitespaceAndCuts() {
        assertEquals("Sounds great, see you at seven.", QuickFormat.snippet("  Sounds   great,\nsee you at seven.  ", 60))
        val cut = QuickFormat.snippet("Could you send over the invoice when you get a chance", 20)
        assertEquals(20, cut.length)
        assertTrue(cut.endsWith("\u2026"))
        assertEquals("", QuickFormat.snippet("   ", 10))
    }

    @Test fun stampShowsClockTodayAndDateWhenOld() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 6, 16, 0, 0) }.timeInMillis
        val today = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 6, 9, 5, 0) }.timeInMillis
        val old = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 1, 9, 5, 0) }.timeInMillis
        assertEquals("09:05", QuickFormat.stamp(today, now))
        assertEquals("1 Sep", QuickFormat.stamp(old, now))
    }

    @Test fun levelsKeepTheNewestAndStayInRange() {
        var levels = emptyList<Float>()
        repeat(50) { levels = QuickFormat.appendLevel(levels, if (it == 49) 5f else 0.5f) }
        assertEquals(QuickFormat.MAX_LEVELS, levels.size)
        assertEquals(1f, levels.last(), 0f)
    }

    @Test fun waveBarsPadOnTheLeftAndNeverVanish() {
        val bars = QuickFormat.waveBars(listOf(0.9f, 0.4f), count = 5)
        assertEquals(5, bars.size)
        assertEquals(0.12f, bars[0], 0.0001f)
        assertEquals(0.9f, bars[3], 0.0001f)
        assertEquals(0.4f, bars[4], 0.0001f)
        assertTrue(QuickFormat.waveBars(emptyList(), 4).all { it > 0f })
    }

    @Test fun transcriptsHiddenOnlyOnALockedKeyguard() {
        assertFalse(QuickFormat.showTranscripts(onKeyguardHost = true, deviceLocked = true))
        assertTrue(QuickFormat.showTranscripts(onKeyguardHost = true, deviceLocked = false))
        assertTrue(QuickFormat.showTranscripts(onKeyguardHost = false, deviceLocked = true))
    }

    @Test fun tileStatesFollowTheOrbAndTheRecording() {
        val idle = QuickSnapshot()
        assertEquals(TileModel(TileLook.OFF, "Off"), QuickFormat.tile(false, idle, 0))
        assertEquals(TileModel(TileLook.ON, "Orb on"), QuickFormat.tile(true, idle, 0))
        val rec = QuickSnapshot(QuickPhase.RECORDING, startedAt = 1_000)
        assertEquals(TileModel(TileLook.RECORDING, "Recording 0:07"), QuickFormat.tile(true, rec, 8_000))
        // Recording wins over the orb being off.
        assertEquals(TileLook.RECORDING, QuickFormat.tile(false, rec, 8_000).look)
        assertEquals(TileLook.WORKING, QuickFormat.tile(true, rec.copy(phase = QuickPhase.WORKING), 8_000).look)
    }

    @Test fun tapMeaningPerPhase() {
        assertEquals(QuickAction.START, QuickFormat.action(QuickPhase.IDLE, orbBusy = false))
        assertEquals(QuickAction.START, QuickFormat.action(QuickPhase.DONE, orbBusy = false))
        assertEquals(QuickAction.START, QuickFormat.action(QuickPhase.FAILED, orbBusy = false))
        assertEquals(QuickAction.STOP, QuickFormat.action(QuickPhase.RECORDING, orbBusy = false))
        assertEquals(QuickAction.IGNORE, QuickFormat.action(QuickPhase.WORKING, orbBusy = false))
        // The floating orb owns the microphone: do not start a second recording.
        assertEquals(QuickAction.IGNORE, QuickFormat.action(QuickPhase.IDLE, orbBusy = true))
        assertEquals(QuickAction.STOP, QuickFormat.action(QuickPhase.RECORDING, orbBusy = true))
    }

    @Test fun widgetOrbTapMatchesTheTile() {
        assertEquals(OrbTap.START, orbTapFor(QuickPhase.IDLE))
        assertEquals(OrbTap.STOP, orbTapFor(QuickPhase.RECORDING))
        assertEquals(OrbTap.NONE, orbTapFor(QuickPhase.WORKING))
    }

    @Test fun recordingBodyNamesTheTarget() {
        assertEquals("Writing into Messages", QuickFormat.recordingBody("Messages", null))
        assertEquals("Writing into Messages \u00b7 Clean & Smooth", QuickFormat.recordingBody("Messages", "Clean & Smooth"))
        assertEquals("Copies to your clipboard when you stop", QuickFormat.recordingBody(null, null))
    }

    @Test fun stateClearsTheWaveformWhenRecordingEnds() {
        QuickDictationState.set(QuickSnapshot(QuickPhase.RECORDING, 1))
        QuickDictationState.pushLevel(0.6f)
        assertEquals(1, QuickDictationState.levels.value.size)
        QuickDictationState.set(QuickSnapshot())
        assertTrue(QuickDictationState.levels.value.isEmpty())
    }
}
