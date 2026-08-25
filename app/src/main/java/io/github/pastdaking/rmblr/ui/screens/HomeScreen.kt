package io.github.pastdaking.rmblr.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.pastdaking.rmblr.ai.DictationController
import io.github.pastdaking.rmblr.audio.AudioRecorderManager
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.orb.FieldWatcherService
import io.github.pastdaking.rmblr.orb.Orb
import io.github.pastdaking.rmblr.orb.OrbPhase
import io.github.pastdaking.rmblr.orb.OrbPrefs
import io.github.pastdaking.rmblr.orb.OrbOverlayService
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.LiveWaveformVisualizer
import io.github.pastdaking.rmblr.ui.components.MicButton
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.QuietAction
import io.github.pastdaking.rmblr.ui.components.SectionLabel
import io.github.pastdaking.rmblr.ui.components.SettingRow
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.Good
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Whether the operator has switched our accessibility service on in Android settings. */
private fun accessibilityEnabled(context: android.content.Context): Boolean {
    val expected = "${context.packageName}/${FieldWatcherService::class.java.name}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

@Composable
fun HomeScreen(
    prefsManager: PreferencesManager,
    historyRepo: HistoryRepository,
    onNavigateToVoiceStudio: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val recorder = remember { AudioRecorderManager(context) }
    val dictation = remember { DictationController(prefsManager, recorder) }
    val isRecording by recorder.isRecording.collectAsState()
    val amplitude by recorder.audioAmplitude.collectAsState()
    // Only ever non-empty on a streaming engine, which is the point: on Gemini Live you
    // watch the sentence appear as you say it.
    val heardSoFar by dictation.liveText.collectAsState()

    val currentMode by prefsManager.transcriptionModeFlow.collectAsState()
    val currentPreset by prefsManager.presetFlow.collectAsState()
    val orbEnabled by prefsManager.floatingEnabledFlow.collectAsState()

    var transcript by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    // These are toggled in Android's own settings, so re-check every time we come back.
    val orbPrefs = remember { OrbPrefs(context) }
    var orbSize by remember { mutableIntStateOf(orbPrefs.sizeDp) }
    var onLeft by remember { mutableStateOf(orbPrefs.onLeftEdge) }
    var bias by remember { mutableFloatStateOf(orbPrefs.verticalBias) }

    var canOverlay by remember { mutableStateOf(false) }
    var canWatch by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    Settings.canDrawOverlays(context)
                canWatch = accessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ready = canOverlay && canWatch

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.xl, bottom = Space.xxl)
            .testTag("home_screen_content")
    ) {
        Text("RMBLR", color = TextHigh, style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(Space.xl))

        SectionLabel("Setup")

        Panel {
            SettingRow(
                label = "Draw over other apps",
                supporting = if (canOverlay) "Allowed" else "So the orb can sit above what you're typing in",
                icon = if (canOverlay) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                iconTint = if (canOverlay) Good else TextLow,
                trailing = {
                    if (!canOverlay) {
                        PrimaryAction(
                            text = "Allow",
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        )
                    }
                }
            )

            Hairline(Modifier.padding(vertical = Space.md))

            SettingRow(
                label = "Know when you're typing",
                supporting = if (canWatch) {
                    "On. The orb appears on focus and writes straight into the field."
                } else {
                    "Turn on RMBLR orb under Accessibility"
                },
                icon = if (canWatch) Icons.Default.CheckCircle else Icons.Default.Accessibility,
                iconTint = if (canWatch) Good else TextLow,
                trailing = {
                    if (!canWatch) {
                        PrimaryAction(
                            text = "Open",
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        )
                    }
                }
            )

            Hairline(Modifier.padding(vertical = Space.md))

            SettingRow(
                label = "Orb",
                supporting = if (ready) "Shows only while a text field has the cursor" else "Finish the two steps above first",
                icon = Icons.Default.Layers,
                trailing = {
                    Switch(
                        checked = orbEnabled && ready,
                        enabled = ready,
                        onCheckedChange = { on ->
                            prefsManager.setFloatingAssistantEnabled(on)
                            val intent = Intent(context, OrbOverlayService::class.java)
                            if (on) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                context.stopService(intent)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Ink,
                            checkedTrackColor = Accent,
                            checkedBorderColor = Accent,
                            uncheckedThumbColor = TextLow,
                            uncheckedTrackColor = Raised,
                            uncheckedBorderColor = Line
                        ),
                        modifier = Modifier.testTag("switch_orb")
                    )
                }
            )
        }

        Spacer(Modifier.height(Space.xxl))

        SectionLabel("Shape and place it")

        Panel {
            // Show the thing being sized, at the size being chosen. Reading "52" tells
            // you nothing about whether it is the right size for your thumb.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().height(88.dp)
            ) {
                Orb(phase = OrbPhase.IDLE, amplitude = 0f, size = orbSize.dp)
            }

            Spacer(Modifier.height(Space.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size", color = TextMid, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(64.dp))
                Slider(
                    value = orbSize.toFloat(),
                    onValueChange = {
                        orbSize = it.toInt()
                        orbPrefs.sizeDp = orbSize
                    },
                    valueRange = 40f..80f,
                    steps = 7,
                    colors = sliderColours(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Space.md))
                Text("$orbSize", color = TextHigh, style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(Space.md))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Height", color = TextMid, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(64.dp))
                Slider(
                    value = bias,
                    onValueChange = {
                        bias = it
                        orbPrefs.verticalBias = it
                    },
                    valueRange = 0.05f..0.9f,
                    colors = sliderColours(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Space.md))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Side", color = TextMid, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(64.dp))
                SideChoice("Left", onLeft) { onLeft = true; orbPrefs.onLeftEdge = true }
                Spacer(Modifier.width(Space.sm))
                SideChoice("Right", !onLeft) { onLeft = false; orbPrefs.onLeftEdge = false }
            }

            Spacer(Modifier.height(Space.lg))
            Text(
                text = "Dragging the orb itself does the same thing, and it remembers.",
                color = TextLow,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(Space.xxl))

        SectionLabel("How to use it")

        Panel {
            Gesture("Tap", "Dictate with your default action.")
            Spacer(Modifier.height(Space.md))
            Gesture("Hold", "Pick one of your four actions, then speak.")
            Spacer(Modifier.height(Space.md))
            Gesture("Flick", "Fire that direction's action straight away.")
            Spacer(Modifier.height(Space.md))
            Gesture("Drag", "Park it wherever you like. It stays there.")
            Spacer(Modifier.height(Space.lg))
            QuietAction(
                text = "Change the four actions",
                onClick = onNavigateToVoiceStudio,
                icon = Icons.Default.Tune
            )
        }

        Spacer(Modifier.height(Space.xxl))

        SectionLabel("Try it here")

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicButton(
                    isRecording = isRecording,
                    isProcessing = working,
                    amplitude = amplitude,
                    size = 56.dp,
                    onClick = {
                        if (isRecording) {
                            working = true
                            status = "Transcribing"
                            scope.launch {
                                // Verbatim means no rewriting call at all. Anything else
                                // hands the tone's own prompt down, custom included.
                                val instruction = when {
                                    currentMode == TranscriptionMode.DIRECT_VERBATIM -> null
                                    currentPreset == CleanupPreset.CUSTOM ->
                                        prefsManager.getCustomPrompt().takeIf { it.isNotBlank() }
                                    else -> currentPreset.systemPrompt
                                }
                                val result = withContext(Dispatchers.IO) { dictation.finish(instruction) }
                                working = false
                                result.onSuccess { (raw, cleaned) ->
                                    transcript = cleaned
                                    status = null
                                    historyRepo.addHistoryItem(
                                        DictationHistoryItem(
                                            rawText = raw,
                                            cleanedText = cleaned,
                                            mode = currentMode,
                                            preset = currentPreset
                                        )
                                    )
                                }.onFailure { status = it.message ?: "That didn't work." }
                            }
                        } else {
                            transcript = null
                            status = null
                            dictation.begin()
                        }
                    }
                )

                Spacer(Modifier.width(Space.lg))

                LiveWaveformVisualizer(
                    amplitude = amplitude,
                    isRecording = isRecording,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Space.md))

            Text(
                text = when {
                    working -> status ?: "Transcribing"
                    isRecording && heardSoFar.isNotBlank() -> heardSoFar
                    isRecording -> "Listening. Tap again when you're done."
                    status != null -> status!!
                    else -> "Tap and speak."
                },
                color = when {
                    isRecording && heardSoFar.isNotBlank() -> TextHigh
                    isRecording -> Alert
                    working -> Accent
                    status != null -> Alert
                    else -> TextMid
                },
                style = MaterialTheme.typography.bodySmall
            )

            if (transcript != null) {
                Spacer(Modifier.height(Space.lg))
                Hairline()
                Spacer(Modifier.height(Space.lg))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        text = transcript ?: "",
                        color = TextHigh,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Space.md))
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(transcript ?: ""))
                            status = "Copied"
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMid, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Gesture(gesture: String, meaning: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Start) {
        Text(
            text = gesture,
            color = Accent,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(56.dp)
        )
        Text(text = meaning, color = TextMid, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SideChoice(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) AccentWash else Raised)
            .clickable { onClick() }
            .padding(horizontal = Space.lg, vertical = Space.sm)
    ) {
        Text(
            text = label,
            color = if (active) Accent else TextMid,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun sliderColours() = SliderDefaults.colors(
    thumbColor = Accent,
    activeTrackColor = Accent,
    inactiveTrackColor = Raised
)
