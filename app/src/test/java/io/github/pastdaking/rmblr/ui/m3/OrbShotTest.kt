package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.orb.Orb
import io.github.pastdaking.rmblr.orb.OrbFan
import io.github.pastdaking.rmblr.orb.OrbPhase
import io.github.pastdaking.rmblr.orb.RecordingCaption
import io.github.pastdaking.rmblr.orb.captionTail
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The orb over a chat: idle, with the tone arc open, and while recording with its caption. */
class OrbShotTest : ShotTest() {

    @Composable
    private fun Chat() {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 48.dp)
        ) {
            listOf(
                "Are we still on for dinner tonight?" to false,
                "Yes! Which place did you pick?" to true,
                "The new noodle place by the station. Table is booked for 7:30." to false,
                "Perfect, I will be there." to true,
                "Great. Can you bring the board game?" to false
            ).forEach { (t, mine) ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Text(
                        t, color = TextHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (mine) AccentWash else RaisedHigh)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    private val orbX = 360.dp
    private val orbY = 470.dp

    @Test fun orbIdle() = shot("07a-orb-idle") {
        Chat()
        Orb(OrbPhase.IDLE, 0f, Modifier.offset(orbX, orbY))
    }

    @Test fun orbArc() = shot("07b-orb-arc") {
        Chat()
        OrbFan(
            centre = DpOffset(orbX + 26.dp, orbY + 26.dp),
            items = listOf("Professional", "Clean up", "Casual", "Bullets"),
            highlighted = 2, openRight = false
        )
        Orb(OrbPhase.IDLE, 0f, Modifier.offset(orbX, orbY))
    }

    @Test fun orbRecording() = shot("07c-orb-recording") {
        Chat()
        Orb(OrbPhase.RECORDING, 0.5f, Modifier.offset(orbX, orbY))
        Box(Modifier.offset(0.dp, orbY - 112.dp).fillMaxWidth().padding(end = 8.dp), contentAlignment = Alignment.CenterEnd) {
            RecordingCaption(
                seconds = 6, text = "Sure, I will bring it. Should I grab some dessert on the way",
                tailFromEnd = true, orbSize = 52.dp
            )
        }
    }

    @Test fun captionTailKeepsNewestWords() {
        val long = "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen"
        val t = captionTail(long, 40)
        assertTrue(t.length <= 40)
        assertTrue(long.endsWith(t))
        assertTrue(!t.startsWith(" "))
        assertEquals("short", captionTail(" short ", 40))
    }
}
