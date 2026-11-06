package io.github.pastdaking.rmblr.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.orb.Orb
import io.github.pastdaking.rmblr.orb.OrbOverlayService
import io.github.pastdaking.rmblr.orb.OrbPhase
import io.github.pastdaking.rmblr.orb.OrbPrefs
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid

/**
 * Orb behaviour: when the orb is there, how big, which side, and when it gets out of
 * the way. These are the controls the old home page carried; the values and where they
 * are stored (`ilight_orb`) are unchanged, so a phone that is already set up keeps its
 * orb exactly as it was.
 */
@Composable
fun OrbBehaviour(
    prefs: PreferencesManager,
    permissions: PermissionState,
    onOpenPermissions: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orbPrefs = remember { OrbPrefs(context) }
    val orbOn by prefs.floatingEnabledFlow.collectAsState()

    var showOnFocus by remember { mutableStateOf(!orbPrefs.alwaysVisible) }
    var sizeDp by remember { mutableIntStateOf(orbPrefs.sizeDp) }
    var onLeft by remember { mutableStateOf(orbPrefs.onLeftEdge) }
    var fadePosition by remember { mutableFloatStateOf(IdleFadeScale.position(orbPrefs.idleFade, orbPrefs.idleAfterSeconds)) }
    var opacity by remember { mutableFloatStateOf(orbPrefs.opacity) }
    val (fades, afterSeconds) = IdleFadeScale.fromPosition(fadePosition)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        // The orb itself. It cannot be switched on until Android has been asked for the two
        // things it needs, so until then the card says what is missing and opens them.
        if (permissions.orbReady) {
            ToggleCard(
                title = "Floating orb",
                supporting = if (orbOn) "On. It follows the text field you are in" else "Off",
                checked = orbOn,
                onCheckedChange = { on -> setOrbRunning(context, prefs, on) },
                modifier = Modifier.testTag("switch_orb")
            )
        } else {
            TonalCard(onClick = onOpenPermissions, padding = PaddingValues(horizontal = Space.lg, vertical = Space.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Floating orb", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Needs ${permissions.missing.size} permission${if (permissions.missing.size == 1) "" else "s"} first",
                            color = TextMid,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextMid)
                }
            }
        }

        ToggleCard(
            title = "Show when a text field is focused",
            supporting = "Hidden everywhere else",
            checked = showOnFocus,
            onCheckedChange = { showOnFocus = it; orbPrefs.alwaysVisible = !it },
            modifier = Modifier.testTag("switch_orb_focus")
        )

        TonalCard(padding = PaddingValues(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size", color = TextHigh, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Box(Modifier.height(80.dp).width(80.dp), contentAlignment = Alignment.Center) {
                    Orb(phase = OrbPhase.IDLE, amplitude = 0f, size = sizeDp.dp, opacity = opacity)
                }
            }
            HandleSlider(
                value = sizeDp.toFloat(),
                onValueChange = { sizeDp = it.toInt(); orbPrefs.sizeDp = sizeDp },
                valueRange = 40f..80f,
                steps = 7,
                startLabel = "Small",
                endLabel = "Large"
            )
        }

        TonalCard(padding = PaddingValues(Space.lg)) {
            Text("Side of the screen", color = TextHigh, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                SegmentChip("Left", onLeft, { onLeft = true; orbPrefs.onLeftEdge = true }, Modifier.weight(1f))
                SegmentChip("Right", !onLeft, { onLeft = false; orbPrefs.onLeftEdge = false }, Modifier.weight(1f))
            }
        }

        TonalCard(padding = PaddingValues(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fade when idle", color = TextHigh, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(IdleFadeScale.label(fades, afterSeconds), color = TextMid, style = MaterialTheme.typography.bodyLarge)
            }
            HandleSlider(
                value = fadePosition,
                onValueChange = {
                    fadePosition = it
                    val (f, s) = IdleFadeScale.fromPosition(it)
                    orbPrefs.idleFade = f
                    if (f) orbPrefs.idleAfterSeconds = s
                },
                startLabel = "Never",
                endLabel = "Quickly"
            )
        }

        TonalCard(onClick = onOpenMore, padding = PaddingValues(horizontal = Space.lg, vertical = Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("More orb options", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Height, see-through, keyboard rule, translating, gestures",
                        color = TextMid,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextMid)
            }
        }
    }
}

/** Start or stop the overlay service together with the preference that says it should run. */
fun setOrbRunning(context: android.content.Context, prefs: PreferencesManager, on: Boolean) {
    prefs.setFloatingAssistantEnabled(on)
    val intent = Intent(context, OrbOverlayService::class.java)
    if (on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    } else {
        context.stopService(intent)
    }
}

/** The orb options that do not earn a card of their own. */
@Composable
fun MoreOrbOptionsSheet(prefs: PreferencesManager, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val orbPrefs = remember { OrbPrefs(context) }
    var bias by remember { mutableFloatStateOf(orbPrefs.verticalBias) }
    var opacity by remember { mutableFloatStateOf(orbPrefs.opacity) }
    var idleOpacity by remember { mutableFloatStateOf(orbPrefs.idleOpacity) }
    var keyboardOnly by remember { mutableStateOf(orbPrefs.requireKeyboard) }
    val showOnFocus = !orbPrefs.alwaysVisible
    val translateOn by prefs.translateEnabledFlow.collectAsState()
    var arc by remember { mutableStateOf(orbPrefs.fanLanguages) }

    RmblrSheet(title = "More orb options", onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            SliderRow("Height", "Low", "High", bias, 0.05f..0.9f) { bias = it; orbPrefs.verticalBias = it }
            SliderRow("Solid", "See-through", "Solid", opacity, 0.15f..1f, "${(opacity * 100).toInt()}%") {
                opacity = it; orbPrefs.opacity = it
            }
            SliderRow("Fades to", "Faint", "Clear", idleOpacity, 0.15f..1f, "${(idleOpacity * 100).toInt()}%") {
                idleOpacity = it; orbPrefs.idleOpacity = it
            }

            ToggleCard(
                title = "Only with the keyboard",
                supporting = if (showOnFocus) "Hide it until the keyboard is actually up" else "Off while the orb is kept on screen",
                checked = keyboardOnly,
                enabled = showOnFocus,
                onCheckedChange = { keyboardOnly = it; orbPrefs.requireKeyboard = it }
            )

            ToggleCard(
                title = "Translate what you copy",
                supporting = "Copy anything and the orb offers to translate it",
                checked = translateOn,
                onCheckedChange = { prefs.setTranslateEnabled(it) }
            )

            TonalCard(padding = PaddingValues(Space.lg)) {
                Text("Languages on the arc", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Up to five. Double tap the orb to translate, then hold to pick one.",
                    color = TextMid,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(Space.md))
                ChipsEditor(
                    items = arc,
                    onItemsChange = { orbPrefs.fanLanguages = it; arc = orbPrefs.fanLanguages },
                    placeholder = "Add a language"
                )
            }

            TonalCard(padding = PaddingValues(Space.lg)) {
                Text("How to use it", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.sm))
                GestureLine("Tap", "Dictate with your default action.")
                GestureLine("Double tap", "Switch between dictating and translating.")
                GestureLine("Hold", "Pick an action, or a language when translating.")
                GestureLine("Flick", "Fire that direction straight away.")
                GestureLine("Drag", "Park it wherever you like. It stays there.")
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    start: String,
    end: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String? = null,
    onChange: (Float) -> Unit
) {
    TonalCard(padding = PaddingValues(Space.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (valueText != null) Text(valueText, color = TextMid, style = MaterialTheme.typography.bodyLarge)
        }
        HandleSlider(value, onChange, valueRange = range, startLabel = start, endLabel = end)
    }
}

@Composable
private fun GestureLine(gesture: String, meaning: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(gesture, color = OnAccentWash, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(88.dp))
        Text(meaning, color = TextMid, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
