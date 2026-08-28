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
import io.github.pastdaking.rmblr.data.Provider
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SystemUpdate
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
import io.github.pastdaking.rmblr.update.UpdateRepository
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
    VOICE("Models & keys", "Who does the listening and the translating, and their keys", Icons.Default.RecordVoiceOver),
    APPEARANCE("Appearance", "Light, dark, OLED black, wallpaper colours", Icons.Default.Palette),
    TYPING("Typing", "Vibration and capitalisation", Icons.Default.Keyboard),
    UPDATES("Updates", "What version you are on, and whether there is a newer one", Icons.Default.SystemUpdate),
    FEEDBACK("Feedback", "Tell whoever builds this what is broken or missing", Icons.Default.ChatBubbleOutline)
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
                subtitle = "Set the first three once, then you can forget this page exists."
            )

            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                SettingsGroup.values().forEach { entry ->
                    GroupRow(entry) { openGroup = entry }
                }

                // The walkthrough was a one-shot: seen once, gone forever, and there was no
                // way back to it when it gained a page. recreate() is enough because the
                // seen-it flag is only read when setContent runs.
                val context = LocalContext.current
                ValueRow(
                    label = "Walkthrough",
                    value = "Show it again",
                    onClick = {
                        OnboardingPrefs(context).done = false
                        (context as? android.app.Activity)?.recreate()
                    }
                )
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
                SettingsGroup.APPEARANCE -> AppearanceSettings()
                SettingsGroup.TYPING -> TypingSettings(prefsManager)
                SettingsGroup.UPDATES -> UpdatesSettings(
                    repo = UpdateRepository.getInstance(LocalContext.current)
                )
                SettingsGroup.FEEDBACK -> FeedbackSettings(prefsManager)
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
private enum class OpenSheet { DICTATION, TRANSLATION, LANGUAGE, TARGET }

/**
 * One page for every model decision in the app.
 *
 * Dictation and translation had a page each, and they were the same page: pick a
 * provider, paste a key, pick a model. Two screens asking the identical question is a
 * sign the split was invented rather than found, so they are one screen with two rows.
 *
 * The keys underneath them are now per PROVIDER rather than per feature, which is what
 * makes the merge worth doing — one Mistral key serves the dictating and the translating,
 * and choosing a different provider never disturbs the key you already pasted for the
 * last one.
 */
@Composable
private fun VoiceSettings(prefsManager: PreferencesManager) {
    val engine by prefsManager.engineFlow.collectAsState()
    val textProvider by prefsManager.textProviderFlow.collectAsState()
    val language by prefsManager.languageFlow.collectAsState()
    val target by prefsManager.translateTargetFlow.collectAsState()
    // Re-read the keys whenever anything about them changes, so "Connected" is honest.
    val geminiKey by prefsManager.apiKeyFlow.collectAsState()
    val providerKey by prefsManager.groqKeyFlow.collectAsState()

    var sheet by remember { mutableStateOf<OpenSheet?>(null) }

    val dictationReady = prefsManager.getProviderKey(engine.provider).isNotBlank()
    val translationReady = prefsManager.getProviderKey(textProvider.provider).isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        ValueRow(
            label = "Dictation",
            value = engine.label,
            supporting = "${engine.provider.label}${if (dictationReady) "" else " — key needed"}",
            valueTint = if (dictationReady) Accent else Alert,
            onClick = { sheet = OpenSheet.DICTATION }
        )
        ValueRow(
            label = "Translation",
            value = textProvider.label,
            supporting = "${textProvider.provider.label}${if (translationReady) "" else " — key needed"}",
            valueTint = if (translationReady) Accent else Alert,
            onClick = { sheet = OpenSheet.TRANSLATION }
        )
        ValueRow(
            label = "Language you speak",
            value = language.ifBlank { "Auto" },
            onClick = { sheet = OpenSheet.LANGUAGE }
        )
        ValueRow(
            label = "Translate into",
            value = target,
            onClick = { sheet = OpenSheet.TARGET }
        )
    }

    when (sheet) {
        OpenSheet.DICTATION -> ModelSheet(
            title = "What does the listening",
            prefsManager = prefsManager,
            providers = TranscriptionEngine.values().map { it.provider }.distinct(),
            selectedProvider = engine.provider,
            modelsFor = { p -> TranscriptionEngine.values().filter { it.provider == p }
                .map { ModelChoice(it.label, it.blurb, it == engine) { prefsManager.setEngine(it) } } },
            editableModelValue = { p ->
                TranscriptionEngine.values().firstOrNull { it.provider == p && p.editableModel }
                    ?.let { prefsManager.getEngineModel(it) }
            },
            onEditableModelChange = { p, value ->
                TranscriptionEngine.values().firstOrNull { it.provider == p }
                    ?.let { prefsManager.setEngineModel(it, value) }
            },
            onDismiss = { sheet = null }
        )

        OpenSheet.TRANSLATION -> ModelSheet(
            title = "What does the translating",
            prefsManager = prefsManager,
            providers = TextProvider.values().map { it.provider }.distinct(),
            selectedProvider = textProvider.provider,
            modelsFor = { p -> TextProvider.values().filter { it.provider == p }
                .map { ModelChoice(it.label, it.blurb, it == textProvider) { prefsManager.setTextProvider(it) } } },
            editableModelValue = { p ->
                if (p.editableModel) prefsManager.getTextModel() else null
            },
            onEditableModelChange = { _, value -> prefsManager.setTextModel(value) },
            onDismiss = { sheet = null }
        )

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

/** One selectable model inside the sheet. */
private data class ModelChoice(
    val label: String,
    val blurb: String,
    val selected: Boolean,
    val select: () -> Unit
)

/**
 * Provider at the top, its key under that, its models at the bottom.
 *
 * Choosing a model used to be two trips — one sheet to pick the provider, another to
 * find the key — and switching provider left the previous provider's key sitting on
 * screen underneath a second empty box. Here the three things arrive together in the
 * order you need them, and tapping a different provider swaps the key field to that
 * provider's key rather than clearing anything: pasted keys stay pasted.
 */
@Composable
private fun ModelSheet(
    title: String,
    prefsManager: PreferencesManager,
    providers: List<Provider>,
    selectedProvider: Provider,
    modelsFor: (Provider) -> List<ModelChoice>,
    editableModelValue: (Provider) -> String?,
    onEditableModelChange: (Provider, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Which provider's shelf you are LOOKING at, which is not the same as the one in
    // use — you can browse Mistral's models without abandoning Gemini until you tap one.
    var viewing by remember(selectedProvider) { mutableStateOf(selectedProvider) }

    RmblrSheet(title, onDismiss) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            providers.forEach { p ->
                val on = viewing == p
                Text(
                    text = p.label,
                    color = if (on) OnAccent else TextMid,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .pressable(onClick = { viewing = p })
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(if (on) Accent else Raised)
                        .padding(horizontal = Space.lg, vertical = Space.md)
                )
            }
        }

        Spacer(Modifier.height(Space.lg))

        ProviderKeyField(prefsManager, viewing)

        // Only the do-it-yourself provider has an address worth typing; every other one
        // already knows where it lives.
        if (viewing == Provider.CUSTOM) {
            Spacer(Modifier.height(Space.md))
            var baseInput by remember(viewing) { mutableStateOf(prefsManager.getProviderBaseUrl(viewing)) }
            OutlinedTextField(
                value = baseInput,
                onValueChange = { baseInput = it; prefsManager.setProviderBaseUrl(viewing, it) },
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

        val editable = editableModelValue(viewing)
        if (editable != null) {
            Spacer(Modifier.height(Space.md))
            var modelInput by remember(viewing) { mutableStateOf(editable) }
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it; onEditableModelChange(viewing, it) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.control),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = fieldColors(),
                placeholder = { Text("model name", color = TextLow, style = MaterialTheme.typography.bodyMedium) }
            )
        }

        Spacer(Modifier.height(Space.md))
        Text(viewing.note, color = TextMid, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(Space.xl))
        SectionLabel("${viewing.label} models")

        SheetOptions {
            modelsFor(viewing).forEach { choice ->
                SheetOption(
                    title = choice.label,
                    supporting = choice.blurb,
                    selected = choice.selected,
                    onClick = { choice.select(); onDismiss() }
                )
            }
        }
    }
}

/** The key for one provider, entered once and used by everything that provider does. */
@Composable
private fun ProviderKeyField(prefsManager: PreferencesManager, provider: Provider) {
    val scope = rememberCoroutineScope()
    var input by remember(provider) { mutableStateOf(prefsManager.getProviderKey(provider)) }
    var reveal by remember { mutableStateOf(false) }
    var testing by remember(provider) { mutableStateOf(false) }
    var result by remember(provider) { mutableStateOf<Pair<Boolean, String>?>(null) }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it; prefsManager.setProviderKey(provider, it) },
        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
        shape = RoundedCornerShape(Radius.control),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = fieldColors(),
        placeholder = { Text(provider.keyHint, color = TextLow, style = MaterialTheme.typography.bodyMedium) }
    )

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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(Raised)
                .clickable(enabled = !testing) {
                    testing = true
                    result = null
                    scope.launch {
                        val key = prefsManager.getProviderKey(provider)
                        val res = if (provider.needsKey) {
                            SpeechToTextClient.testConnection(provider.baseUrl, key, provider.label)
                        } else {
                            GeminiApiClient.testConnection(key)
                        }
                        testing = false
                        res.onSuccess { result = true to it }
                            .onFailure { result = false to (it.message ?: "Could not reach it.") }
                    }
                }
                .padding(horizontal = Space.lg, vertical = Space.md)
        ) {
            if (testing) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(Space.sm))
            }
            Text("Test connection", color = TextHigh, style = MaterialTheme.typography.labelLarge)
        }
    }

    result?.let { (ok, msg) ->
        Spacer(Modifier.height(Space.md))
        Text(msg, color = if (ok) Good else Alert, style = MaterialTheme.typography.bodySmall)
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
