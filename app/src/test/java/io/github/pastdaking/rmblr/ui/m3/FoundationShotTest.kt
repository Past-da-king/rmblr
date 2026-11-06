package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import org.junit.Test

/** The foundation components, drawn together so B and C can see what exists. */
class FoundationShotTest : ShotTest() {

    @Test
    fun gallery() = shot("foundation-gallery") {
        var q by remember { mutableStateOf("") }
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 40.dp)
        ) {
            Eyebrow("Tonal field")
            TonalTextField(q, { q = it }, placeholder = "Search your notes", leadingIcon = Icons.Default.Search)
            Eyebrow("Chips")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3Chip("Auto", selected = true, onClick = {})
                M3Chip("Spanish", selected = false, onClick = {})
                AssistChip("Clean up", onClick = {}, icon = Icons.Default.AutoFixHigh)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Recording", Status.RECORDING)
                StatusChip("Done", Status.DONE)
                StatusChip("Working", Status.WORKING)
                ToneTag("Casual")
            }
            Eyebrow("Grouped rows")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    GroupedRow(i, 3) { Text("Row ${i + 1}", color = TextHigh) }
                }
            }
            Eyebrow("Waveform")
            WaveformBars(sampleWaveform())
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                RecordButton(recording = false, onClick = {}, size = 72.dp, animate = false)
            }
            Spacer(Modifier.weight(1f))
            FloatingNav(
                listOf(
                    FloatingNavItem("Home", Icons.Default.Mic, "a"),
                    FloatingNavItem("Profiles", Icons.Default.Layers, "b"),
                    FloatingNavItem("History", Icons.Default.History, "c"),
                    FloatingNavItem("Settings", Icons.Default.Settings, "d")
                ),
                selected = 0, onSelect = {}
            )
        }
    }
}
