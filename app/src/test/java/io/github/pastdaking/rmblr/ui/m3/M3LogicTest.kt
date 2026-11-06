package io.github.pastdaking.rmblr.ui.m3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3LogicTest {

    @Test fun groupedCornersFirstMiddleLastOnly() {
        assertEquals(28 to 8, groupedCorners(0, 3))
        assertEquals(8 to 8, groupedCorners(1, 3))
        assertEquals(8 to 28, groupedCorners(2, 3))
        assertEquals(28 to 28, groupedCorners(0, 1))
    }

    @Test fun elapsedFormatsMinutesAndSeconds() {
        assertEquals("0:14", formatElapsed(14))
        assertEquals("1:05", formatElapsed(65))
        assertEquals("12:34", formatElapsed(754))
        assertEquals("0:00", formatElapsed(-3))
    }

    @Test fun liveSplitKeepsFinishedSentencesSettled() {
        val w = splitLive("Hi Dana, thanks for your time. As discussed I will send")
        assertEquals("Hi Dana, thanks for your time.", w.settled)
        assertEquals(" As discussed I will send", w.guess)
    }

    @Test fun liveSplitFinishedTextHasNoGuess() {
        val w = splitLive("All done.")
        assertEquals("All done.", w.settled)
        assertEquals("", w.guess)
    }

    @Test fun liveSplitUnfinishedSentenceKeepsLastThreeWordsAsGuess() {
        val w = splitLive("revised timeline by Thursday and loop")
        assertEquals("revised timeline by ", w.settled)
        assertEquals("Thursday and loop", w.guess)
        assertEquals("revised timeline by Thursday and loop", w.settled + w.guess)
    }

    @Test fun liveSplitShortTextIsAllGuess() {
        assertEquals(LiveWords("", "Hello there"), splitLive("Hello there"))
        assertEquals(LiveWords("", ""), splitLive("  "))
    }

    @Test fun amplitudeMapsIntoZeroToOne() {
        assertEquals(0f, amplitudeToLevel(0), 0f)
        assertEquals(1f, amplitudeToLevel(32767), 0.001f)
        assertTrue(amplitudeToLevel(-8000) in 0.3f..0.7f)
    }

    @Test fun waveformBufferRollsAndClamps() {
        val b = WaveformBuffer(4)
        b.push(0.5f); b.push(2f); b.push(-1f)
        assertEquals(listOf(0f, 0.5f, 1f, 0f), b.snapshot())
    }

    @Test fun morphBlendsRadii() {
        val a = Radial { 1f }
        val c = Radial { 0f }
        assertEquals(0.75f, morph(a, c, 0.25f).at(1.2f), 0.0001f)
        assertEquals(1f, morph(a, c, -2f).at(0f), 0f)
    }

    @Test fun shapesTouchTheBoxAtMostOnce() {
        ExpressiveShapes.Library.forEach { (name, r) ->
            var max = 0f
            for (k in 0 until 360) max = maxOf(max, r.at(k * Math.PI.toFloat() / 180f))
            assertEquals("max radius of $name", 1f, max, 0.001f)
        }
    }
}
