package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.BuildConfig
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.Provider
import io.github.pastdaking.rmblr.data.TextProvider
import io.github.pastdaking.rmblr.data.TranscriptionEngine
import io.github.pastdaking.rmblr.update.UpdateRepository
import io.github.pastdaking.rmblr.update.UpdateStatus
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.ScreenHeader
import io.github.pastdaking.rmblr.ui.components.SheetOption
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.copyToClipboard
import io.github.pastdaking.rmblr.ui.components.openUrl
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.AssistChip
import io.github.pastdaking.rmblr.ui.m3.ExpressiveShapes
import io.github.pastdaking.rmblr.ui.m3.GroupedRow
import io.github.pastdaking.rmblr.ui.m3.M3Chip
import io.github.pastdaking.rmblr.ui.m3.RadialShape
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.update.GitHubReleases
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.DYNAMIC_SUPPORTED
import io.github.pastdaking.rmblr.ui.theme.Good
import io.github.pastdaking.rmblr.ui.theme.GoodWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.OnGoodWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.RaisedHighest
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.Teal
import io.github.pastdaking.rmblr.ui.theme.TealWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import io.github.pastdaking.rmblr.ui.theme.ThemeChoice
import io.github.pastdaking.rmblr.ui.theme.ThemePrefs
import kotlinx.coroutines.launch

/** Sheets the Settings page can have open. One at a time, by construction. */
private sealed interface SettingsSheet {
    data class Key(val provider: Provider) : SettingsSheet
    data class Engines(val provider: Provider) : SettingsSheet
    data object AllEngineProviders : SettingsSheet
    data object TextModel : SettingsSheet
    data object Permissions : SettingsSheet
    data object MoreOrb : SettingsSheet
    data class Available(val release: io.github.pastdaking.rmblr.update.ReleaseInfo) : SettingsSheet
    data class WhatsNew(val entry: io.github.pastdaking.rmblr.update.ChangelogEntry) : SettingsSheet
}

/** The providers that can do the listening, in the order the chips show them. */
fun listeningProviders(): List<Provider> =
    TranscriptionEngine.values().map { it.provider }.distinct()

/** The name of an engine without the sales word on the end. */
fun engineName(engine: TranscriptionEngine): String = engine.label.substringBefore(" — ")

/** One short line for the engine card; the long one lives in the engine sheet. */
fun engineTagline(engine: TranscriptionEngine): String =
    if (engine.streams) "Words appear while you speak" else "Sent when you stop talking"

/**
 * Settings, as one page you scroll rather than a corridor of rooms.
 *
 * Everything that was in the old rooms is here and stores exactly what it stored before:
 * engines and models, one key per provider, languages, orb behaviour, theme, typing and
 * updates. What changed is that you can see what is set, and the keys carry a status you
 * can trust because it comes from a real call.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    prefsManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val repo = remember { UpdateRepository.getInstance(context) }
    val checks = remember { KeyChecks(context) }
    val permissions by rememberPermissionState()

    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var showFeedback by remember { mutableStateOf(false) }

    val engine by prefsManager.engineFlow.collectAsState()
    val textProvider by prefsManager.textProviderFlow.collectAsState()
    // Keys change through two flows depending on which provider; either one means re-read.
    val keyTick1 by prefsManager.apiKeyFlow.collectAsState()
    val keyTick2 by prefsManager.groqKeyFlow.collectAsState()
    val keyTick3 by prefsManager.textKeyFlow.collectAsState()
    var keyVersion by remember { mutableStateOf(0) }
    val updateStatus by repo.status.collectAsState()
    val checking by repo.checking.collectAsState()
    val autoCheck by repo.autoCheck.collectAsState()

    val testing = remember { mutableStateMapOf<Provider, Boolean>() }
    val failures = remember { mutableStateMapOf<Provider, String>() }

    LaunchedEffect(Unit) { repo.check(force = false) }

    BackHandler(enabled = showFeedback) { showFeedback = false }

    if (showFeedback) {
        FeedbackScreen(prefsManager = prefsManager, onBack = { showFeedback = false }, modifier = modifier)
        return
    }

    val collapsed by remember { derivedStateOf { scroll.value > 260 } }

    Box(modifier.fillMaxSize().background(Ink).testTag("settings_screen")) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Space.xl),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = Space.lg)
                .padding(top = Space.xl, bottom = Space.navClear)
        ) {
            ScreenHeader(
                title = "Settings",
                subtitle = "Set the first three once, then you can forget this page exists.",
                modifier = Modifier.padding(horizontal = Space.sm),
                trailing = {
                    ShapedIcon(Icons.Default.Settings, ExpressiveShapes.Gem, AccentWash, OnAccentWash, size = 64.dp)
                }
            )

            // ----------------------------------------------------------- listening
            Section {
                SectionHeader(Icons.Default.Mic, ExpressiveShapes.Cookie9, AccentWash, OnAccentWash, "Listening", subtitle = "Who turns your voice into words")
                val providers = listeningProviders()
                val firstThree = providers.take(3)
                val extra = if (engine.provider in firstThree) null else engine.provider
                TonalCard(padding = PaddingValues(Space.lg)) {
                    Text("Speech engine", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        firstThree.forEach { p ->
                            SegmentChip(p.label, engine.provider == p, { chooseListeningProvider(prefsManager, p) }, Modifier.weight(if (engine.provider == p) 1.45f else 1f))
                        }
                        SegmentChip(
                            label = extra?.label ?: "More",
                            selected = extra != null,
                            onClick = { sheet = SettingsSheet.AllEngineProviders },
                            modifier = Modifier.weight(if (extra != null) 1.45f else 1f)
                        )
                    }
                    Spacer(Modifier.height(Space.md))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(pressedScale = 0.99f) { sheet = SettingsSheet.Engines(engine.provider) }
                            .clip(RoundedCornerShape(20.dp))
                            .background(RaisedHigh)
                            .padding(Space.lg)
                            .testTag("engine_card")
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(engineName(engine), color = TextHigh, style = MaterialTheme.typography.titleMedium)
                            Text(engineTagline(engine), color = TextMid, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (engine.streams) {
                            InfoPill("Streams", icon = Icons.Default.Bolt)
                        } else {
                            Icon(Icons.Default.ChevronRight, null, tint = TextMid)
                        }
                    }
                    if (prefsManager.getProviderKey(engine.provider).isBlank()) {
                        Spacer(Modifier.height(Space.md))
                        KeyNeededLine(engine.provider) { sheet = SettingsSheet.Key(engine.provider) }
                    }
                }
            }

            // ----------------------------------------------------------- rewriting
            Section {
                SectionHeader(Icons.Default.AutoFixHigh, ExpressiveShapes.Clover4, TealWash, Teal, "Rewriting", subtitle = "Who applies your tones and translates")
                TonalCard(padding = PaddingValues(Space.lg)) {
                    Text("Text provider", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.md))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        verticalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        TextProvider.values().forEach { tp ->
                            M3Chip(
                                label = if (tp == TextProvider.CUSTOM) "Custom" else tp.label,
                                selected = tp == textProvider,
                                onClick = { prefsManager.setTextProvider(tp) }
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.md))
                    val model = remember(textProvider, keyVersion) { prefsManager.getTextModel() }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (textProvider.provider.editableModel) Modifier.pressable(pressedScale = 0.99f) { sheet = SettingsSheet.TextModel } else Modifier)
                            .padding(horizontal = Space.sm, vertical = Space.sm)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Model", color = TextMid, style = MaterialTheme.typography.labelLarge)
                            Text(model.ifBlank { "Provider default" }, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                        }
                        if (textProvider.provider.editableModel) {
                            Icon(Icons.Default.Edit, "Change model", tint = TextHigh)
                        }
                    }
                    if (prefsManager.getProviderKey(textProvider.provider).isBlank()) {
                        Spacer(Modifier.height(Space.sm))
                        KeyNeededLine(textProvider.provider) { sheet = SettingsSheet.Key(textProvider.provider) }
                    }
                }
            }

            // ----------------------------------------------------------- API keys
            Section {
                SectionHeader(Icons.Default.Key, ExpressiveShapes.Flower, GoodWash, OnGoodWash, "API keys", subtitle = "Stored on this phone only. One key per provider.")
                val rows = listOf(Provider.GEMINI, Provider.GROQ, Provider.MISTRAL, Provider.OPENROUTER, Provider.OPENAI)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEachIndexed { index, provider ->
                        val key = remember(keyVersion, keyTick1, keyTick2, keyTick3, provider) { prefsManager.getProviderKey(provider) }
                        val status = keyStatusOf(provider, key, checks, failures[provider])
                        KeyRow(
                            index = index,
                            count = rows.size + 1,
                            provider = provider,
                            key = key,
                            status = status,
                            testing = testing[provider] == true,
                            onOpen = { sheet = SettingsSheet.Key(provider) },
                            onTest = {
                                testing[provider] = true
                                failures.remove(provider)
                                scope.launch {
                                    val outcome = KeyTester.test(provider, key, prefsManager.getProviderBaseUrl(provider))
                                    if (outcome.ok) checks.passed(provider, key) else failures[provider] = outcome.message
                                    testing[provider] = false
                                    keyVersion++
                                }
                            }
                        )
                    }
                    GroupedRow(rows.size, rows.size + 1, onClick = { sheet = SettingsSheet.Key(Provider.CUSTOM) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            ProviderMark(Provider.CUSTOM)
                            Spacer(Modifier.width(Space.md))
                            Column(Modifier.weight(1f)) {
                                Text(Provider.CUSTOM.label, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (prefsManager.getProviderKey(Provider.CUSTOM).isBlank()) "Your own base URL and model" else maskKey(prefsManager.getProviderKey(Provider.CUSTOM)),
                                    color = TextMid,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextHigh)
                        }
                    }
                }
            }

            // ----------------------------------------------------------- languages
            Section {
                SectionHeader(Icons.Default.Translate, ExpressiveShapes.Puffy, TealWash, Teal, "Languages", subtitle = "What you speak")
                LanguagesSection(prefsManager)
            }

            // ----------------------------------------------------------- orb
            Section {
                SectionHeader(Icons.Default.Circle, ExpressiveShapes.Cookie7, AccentWash, OnAccentWash, "Orb behaviour", subtitle = "How the floating orb acts")
                OrbBehaviour(
                    prefs = prefsManager,
                    permissions = permissions,
                    onOpenPermissions = { sheet = SettingsSheet.Permissions },
                    onOpenMore = { sheet = SettingsSheet.MoreOrb }
                )
            }

            // ----------------------------------------------------------- appearance
            Section {
                SectionHeader(Icons.Default.Palette, ExpressiveShapes.Gem, AccentWash, OnAccentWash, "Appearance", subtitle = "Colour and brightness")
                AppearanceCard()
            }

            // ----------------------------------------------------------- privacy
            PrivacyCard()

            // ----------------------------------------------------------- typing
            Section {
                SectionHeader(Icons.Default.Keyboard, ExpressiveShapes.Gem, AccentWash, OnAccentWash, "Typing", subtitle = "Small touches")
                var haptic by remember { mutableStateOf(prefsManager.isHapticEnabled()) }
                var capitalise by remember { mutableStateOf(prefsManager.isAutoCapitalizeEnabled()) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToggleCard("Vibrate on tap", haptic, { haptic = it; prefsManager.setHapticEnabled(it) }, corner = 28.dp)
                    ToggleCard("Capitalise sentences", capitalise, { capitalise = it; prefsManager.setAutoCapitalizeEnabled(it) }, corner = 28.dp)
                }
            }

            // ----------------------------------------------------------- about
            Section {
                SectionHeader(Icons.Default.Info, ExpressiveShapes.Flower, AccentWash, OnAccentWash, "About")
                AboutCard(
                    version = repo.installedVersion,
                    status = updateStatus,
                    checking = checking
                )
                val available = (updateStatus as? UpdateStatus.Available)?.release
                val notes = repo.currentNotes()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val items = buildList<AboutItem> {
                        add(AboutItem(
                            icon = Icons.Default.SystemUpdate,
                            title = if (checking) "Checking…" else if (available != null) "RMBLR ${available.version} is out" else "Check for updates",
                            supporting = null,
                            onClick = {
                                if (available != null) sheet = SettingsSheet.Available(available)
                                else scope.launch {
                                    val result = repo.check(force = true)
                                    (result as? UpdateStatus.Available)?.let { sheet = SettingsSheet.Available(it.release) }
                                }
                            }
                        ))
                        if (notes != null) add(AboutItem(Icons.Default.AutoAwesome, "What is new", notes.headline) { sheet = SettingsSheet.WhatsNew(notes) })
                        add(AboutItem(Icons.Default.Replay, "Show the walkthrough again", null) {
                            OnboardingPrefs(context).done = false
                            (context as? android.app.Activity)?.recreate()
                        })
                        add(AboutItem(Icons.Default.ChatBubbleOutline, "Send feedback", "A bug, an idea or a question") { showFeedback = true })
                        add(AboutItem(Icons.Default.OpenInNew, "All releases", "Every version, on GitHub") {
                            if (!openUrl(context, GitHubReleases.RELEASES_URL)) copyToClipboard(context, "Releases", GitHubReleases.RELEASES_URL)
                        })
                    }
                    items.forEachIndexed { i, item ->
                        GroupedRow(i, items.size + 1, onClick = item.onClick) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(item.icon, null, tint = TextHigh, modifier = Modifier.padding(horizontal = 6.dp))
                                Spacer(Modifier.width(Space.md))
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                                    if (item.supporting != null) {
                                        Text(item.supporting, color = TextMid, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = TextHigh)
                            }
                        }
                    }
                    GroupedRow(items.size, items.size + 1) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                                Text("Check automatically", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                                Text("Once a day, and tells you when there is something new", color = TextMid, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.width(Space.md))
                            TickSwitch(autoCheck, { repo.setAutoCheck(it) })
                        }
                    }
                }
            }
        }

        // The compact bar the large title becomes once it has scrolled away.
        AnimatedVisibility(visible = collapsed, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopStart)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = Space.lg, vertical = Space.md)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to the top",
                    tint = TextHigh,
                    modifier = Modifier.size(24.dp).pressable(onClick = { scope.launch { scroll.animateScrollTo(0) } })
                )
                Spacer(Modifier.width(Space.lg))
                Text("Settings", color = TextHigh, style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    // ---------------------------------------------------------------- sheets
    when (val open = sheet) {
        is SettingsSheet.Key -> ApiKeySheet(
            provider = open.provider,
            prefs = prefsManager,
            checks = checks,
            onDismiss = { sheet = null },
            onSaved = { failures.remove(open.provider); keyVersion++ }
        )

        is SettingsSheet.Engines -> EngineSheet(
            provider = open.provider,
            prefs = prefsManager,
            onAddKey = { sheet = SettingsSheet.Key(open.provider) },
            onDismiss = { sheet = null }
        )

        SettingsSheet.AllEngineProviders -> RmblrSheet(title = "Speech engine", onDismiss = { sheet = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                listeningProviders().forEach { p ->
                    SheetOption(
                        title = p.label,
                        supporting = if (prefsManager.getProviderKey(p).isBlank() && p.needsKey) "Key needed" else "Key saved",
                        selected = engine.provider == p,
                        onClick = { chooseListeningProvider(prefsManager, p); sheet = SettingsSheet.Engines(p) }
                    )
                }
            }
        }

        SettingsSheet.TextModel -> TextModelSheet(prefsManager, onDismiss = { sheet = null; keyVersion++ })

        SettingsSheet.Permissions -> RmblrSheet(title = "Permissions", onDismiss = { sheet = null }) {
            val action = rememberPermissionAction()
            PermissionCards(permissions, action)
        }

        SettingsSheet.MoreOrb -> MoreOrbOptionsSheet(prefsManager, onDismiss = { sheet = null })

        is SettingsSheet.Available -> UpdateAvailableSheet(
            release = open.release,
            onDownload = {
                io.github.pastdaking.rmblr.update.UpdateNotifier.dismiss(context)
                if (!openUrl(context, open.release.downloadUrl)) copyToClipboard(context, "RMBLR download", open.release.downloadUrl)
                sheet = null
            },
            onSkip = {
                repo.skip(open.release.version)
                io.github.pastdaking.rmblr.update.UpdateNotifier.dismiss(context)
                sheet = null
            },
            onDismiss = { sheet = null }
        )

        is SettingsSheet.WhatsNew -> WhatsNewSheet(entry = open.entry, onDismiss = { sheet = null })

        null -> Unit
    }
}

/** Choosing a provider chip selects that provider's first engine; the card then shows it. */
private fun chooseListeningProvider(prefs: PreferencesManager, provider: Provider) {
    if (prefs.getEngine().provider == provider) return
    TranscriptionEngine.values().firstOrNull { it.provider == provider }?.let { prefs.setEngine(it) }
}

private class AboutItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val supporting: String?,
    val onClick: () -> Unit
)

/** A section is a header and what it heads, 12dp apart. */
@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) { content() }
}

@Composable
private fun KeyNeededLine(provider: Provider, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            "${provider.label} needs a key before it can work.",
            color = Alert,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        AssistChip(label = "Add key", icon = Icons.Default.Add, onClick = onAdd)
    }
}

@Composable
private fun KeyRow(
    index: Int,
    count: Int,
    provider: Provider,
    key: String,
    status: KeyStatus,
    testing: Boolean,
    onOpen: () -> Unit,
    onTest: () -> Unit
) {
    GroupedRow(index, count, onClick = onOpen, modifier = Modifier.testTag("key_row_${provider.name}")) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
            ProviderMark(provider)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(provider.label, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                val line = when (status) {
                    KeyStatus.NotSet -> "Not set"
                    is KeyStatus.Failed -> status.message
                    else -> maskKey(key)
                }
                Text(
                    line,
                    color = if (status is KeyStatus.Failed) Alert else TextMid,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(Space.sm))
            when {
                testing -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                }
                status == KeyStatus.Connected -> InfoPill("Connected", container = GoodWash, content = OnGoodWash, icon = Icons.Default.Check)
                status == KeyStatus.NotSet -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .pressable(onClick = onOpen)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Accent)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = OnAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add", color = OnAccent, style = MaterialTheme.typography.titleSmall)
                }
                else -> AssistChip(label = if (status is KeyStatus.Failed) "Retry" else "Test", icon = Icons.Default.NetworkCheck, onClick = onTest)
            }
        }
    }
}

@Composable
private fun AppearanceCard() {
    val context = LocalContext.current
    val themePrefs = remember { ThemePrefs(context) }
    var choice by remember { mutableStateOf(themePrefs.choice) }
    var dynamic by remember { mutableStateOf(themePrefs.dynamic) }

    TonalCard(padding = PaddingValues(Space.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            ThemeChoice.values().forEach { option ->
                SegmentChip(
                    label = when (option) { ThemeChoice.AMOLED -> "Black"; else -> option.label },
                    selected = choice == option,
                    onClick = { choice = option; themePrefs.choice = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (DYNAMIC_SUPPORTED) {
            Spacer(Modifier.height(Space.lg))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.pressable(pressedScale = 0.99f) {
                dynamic = !dynamic; themePrefs.dynamic = dynamic
            }) {
                Column(Modifier.weight(1f)) {
                    Text("Wallpaper colours", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                    Text("Take the palette from your wallpaper. Black keeps its true black.", color = TextMid, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.width(Space.md))
                TickSwitch(dynamic, { dynamic = it; themePrefs.dynamic = it })
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(TealWash)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .size(150.dp)
                .background(Teal.copy(alpha = 0.22f), RadialShape(ExpressiveShapes.SoftBurst))
        )
        Column(Modifier.padding(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapedIcon(Icons.Default.Security, ExpressiveShapes.Gem, Teal, TealWash, size = 56.dp)
                Spacer(Modifier.width(Space.lg))
                Text("Private by design", color = Teal.copy(alpha = 1f), style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(Space.md))
            Text(
                "Your keys stay on this phone. Audio goes only to the provider you pick, straight from the app. No account, nothing sent to us.",
                color = TextHigh,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AboutCard(version: String, status: UpdateStatus, checking: Boolean) {
    TonalCard(padding = PaddingValues(Space.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp).background(Accent, RadialShape(ExpressiveShapes.Cookie9))
            ) {
                Icon(Icons.Default.Mic, null, tint = OnAccent, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.width(Space.lg))
            Column(Modifier.weight(1f)) {
                Text("RMBLR", color = TextHigh, style = MaterialTheme.typography.headlineSmall)
                Text("Version $version · MIT licence", color = TextMid, style = MaterialTheme.typography.bodyLarge)
            }
            when {
                checking -> CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                status is UpdateStatus.UpToDate -> InfoPill("Up to date", container = GoodWash, content = OnGoodWash, icon = Icons.Default.Check)
                status is UpdateStatus.Available -> InfoPill("Update", icon = Icons.Default.SystemUpdate)
                else -> Unit
            }
        }
    }
}

/** Pick the engine for the provider, and the model where the model is yours to name. */
@Composable
private fun EngineSheet(
    provider: Provider,
    prefs: PreferencesManager,
    onAddKey: () -> Unit,
    onDismiss: () -> Unit
) {
    val current by prefs.engineFlow.collectAsState()
    val engines = TranscriptionEngine.values().filter { it.provider == provider }
    RmblrSheet(title = "${provider.label} engine", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            engines.forEach { e ->
                SheetOption(
                    title = engineName(e),
                    supporting = e.blurb,
                    selected = e == current,
                    onClick = { prefs.setEngine(e) }
                )
            }
            val editable = engines.firstOrNull()?.takeIf { provider.editableModel }
            if (editable != null) {
                var model by remember(provider) { mutableStateOf(prefs.getEngineModel(editable)) }
                Spacer(Modifier.height(Space.sm))
                TonalTextField(
                    value = model,
                    onValueChange = { model = it; engines.forEach { e -> prefs.setEngineModel(e, it) } },
                    placeholder = "Model name",
                    label = "Model",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (prefs.getProviderKey(provider).isBlank()) {
                Spacer(Modifier.height(Space.sm))
                KeyNeededLine(provider, onAddKey)
            }
            Spacer(Modifier.height(Space.sm))
            Text(provider.note, color = TextMid, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(Space.lg))
            io.github.pastdaking.rmblr.ui.m3.SheetButton(
                "Done", onDismiss, Accent, OnAccent, Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TextModelSheet(prefs: PreferencesManager, onDismiss: () -> Unit) {
    var model by remember { mutableStateOf(prefs.getTextModel()) }
    RmblrSheet(title = "Rewriting model", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "The name of the model the provider should use for tones and translations.",
                color = TextMid,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(Space.lg))
            TonalTextField(
                value = model,
                onValueChange = { model = it },
                placeholder = "Model name",
                label = "Model",
                modifier = Modifier.fillMaxWidth().testTag("text_model_input")
            )
            Spacer(Modifier.height(Space.xl))
            io.github.pastdaking.rmblr.ui.m3.SheetButton(
                "Save",
                { prefs.setTextModel(model); onDismiss() },
                Accent, OnAccent, Modifier.fillMaxWidth()
            )
        }
    }
}
