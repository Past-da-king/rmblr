package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.Provider
import io.github.pastdaking.rmblr.data.TextProvider
import io.github.pastdaking.rmblr.data.TranscriptionEngine
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.copyToClipboard
import io.github.pastdaking.rmblr.ui.components.openUrl
import io.github.pastdaking.rmblr.ui.m3.AssistChip
import io.github.pastdaking.rmblr.ui.m3.ExpressiveShapes
import io.github.pastdaking.rmblr.ui.m3.Radial
import io.github.pastdaking.rmblr.ui.m3.SheetButton
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalIconButton
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.AlertWash
import io.github.pastdaking.rmblr.ui.theme.GoodWash
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.OnAlertWash
import io.github.pastdaking.rmblr.ui.theme.OnGoodWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.Teal
import io.github.pastdaking.rmblr.ui.theme.TealWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.launch

/** What a provider's key is used for, in the words the sheet says under its title. */
fun usedFor(provider: Provider): String {
    val dictation = TranscriptionEngine.values().any { it.provider == provider }
    val rewriting = TextProvider.values().any { it.provider == provider }
    return when {
        dictation && rewriting -> "Used for dictation and rewriting"
        dictation -> "Used for dictation"
        rewriting -> "Used for rewriting"
        else -> "Used for anything you point it at"
    }
}

/** The look of one provider's badge: a letter in an expressive shape. */
data class ProviderBadge(val letter: String, val shape: Radial, val container: androidx.compose.ui.graphics.Color, val tint: androidx.compose.ui.graphics.Color)

@Composable
fun badgeOf(provider: Provider): ProviderBadge = when (provider) {
    Provider.GEMINI -> ProviderBadge("G", ExpressiveShapes.Flower, AccentWash, OnAccentWash)
    Provider.GROQ -> ProviderBadge("Q", ExpressiveShapes.Clover4, TealWash, Teal)
    Provider.MISTRAL -> ProviderBadge("M", ExpressiveShapes.Gem, AccentWash, OnAccentWash)
    Provider.OPENROUTER -> ProviderBadge("R", ExpressiveShapes.Puffy, GoodWash, OnGoodWash)
    Provider.OPENAI -> ProviderBadge("O", ExpressiveShapes.Cookie9, TealWash, Teal)
    Provider.CUSTOM -> ProviderBadge("+", ExpressiveShapes.Sunny, androidx.compose.ui.graphics.Color.Transparent, TextMid)
}

/** The letter badge itself. */
@Composable
fun ProviderMark(provider: Provider, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val badge = badgeOf(provider)
    val container = if (provider == Provider.CUSTOM) RaisedHigh else badge.container
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(container, io.github.pastdaking.rmblr.ui.m3.RadialShape(badge.shape))
    ) {
        Text(badge.letter, color = badge.tint, style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * Enter, test and save one provider's key, in a sheet that slides up.
 *
 * The test is a real request with the key as typed, before anything is saved, so "Save"
 * is never a leap of faith. Saving does not need a passing test, because a key can be
 * right and the provider briefly unreachable; the result is simply remembered when it
 * passes, and the list behind the sheet shows "Connected" only for a key that did.
 */
@Composable
fun ApiKeySheet(
    provider: Provider,
    prefs: PreferencesManager,
    checks: KeyChecks,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    initialResult: KeyTestResult? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var input by remember(provider) { mutableStateOf(prefs.getProviderKey(provider)) }
    var baseUrl by remember(provider) {
        mutableStateOf(if (provider == Provider.CUSTOM) prefs.getProviderBaseUrl(provider) else provider.baseUrl)
    }
    var reveal by remember(provider) { mutableStateOf(false) }
    var testing by remember(provider) { mutableStateOf(false) }
    var result by remember(provider) { mutableStateOf(initialResult) }

    fun runTest() {
        if (testing) return
        testing = true
        result = null
        val keyAtStart = input
        scope.launch {
            val outcome = KeyTester.test(provider, keyAtStart, baseUrl.ifBlank { provider.baseUrl })
            // The key may have been edited while the request was out; its answer then says
            // nothing about what is in the box now.
            if (keyAtStart == input) result = outcome
            testing = false
        }
    }

    RmblrSheet(title = "", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderMark(provider, size = 64.dp)
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text(
                        if (provider == Provider.CUSTOM) "OpenAI-compatible" else "${provider.label} API key",
                        color = TextHigh,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(usedFor(provider), color = TextMid, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(Space.xl))

            if (provider == Provider.CUSTOM) {
                TonalTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; result = null },
                    placeholder = "https://api.example.com/v1",
                    label = "Base URL",
                    modifier = Modifier.fillMaxWidth().testTag("api_base_input")
                )
                Spacer(Modifier.height(Space.md))
            }

            TonalTextField(
                value = input,
                onValueChange = { input = it; result = null },
                placeholder = provider.keyHint,
                label = "API key",
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation('•'),
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(androidx.compose.ui.graphics.Color.Transparent)
                    ) {
                        TonalIconButton(
                            icon = if (reveal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (reveal) "Hide key" else "Show key",
                            onClick = { reveal = !reveal }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("api_key_input")
            )

            Spacer(Modifier.height(Space.md))

            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                AssistChip(
                    label = "Paste",
                    icon = Icons.Default.ContentPaste,
                    onClick = {
                        clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let {
                            input = it
                            result = null
                        }
                    }
                )
                val console = keyConsoleUrl(provider)
                if (console.isNotEmpty()) {
                    AssistChip(
                        label = "Get a key",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            if (!openUrl(context, console)) copyToClipboard(context, "Key page", console)
                        }
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))

            KeyResultCard(testing = testing, result = result)

            Spacer(Modifier.height(Space.lg))

            Text(
                text = provider.note.substringBefore(". ").trimEnd('.') + ". The key is saved on this phone and sent only to ${provider.label}.",
                color = TextMid,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(Space.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), modifier = Modifier.fillMaxWidth()) {
                SheetButton(
                    label = if (result == null) "Test" else "Test again",
                    onClick = { runTest() },
                    container = AccentWash,
                    content = OnAccentWash,
                    modifier = Modifier.weight(1f).testTag("api_key_test")
                )
                SheetButton(
                    label = "Save",
                    onClick = {
                        val cleaned = input.trim()
                        prefs.setProviderKey(provider, cleaned)
                        if (provider == Provider.CUSTOM) prefs.setProviderBaseUrl(provider, baseUrl)
                        if (result?.ok == true && cleaned.isNotEmpty()) checks.passed(provider, cleaned) else checks.clear(provider)
                        onSaved()
                        onDismiss()
                    },
                    container = Accent,
                    content = OnAccent,
                    modifier = Modifier.weight(1f).testTag("api_key_save")
                )
            }
        }
    }
}

/** The card between the field and the buttons: checking, works, or what went wrong. */
@Composable
private fun KeyResultCard(testing: Boolean, result: KeyTestResult?) {
    when {
        testing -> TonalCard(
            container = RaisedHigh,
            padding = PaddingValues(Space.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text("Checking the key", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                    Text("Asking the provider, one small request.", color = TextMid, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        result != null && result.ok -> TonalCard(container = GoodWash, padding = PaddingValues(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapedIcon(Icons.Default.Check, ExpressiveShapes.Flower, androidx.compose.ui.graphics.Color(0xFF1D7A4B), androidx.compose.ui.graphics.Color.White, size = 48.dp)
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text("Key works", color = OnGoodWash, style = MaterialTheme.typography.titleMedium)
                    Text("Checked just now. ${result.message}", color = OnGoodWash, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        result != null -> TonalCard(container = AlertWash, padding = PaddingValues(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, tint = OnAlertWash, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text("That did not work", color = OnAlertWash, style = MaterialTheme.typography.titleMedium)
                    Text(result.message, color = OnAlertWash, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        else -> TonalCard(container = RaisedHigh, padding = PaddingValues(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NetworkCheck, null, tint = TextMid, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text("Not checked yet", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                    Text("Test sends one small request with this key.", color = TextMid, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
