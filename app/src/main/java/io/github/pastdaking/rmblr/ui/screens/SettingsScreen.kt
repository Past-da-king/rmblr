package io.github.pastdaking.rmblr.ui.screens

import androidx.activity.compose.BackHandler
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.components.IconChip
import io.github.pastdaking.rmblr.ui.components.ScreenHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ai.GeminiApiClient
import io.github.pastdaking.rmblr.ai.GroqApiClient
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.SPOKEN_LANGUAGES
import io.github.pastdaking.rmblr.data.TARGET_LANGUAGES
import io.github.pastdaking.rmblr.data.TextProvider
import io.github.pastdaking.rmblr.data.TranscriptionEngine
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.Radius
import io.github.pastdaking.rmblr.ui.components.SectionLabel
import io.github.pastdaking.rmblr.ui.components.SettingRow
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.DYNAMIC_SUPPORTED
import io.github.pastdaking.rmblr.ui.theme.Good
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import io.github.pastdaking.rmblr.ui.theme.ThemeChoice
import io.github.pastdaking.rmblr.ui.theme.ThemePrefs
import kotlinx.coroutines.launch

/**
 * Settings, in four rooms rather than one corridor.
 *
 * This page had grown to nine sections on a single scroll, so an API key sat directly
 * above a colour picker and neither was findable. Everything below is the same set of
 * controls; the only change is that you now choose a subject first and see four things
 * instead of forty. Translation's on-switch is deliberately NOT here — turning it on is
 * something you do, not something you configure, so it lives on the main page with the
 * rest of the orb's behaviour. What stays here is the part that is genuinely
 * configuration: which model does it, with which key, into which language.
 */
private enum class SettingsGroup(
    val label: String,
    val blurb: String,
    val icon: ImageVector
) {
    VOICE("Models & keys", "What does the listening, and the keys it uses", Icons.Default.RecordVoiceOver),
    TRANSLATION("Translation", "Which model translates, and into what", Icons.Default.Translate),
    APPEARANCE("Appearance", "Light, dark, OLED black, wallpaper colours", Icons.Default.Palette),
    TYPING("Typing", "Vibration and capitalisation", Icons.Default.Keyboard)
}

@Composable
fun SettingsScreen(
    prefsManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    var openGroup by remember { mutableStateOf<SettingsGroup?>(null) }

    // Back should climb one level before it leaves the screen, which is what anyone
    // expects of a settings page with rooms in it.
    BackHandler(enabled = openGroup != null) { openGroup = null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.xl, bottom = Space.navClear)
            .testTag("settings_screen")
    ) {
        val group = openGroup

        if (group == null) {
            ScreenHeader(
                title = "Settings",
                subtitle = "Four things to set, then you can forget this page exists."
            )

            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                SettingsGroup.values().forEach { entry ->
                    GroupRow(entry) { openGroup = entry }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.control))
                    .clickable { openGroup = null }
                    .padding(vertical = Space.sm)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to settings",
                    tint = TextMid,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(Space.md))
                Text(group.label, color = TextHigh, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(Space.xl))

            when (group) {
                SettingsGroup.VOICE -> VoiceSettings(prefsManager)
                SettingsGroup.TRANSLATION -> TranslationSettings(prefsManager)
                SettingsGroup.APPEARANCE -> AppearanceSettings()
                SettingsGroup.TYPING -> TypingSettings(prefsManager)
            }
        }
    }
}

@Composable
private fun GroupRow(group: SettingsGroup, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.panel))
            .background(Surface)
            .padding(Space.lg)
    ) {
        IconChip(group.icon)
        Spacer(Modifier.width(Space.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.label, color = TextHigh, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(group.blurb, color = TextMid, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextLow,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------- models & keys

@Composable
private fun VoiceSettings(prefsManager: PreferencesManager) {
    val scope = rememberCoroutineScope()

    val userApiKey by prefsManager.apiKeyFlow.collectAsState()
    val selectedModel by prefsManager.modelFlow.collectAsState()
    val engine by prefsManager.engineFlow.collectAsState()
    val groqKey by prefsManager.groqKeyFlow.collectAsState()
    val languageCode by prefsManager.languageFlow.collectAsState()

    var inputApiKey by remember(userApiKey) { mutableStateOf(prefsManager.getUserApiKey()) }
    var inputGroqKey by remember(groqKey) { mutableStateOf(groqKey) }
    var showKeyText by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val hasKey = prefsManager.getEffectiveApiKey().isNotBlank()

    SectionLabel("What does the listening")

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        TranscriptionEngine.values().forEach { option ->
            ChoiceRow(
                title = option.label,
                blurb = option.blurb,
                selected = engine == option,
                onClick = { prefsManager.setEngine(option) }
            )
        }
    }

    Spacer(Modifier.height(Space.xxl))

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
            colors = fieldColors(),
            placeholder = { Text("AIzaSy...", color = TextLow, style = MaterialTheme.typography.bodyMedium) }
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
                    .clickable(enabled = !isTesting) {
                        isTesting = true
                        testResult = null
                        scope.launch {
                            val res = if (engine.needsGroqKey) {
                                GroqApiClient.testConnection(prefsManager.getGroqApiKey())
                            } else {
                                GeminiApiClient.testConnection(prefsManager.getEffectiveApiKey(), selectedModel)
                            }
                            isTesting = false
                            res.onSuccess { msg -> testResult = true to msg }
                                .onFailure { err -> testResult = false to (err.message ?: "Could not reach it.") }
                        }
                    }
                    .padding(horizontal = Space.lg, vertical = Space.md)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(Space.sm))
                }
                Text("Test connection", color = TextHigh, style = MaterialTheme.typography.labelLarge)
            }
        }

        testResult?.let { (ok, msg) ->
            Spacer(Modifier.height(Space.md))
            Text(msg, color = if (ok) Good else Alert, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (engine.needsGroqKey) {
        Spacer(Modifier.height(Space.xxl))
        SectionLabel("Groq key")
        Panel {
            OutlinedTextField(
                value = inputGroqKey,
                onValueChange = {
                    inputGroqKey = it
                    prefsManager.setGroqApiKey(it)
                },
                visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("groq_key_input"),
                shape = RoundedCornerShape(Radius.control),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = fieldColors(),
                placeholder = { Text("gsk_...", color = TextLow, style = MaterialTheme.typography.bodyMedium) }
            )
            Spacer(Modifier.height(Space.md))
            Text(
                text = "Free from console.groq.com. Groq runs Whisper, which is quick and accurate " +
                    "in one language but is the model that struggles when you switch languages " +
                    "mid-sentence — the reason this app exists. Keep Gemini selected if that is you.",
                color = TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    Spacer(Modifier.height(Space.xxl))

    SectionLabel("Language you speak")
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        SPOKEN_LANGUAGES.forEach { language ->
            Chip(
                label = language.label,
                selected = languageCode == language.code,
                onClick = { prefsManager.setLanguageCode(language.code) }
            )
        }
    }
    Spacer(Modifier.height(Space.md))
    Text(
        text = "Leave this on auto unless you are being transcribed into the wrong language. The " +
            "list is a hint, not a limit: anything Gemini can hear works, including languages " +
            "that are not in its documented list, and mixing two of them in one sentence is the " +
            "case this app was built for.",
        color = TextMid,
        style = MaterialTheme.typography.bodySmall
    )
}

// ---------------------------------------------------------------- translation

@Composable
private fun TranslationSettings(prefsManager: PreferencesManager) {
    val translateTarget by prefsManager.translateTargetFlow.collectAsState()
    val textProvider by prefsManager.textProviderFlow.collectAsState()
    val textKey by prefsManager.textKeyFlow.collectAsState()

    var showKeyText by remember { mutableStateOf(false) }
    var inputTextKey by remember(textProvider, textKey) {
        mutableStateOf(if (textProvider.usesGeminiKey) "" else textKey)
    }
    var inputBaseUrl by remember(textProvider) { mutableStateOf(prefsManager.getTextBaseUrl()) }
    var inputTextModel by remember(textProvider) { mutableStateOf(prefsManager.getTextModel()) }

    Text(
        text = "Switching translation on lives on the main page. This is where you say who does it.",
        color = TextMid,
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(Modifier.height(Space.xl))

    SectionLabel("Translate into")
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        TARGET_LANGUAGES.forEach { language ->
            Chip(
                label = language.label,
                selected = translateTarget == language.code,
                onClick = { prefsManager.setTranslateTargetCode(language.code) }
            )
        }
    }

    Spacer(Modifier.height(Space.xxl))

    SectionLabel("Which model translates")
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        TextProvider.values().forEach { option ->
            ChoiceRow(
                title = option.label,
                blurb = option.blurb,
                selected = textProvider == option,
                onClick = { prefsManager.setTextProvider(option) }
            )
        }
    }

    if (!textProvider.usesGeminiKey) {
        Spacer(Modifier.height(Space.lg))
        Panel {
            OutlinedTextField(
                value = inputTextKey,
                onValueChange = {
                    inputTextKey = it
                    prefsManager.setTextApiKey(it)
                },
                visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("text_key_input"),
                shape = RoundedCornerShape(Radius.control),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = fieldColors(),
                placeholder = {
                    Text("${textProvider.label} API key", color = TextLow, style = MaterialTheme.typography.bodyMedium)
                }
            )

            Spacer(Modifier.height(Space.sm))
            Text(
                text = if (showKeyText) "Hide key" else "Show key",
                color = TextMid,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable { showKeyText = !showKeyText }
            )

            if (textProvider.editable) {
                Spacer(Modifier.height(Space.md))
                OutlinedTextField(
                    value = inputBaseUrl,
                    onValueChange = {
                        inputBaseUrl = it
                        prefsManager.setTextBaseUrl(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.control),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = fieldColors(),
                    placeholder = {
                        Text("https://api.example.com/v1", color = TextLow, style = MaterialTheme.typography.bodyMedium)
                    }
                )

                Spacer(Modifier.height(Space.md))
                OutlinedTextField(
                    value = inputTextModel,
                    onValueChange = {
                        inputTextModel = it
                        prefsManager.setTextModel(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.control),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = fieldColors(),
                    placeholder = { Text("model name", color = TextLow, style = MaterialTheme.typography.bodyMedium) }
                )

                Spacer(Modifier.height(Space.md))
                Text(
                    text = "Anything that speaks the OpenAI chat-completions API works here, " +
                        "including a server running on your own machine. Give the base URL up to " +
                        "and including /v1.",
                    color = TextMid,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ---------------------------------------------------------------- appearance

@Composable
private fun AppearanceSettings() {
    val context = LocalContext.current
    val themePrefs = remember { ThemePrefs(context) }

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        ThemeChoice.values().forEach { option ->
            ChoiceRow(
                title = option.label,
                blurb = option.blurb,
                selected = themePrefs.choice == option,
                onClick = { themePrefs.choice = option }
            )
        }
    }

    if (DYNAMIC_SUPPORTED) {
        Spacer(Modifier.height(Space.lg))
        Panel {
            SettingRow(
                label = "Match my wallpaper",
                icon = Icons.Default.Palette,
                trailing = {
                    Switch(
                        checked = themePrefs.dynamic,
                        onCheckedChange = { themePrefs.dynamic = it },
                        colors = switchColors()
                    )
                }
            )
            Spacer(Modifier.height(Space.md))
            Text(
                text = "Material You: the app takes its colours from your wallpaper, the way " +
                    "Android's own apps do. OLED black keeps its true black background and " +
                    "borrows only the accent.",
                color = TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ---------------------------------------------------------------- typing

@Composable
private fun TypingSettings(prefsManager: PreferencesManager) {
    var isHaptic by remember { mutableStateOf(prefsManager.isHapticEnabled()) }
    var isAutoCap by remember { mutableStateOf(prefsManager.isAutoCapitalizeEnabled()) }

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

// ---------------------------------------------------------------- shared bits

/** One pickable option: a tick, a name, and the honest catch underneath it. */
@Composable
private fun ChoiceRow(
    title: String,
    blurb: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.panel))
            .background(if (selected) AccentWash else Surface)
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
            Text(text = blurb, color = TextMid, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) OnAccent else TextMid,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (selected) Accent else Raised)
            .padding(horizontal = Space.lg, vertical = Space.md)
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    focusedTextColor = TextHigh,
    unfocusedTextColor = TextHigh,
    cursorColor = Accent,
    focusedContainerColor = Raised,
    unfocusedContainerColor = Raised
)

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Ink,
    checkedTrackColor = Accent,
    checkedBorderColor = Accent,
    uncheckedThumbColor = TextLow,
    uncheckedTrackColor = Raised,
    uncheckedBorderColor = Line
)
