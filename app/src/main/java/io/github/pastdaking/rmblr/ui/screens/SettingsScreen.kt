package io.github.pastdaking.rmblr.ui.screens

import androidx.activity.compose.BackHandler
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.components.IconChip
import io.github.pastdaking.rmblr.ui.components.ScreenHeader
import io.github.pastdaking.rmblr.ui.components.ValueRow
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.SheetOption
import io.github.pastdaking.rmblr.ui.components.SheetOptions
import io.github.pastdaking.rmblr.ui.components.LanguageEntry
import io.github.pastdaking.rmblr.data.SUGGESTED_LANGUAGES
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
import io.github.pastdaking.rmblr.ai.SpeechToTextClient
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

/** Which sheet is open, if any. One at a time, by construction. */
private enum class OpenSheet { ENGINE, KEY, LANGUAGE, TARGET, PROVIDER, PROVIDER_KEY }

@Composable
private fun VoiceSettings(prefsManager: PreferencesManager) {
    val scope = rememberCoroutineScope()

    val userApiKey by prefsManager.apiKeyFlow.collectAsState()
    val selectedModel by prefsManager.modelFlow.collectAsState()
    val engine by prefsManager.engineFlow.collectAsState()
    val providerKey by prefsManager.groqKeyFlow.collectAsState()
    val language by prefsManager.languageFlow.collectAsState()

    var sheet by remember { mutableStateOf<OpenSheet?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Only the key the SELECTED engine actually uses is ever shown. Leaving the Gemini
    // field on screen after picking Mistral, with a second box underneath it, was the
    // most confusing thing on this page.
    val usesOwnKey = engine.needsGroqKey
    val keyInUse = if (usesOwnKey) providerKey else prefsManager.getEffectiveApiKey()
    val keyLabel = if (usesOwnKey) "${engine.label} key" else "Gemini key"

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        ValueRow(
            label = "Engine",
            value = engine.label,
            supporting = engine.blurb,
            onClick = { sheet = OpenSheet.ENGINE }
        )
        ValueRow(
            label = keyLabel,
            value = if (keyInUse.isNotBlank()) "Connected" else "Not set",
            valueTint = if (keyInUse.isNotBlank()) Good else Accent,
            onClick = { sheet = OpenSheet.KEY }
        )
        ValueRow(
            label = "Language you speak",
            value = language.ifBlank { "Auto" },
            supporting = "Leave it on auto unless you are being transcribed into the wrong language.",
            onClick = { sheet = OpenSheet.LANGUAGE }
        )
    }

    when (sheet) {
        OpenSheet.ENGINE -> RmblrSheet("What does the listening", { sheet = null }) {
            SheetOptions {
                TranscriptionEngine.values().forEach { option ->
                    SheetOption(
                        title = option.label,
                        supporting = option.blurb,
                        selected = engine == option,
                        onClick = {
                            prefsManager.setEngine(option)
                            testResult = null
                            sheet = null
                        }
                    )
                }
            }
        }

        OpenSheet.KEY -> RmblrSheet(keyLabel, { sheet = null }) {
            KeySheet(
                key = keyInUse,
                onKeyChange = {
                    if (usesOwnKey) prefsManager.setEngineKey(engine, it) else prefsManager.setUserApiKey(it)
                },
                placeholder = if (usesOwnKey) "${engine.label} API key" else "AIzaSy...",
                model = if (engine.editableModel) prefsManager.getEngineModel(engine) else null,
                modelPlaceholder = engine.id,
                onModelChange = { prefsManager.setEngineModel(engine, it) },
                note = keyNoteFor(engine),
                testing = isTesting,
                result = testResult,
                onTest = {
                    isTesting = true
                    testResult = null
                    scope.launch {
                        val res = if (usesOwnKey) {
                            SpeechToTextClient.testConnection(engine.baseUrl, prefsManager.getEngineKey(engine), engine.label)
                        } else {
                            GeminiApiClient.testConnection(prefsManager.getEffectiveApiKey(), selectedModel)
                        }
                        isTesting = false
                        res.onSuccess { testResult = true to it }
                            .onFailure { testResult = false to (it.message ?: "Could not reach it.") }
                    }
                }
            )
        }

        OpenSheet.LANGUAGE -> RmblrSheet("Language you speak", { sheet = null }) {
            LanguageEntry(
                value = language,
                onValueChange = { prefsManager.setSpokenLanguage(it) },
                suggestions = SUGGESTED_LANGUAGES,
                placeholder = "Leave blank for auto"
            )
            Spacer(Modifier.height(Space.lg))
            Text(
                text = "Type any language — it is only a name passed to the model, so it does not " +
                    "have to be on anyone's supported list. Blank means work it out.",
                color = TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        }

        else -> Unit
    }
}

private fun keyNoteFor(engine: TranscriptionEngine): String = when (engine) {
    TranscriptionEngine.GROQ ->
        "Free from console.groq.com. Groq runs Whisper: quick and accurate in one language, " +
            "but the model that struggles when you switch languages mid-sentence."
    TranscriptionEngine.MISTRAL_VOXTRAL ->
        "From console.mistral.ai. Voxtral is Mistral's own transcriber, strong across European " +
            "languages. Leave the model blank for voxtral-mini-latest."
    TranscriptionEngine.OPENROUTER_STT ->
        "From openrouter.ai. One key reaches Voxtral, GPT-4o Transcribe, Whisper and the rest — " +
            "name whichever you want, or leave it blank for the default."
    else ->
        "Free from Google AI Studio. The key is stored on this device and calls Google directly."
}

/** The key sheet, shared by dictation and translation because they need the same four things. */
@Composable
private fun KeySheet(
    key: String,
    onKeyChange: (String) -> Unit,
    placeholder: String,
    model: String?,
    modelPlaceholder: String,
    onModelChange: (String) -> Unit,
    note: String,
    testing: Boolean,
    result: Pair<Boolean, String>?,
    onTest: (() -> Unit)?
) {
    var input by remember(key) { mutableStateOf(key) }
    var modelInput by remember(model) { mutableStateOf(model.orEmpty()) }
    var reveal by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it; onKeyChange(it) },
        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
        shape = RoundedCornerShape(Radius.control),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = fieldColors(),
        placeholder = { Text(placeholder, color = TextLow, style = MaterialTheme.typography.bodyMedium) }
    )

    if (model != null) {
        Spacer(Modifier.height(Space.md))
        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it; onModelChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.control),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = fieldColors(),
            placeholder = { Text(modelPlaceholder, color = TextLow, style = MaterialTheme.typography.bodyMedium) }
        )
    }

    Spacer(Modifier.height(Space.md))

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (reveal) "Hide key" else "Show key",
            color = TextMid,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { reveal = !reveal }
        )

        if (onTest != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Raised)
                    .clickable(enabled = !testing, onClick = onTest)
                    .padding(horizontal = Space.lg, vertical = Space.md)
            ) {
                if (testing) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(Space.sm))
                }
                Text("Test connection", color = TextHigh, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    result?.let { (ok, msg) ->
        Spacer(Modifier.height(Space.md))
        Text(msg, color = if (ok) Good else Alert, style = MaterialTheme.typography.bodySmall)
    }

    Spacer(Modifier.height(Space.lg))
    Text(note, color = TextMid, style = MaterialTheme.typography.bodySmall)
}

// ---------------------------------------------------------------- translation

@Composable
private fun TranslationSettings(prefsManager: PreferencesManager) {
    val target by prefsManager.translateTargetFlow.collectAsState()
    val provider by prefsManager.textProviderFlow.collectAsState()
    val textKey by prefsManager.textKeyFlow.collectAsState()

    var sheet by remember { mutableStateOf<OpenSheet?>(null) }
    var baseUrlInput by remember(provider) { mutableStateOf(prefsManager.getTextBaseUrl()) }

    val keyInUse = if (provider.usesGeminiKey) prefsManager.getEffectiveApiKey() else textKey

    Text(
        text = "Switching translation on lives on the main page. This is where you say who does it.",
        color = TextMid,
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(Modifier.height(Space.xl))

    // Deliberately the same three rows, in the same order, as Models & keys. The two
    // pages answer the same shape of question and should not look like different apps.
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        ValueRow(
            label = "Provider",
            value = provider.label,
            supporting = provider.blurb,
            onClick = { sheet = OpenSheet.PROVIDER }
        )
        ValueRow(
            label = if (provider.usesGeminiKey) "Gemini key" else "${provider.label} key",
            value = if (keyInUse.isNotBlank()) "Connected" else "Not set",
            valueTint = if (keyInUse.isNotBlank()) Good else Accent,
            onClick = { sheet = OpenSheet.PROVIDER_KEY }
        )
        ValueRow(
            label = "Translate into",
            value = target,
            onClick = { sheet = OpenSheet.TARGET }
        )
    }

    when (sheet) {
        OpenSheet.PROVIDER -> RmblrSheet("Which model translates", { sheet = null }) {
            SheetOptions {
                TextProvider.values().forEach { option ->
                    SheetOption(
                        title = option.label,
                        supporting = option.blurb,
                        selected = provider == option,
                        onClick = { prefsManager.setTextProvider(option); sheet = null }
                    )
                }
            }
        }

        OpenSheet.PROVIDER_KEY -> RmblrSheet(
            if (provider.usesGeminiKey) "Gemini key" else "${provider.label} key",
            { sheet = null }
        ) {
            KeySheet(
                key = keyInUse,
                onKeyChange = {
                    if (provider.usesGeminiKey) prefsManager.setUserApiKey(it) else prefsManager.setTextApiKey(it)
                },
                placeholder = if (provider.usesGeminiKey) "AIzaSy..." else "${provider.label} API key",
                model = if (provider.editable) prefsManager.getTextModel() else null,
                modelPlaceholder = provider.defaultModel.ifBlank { "model name" },
                onModelChange = { prefsManager.setTextModel(it) },
                note = if (provider.editable)
                    "Anything speaking the OpenAI chat-completions API works, including a server on " +
                        "your own machine. Give the base URL up to and including /v1."
                else provider.blurb,
                testing = false,
                result = null,
                onTest = null
            )

            if (provider.editable) {
                Spacer(Modifier.height(Space.md))
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it; prefsManager.setTextBaseUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.control),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = fieldColors(),
                    placeholder = {
                        Text("https://api.example.com/v1", color = TextLow, style = MaterialTheme.typography.bodyMedium)
                    }
                )
            }
        }

        OpenSheet.TARGET -> RmblrSheet("Translate into", { sheet = null }) {
            LanguageEntry(
                value = target,
                onValueChange = { prefsManager.setTranslateTarget(it) },
                suggestions = SUGGESTED_LANGUAGES,
                placeholder = "English"
            )
        }

        else -> Unit
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
