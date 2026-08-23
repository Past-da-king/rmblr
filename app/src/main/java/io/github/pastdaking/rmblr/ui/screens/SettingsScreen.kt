package io.github.pastdaking.rmblr.ui.screens

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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ai.GeminiApiClient
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.Radius
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
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.launch

// Flash Live does the work: it has the most headroom, it is the fastest, and it is the
// only one that handles isiZulu properly. The rest exist so a quota block on one model
// cannot lose a dictation; the client walks down this order automatically.
private val MODELS = listOf(
    "gemini-3.1-flash-live-preview" to "Best for isiZulu and mixed languages. Use this.",
    "gemini-3.5-flash" to "Backup. Solid on English.",
    "gemini-3.1-flash-lite" to "Cheapest. Last resort."
)

@Composable
fun SettingsScreen(
    prefsManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val userApiKey by prefsManager.apiKeyFlow.collectAsState()
    val selectedModel by prefsManager.modelFlow.collectAsState()

    var inputApiKey by remember(userApiKey) { mutableStateOf(prefsManager.getUserApiKey()) }
    var showKeyText by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }


    var isHaptic by remember { mutableStateOf(prefsManager.isHapticEnabled()) }
    var isAutoCap by remember { mutableStateOf(prefsManager.isAutoCapitalizeEnabled()) }

    val hasKey = prefsManager.getEffectiveApiKey().isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.xl, bottom = Space.xxl)
            .testTag("settings_screen")
    ) {
        Text("Settings", color = TextHigh, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Space.xs))
        Text(
            text = "Your Gemini key, the model behind it, and how the keys feel.",
            color = TextMid,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(Space.xxl))

        // ---- API key -----------------------------------------------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionLabel("Gemini key")
            Text(
                text = if (hasKey) "Connected" else "Not set",
                color = if (hasKey) Good else Accent,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = Space.sm)
            )
        }

        Panel {
            OutlinedTextField(
                value = inputApiKey,
                onValueChange = {
                    inputApiKey = it
                    prefsManager.setUserApiKey(it)
                },
                visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                shape = RoundedCornerShape(Radius.control),
                singleLine = true,
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
                    Text("AIzaSy...", color = TextLow, style = MaterialTheme.typography.bodyMedium)
                }
            )

            Spacer(Modifier.height(Space.md))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (showKeyText) "Hide key" else "Show key",
                    color = TextMid,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { showKeyText = !showKeyText }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.control))
                        .background(Raised)
                        .clickable(enabled = !isTestingConnection) {
                            isTestingConnection = true
                            testResult = null
                            scope.launch {
                                val keyToTest = prefsManager.getEffectiveApiKey()
                                val res = GeminiApiClient.testConnection(keyToTest, selectedModel)
                                isTestingConnection = false
                                res.onSuccess { msg -> testResult = Pair(true, msg) }
                                    .onFailure { err ->
                                        testResult = Pair(false, err.message ?: "Could not reach Gemini.")
                                    }
                            }
                        }
                        .padding(horizontal = Space.lg, vertical = Space.md)
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            color = Accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(Space.sm))
                    }
                    Text("Test connection", color = TextHigh, style = MaterialTheme.typography.labelLarge)
                }
            }

            if (testResult != null) {
                val (isSuccess, msg) = testResult!!
                Spacer(Modifier.height(Space.md))
                Text(
                    text = msg,
                    color = if (isSuccess) Good else Alert,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(Space.xxl))

        // ---- Model -------------------------------------------------------
        SectionLabel("Model")

        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            MODELS.forEach { (modelId, blurb) ->
                val isSelected = selectedModel == modelId
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.panel))
                        .background(if (isSelected) AccentWash else Surface)
                        .clickable { prefsManager.setSelectedModel(modelId) }
                        .padding(Space.lg)
                ) {
                    Column(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Accent else Raised),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
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
                            text = modelId,
                            color = if (isSelected) Accent else TextHigh,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(text = blurb, color = TextMid, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(Space.xxl))

        // ---- Typing ------------------------------------------------------
        SectionLabel("Typing")

        Panel {
            SettingRow(
                label = "Vibrate on keypress",
                icon = Icons.Default.Vibration,
                trailing = {
                    Switch(
                        checked = isHaptic,
                        onCheckedChange = {
                            isHaptic = it
                            prefsManager.setHapticEnabled(it)
                        },
                        colors = switchColors()
                    )
                }
            )

            Hairline(Modifier.padding(vertical = Space.md))

            SettingRow(
                label = "Capitalise sentences",
                icon = Icons.Default.Keyboard,
                trailing = {
                    Switch(
                        checked = isAutoCap,
                        onCheckedChange = {
                            isAutoCap = it
                            prefsManager.setAutoCapitalizeEnabled(it)
                        },
                        colors = switchColors()
                    )
                }
            )
        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Ink,
    checkedTrackColor = Accent,
    checkedBorderColor = Accent,
    uncheckedThumbColor = TextLow,
    uncheckedTrackColor = Raised,
    uncheckedBorderColor = Line
)
