package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.orb.OrbDirection
import io.github.pastdaking.rmblr.orb.OrbPrefs
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.Radius
import io.github.pastdaking.rmblr.ui.components.SectionLabel
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid

@Composable
fun VoiceStudioScreen(
    prefsManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val currentMode by prefsManager.transcriptionModeFlow.collectAsState()
    val currentPreset by prefsManager.presetFlow.collectAsState()
    val customPrompt by prefsManager.customPromptFlow.collectAsState()

    var editableCustomPrompt by remember(customPrompt) { mutableStateOf(customPrompt) }
    var promptSaved by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val orbPrefs = remember { OrbPrefs(context) }
    var assignments by remember {
        mutableStateOf(OrbDirection.values().associateWith { orbPrefs.actionFor(it) })
    }
    var tapAction by remember { mutableStateOf(orbPrefs.tapAction()) }
    var editing by remember { mutableStateOf<OrbDirection?>(null) }
    var editingTap by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.xl, bottom = Space.xxl)
            .testTag("voice_studio_screen")
    ) {
        Text("Voice", color = TextHigh, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Space.xs))
        Text(
            text = "Decide what Gemini does with your words after it hears them.",
            color = TextMid,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(Space.xxl))

        SectionLabel("Orb shortcuts")

        Panel {
            Text(
                text = "A flick in each direction runs that action straight away. Holding the orb shows the same four.",
                color = TextMid,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(Space.md))

            ShortcutRow("Tap", tapAction.displayName) { editingTap = true; editing = null }
            Hairline(Modifier.padding(vertical = Space.sm))
            OrbDirection.values().forEachIndexed { i, dir ->
                if (i > 0) Hairline(Modifier.padding(vertical = Space.sm))
                ShortcutRow(
                    gesture = dir.name.lowercase().replaceFirstChar { it.uppercase() },
                    action = assignments[dir]?.displayName ?: ""
                ) { editing = dir; editingTap = false }
            }
        }

        if (editing != null || editingTap) {
            Spacer(Modifier.height(Space.md))
            Panel {
                Text(
                    text = "Pick an action",
                    color = TextHigh,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(Space.sm))
                CleanupPreset.values().forEach { preset ->
                    Text(
                        text = preset.displayName,
                        color = TextHigh,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.control))
                            .clickable {
                                if (editingTap) {
                                    orbPrefs.setTapAction(preset)
                                    tapAction = preset
                                } else {
                                    editing?.let {
                                        orbPrefs.setActionFor(it, preset)
                                        assignments = assignments + (it to preset)
                                    }
                                }
                                editing = null
                                editingTap = false
                            }
                            .padding(vertical = Space.md, horizontal = Space.sm)
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xxl))

        SectionLabel("Mode")

        ChoiceRow(
            title = "Polish",
            description = "Drops the ums and false starts, fixes the grammar, keeps your meaning.",
            selected = currentMode == TranscriptionMode.POST_PROCESS_CLEANUP,
            onClick = { prefsManager.setTranscriptionMode(TranscriptionMode.POST_PROCESS_CLEANUP) }
        )
        Spacer(Modifier.height(Space.sm))
        ChoiceRow(
            title = "Verbatim",
            description = "Writes exactly what you said, punctuation only.",
            selected = currentMode == TranscriptionMode.DIRECT_VERBATIM,
            onClick = { prefsManager.setTranscriptionMode(TranscriptionMode.DIRECT_VERBATIM) }
        )

        Spacer(Modifier.height(Space.xxl))

        SectionLabel("Style")

        CleanupPreset.values().forEachIndexed { index, preset ->
            if (index > 0) Spacer(Modifier.height(Space.sm))
            ChoiceRow(
                title = preset.displayName,
                description = preset.description,
                selected = currentPreset == preset,
                onClick = { prefsManager.setCleanupPreset(preset) }
            )
        }

        AnimatedVisibility(visible = currentPreset == CleanupPreset.CUSTOM) {
            Column(modifier = Modifier.padding(top = Space.lg)) {
                Panel {
                    Text(
                        text = "Your instructions",
                        color = TextHigh,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Space.sm))
                    OutlinedTextField(
                        value = editableCustomPrompt,
                        onValueChange = {
                            editableCustomPrompt = it
                            promptSaved = false
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(Radius.control),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Line,
                            focusedTextColor = TextHigh,
                            unfocusedTextColor = TextHigh,
                            cursorColor = Accent,
                            focusedContainerColor = Raised,
                            unfocusedContainerColor = Raised
                        ),
                        placeholder = {
                            Text(
                                "Translate to Spanish and format as bullet points",
                                color = TextLow,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                    Spacer(Modifier.height(Space.md))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.md)
                    ) {
                        PrimaryAction(
                            text = "Save",
                            onClick = {
                                prefsManager.setCustomPrompt(editableCustomPrompt)
                                promptSaved = true
                            }
                        )
                        if (promptSaved) {
                            Text("Saved", color = TextMid, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One selectable option. Selection reads as a filled amber check plus an amber
 * title, so it survives being glanced at rather than needing a border to be found.
 */
@Composable
private fun ChoiceRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.panel))
            .background(if (selected) AccentWash else Surface)
            .clickable { onClick() }
            .padding(Space.lg)
    ) {
        Column(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) Accent else Raised),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.width(Space.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) Accent else TextHigh,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ShortcutRow(gesture: String, action: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.control))
            .clickable { onClick() }
            .padding(vertical = Space.sm)
    ) {
        Text(gesture, color = Accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp))
        Text(action, color = TextHigh, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
