package io.github.pastdaking.rmblr.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.BuildConfig
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.copyToClipboard
import io.github.pastdaking.rmblr.ui.components.openUrl
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.GroupedRow
import io.github.pastdaking.rmblr.ui.m3.SheetButton
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.update.GitHubReleases
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import java.net.URLEncoder

/** What kind of thing is being sent. Decides the issue title and the label on it. */
enum class FeedbackKind(
    val label: String,
    val icon: ImageVector,
    val titlePrefix: String,
    val prompt: String
) {
    BUG("Something is broken", Icons.Default.BugReport, "Bug", "Steps, what you saw, what you expected"),
    IDEA("An idea", Icons.Default.Lightbulb, "Idea", "What should it do?"),
    QUESTION("A question", Icons.Default.HelpOutline, "Question", "What is not making sense?")
}

/**
 * Feedback: a report written here and handed to GitHub.
 *
 * There is no server behind RMBLR and there is not going to be one, so there is nowhere
 * for a Send button to send anything to. The report is composed here and opened as a
 * prefilled GitHub issue, where an answer can be public and useful to the next person
 * with the same problem. The version and phone details are attached because they are the
 * first two things anyone triaging a bug has to ask for; the switch is there because they
 * are still that person's details to withhold.
 */
@Composable
fun FeedbackScreen(
    prefsManager: PreferencesManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine by prefsManager.engineFlow.collectAsState()

    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var body by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(true) }

    val details = remember(attach, engine) {
        if (!attach) "" else deviceDetails(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            androidRelease = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            engineLine = "${engine.label} via ${engine.provider.label}"
        )
    }
    val report = composeReport(body, details)
    val canSend = body.isNotBlank()

    Column(modifier.fillMaxSize().background(Ink).testTag("feedback_screen")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = Space.lg, vertical = Space.md)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to settings",
                tint = TextHigh,
                modifier = Modifier.size(24.dp).pressable(onClick = onBack)
            )
            Spacer(Modifier.width(Space.lg))
            Text("Feedback", color = TextHigh, style = MaterialTheme.typography.titleLarge)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg)
                .padding(top = Space.lg, bottom = Space.navClear)
        ) {
            Eyebrow("What is it about", modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                FeedbackKind.values().forEach { entry ->
                    KindCard(entry, selected = entry == kind, modifier = Modifier.weight(1f)) { kind = entry }
                }
            }

            Spacer(Modifier.height(Space.xs))
            TonalTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = kind.prompt,
                minLines = 5,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth().testTag("feedback_body")
            )

            TonalCard(padding = PaddingValues(horizontal = Space.lg, vertical = Space.md), onClick = { attach = !attach }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Attach version and phone", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (attach) "RMBLR ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE}" else "Nothing about your device is included",
                            color = TextMid,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.width(Space.md))
                    TickSwitch(attach, { attach = it })
                }
            }

            Spacer(Modifier.height(Space.xs))
            SheetButton(
                label = "Open a GitHub issue",
                onClick = {
                    if (canSend) {
                        val url = newIssueUrl(kind, report)
                        if (!openUrl(context, url)) copyToClipboard(context, "RMBLR feedback", report)
                    }
                },
                container = if (canSend) Accent else RaisedHigh,
                content = if (canSend) OnAccent else TextMid,
                modifier = Modifier.fillMaxWidth().testTag("feedback_send")
            )
            SheetButton(
                label = "Copy it instead",
                onClick = { if (canSend) copyToClipboard(context, "RMBLR feedback", report) },
                container = AccentWash,
                content = OnAccentWash,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Space.md))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinkRow(0, 2, Icons.Default.Forum, "Read what others have said", "Open issues on GitHub") {
                    if (!openUrl(context, GitHubReleases.ISSUES_URL)) copyToClipboard(context, "Issues", GitHubReleases.ISSUES_URL)
                }
                LinkRow(1, 2, Icons.Default.Code, "The code itself", "github.com/${GitHubReleases.OWNER}/${GitHubReleases.REPO}") {
                    val url = "https://github.com/${GitHubReleases.OWNER}/${GitHubReleases.REPO}"
                    if (!openUrl(context, url)) copyToClipboard(context, "Repository", url)
                }
            }

            Text(
                text = "Nothing is sent anywhere until you press one of the two buttons. A GitHub issue needs a free GitHub account.",
                color = TextMid,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.md)
            )
        }
    }
}

@Composable
private fun KindCard(kind: FeedbackKind, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (selected) 24.dp else 28.dp)
    Column(
        modifier = modifier
            .heightIn128()
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .clip(shape)
            .background(if (selected) AccentWash else RaisedHigh)
            .then(if (selected) Modifier.border(2.dp, Accent, shape) else Modifier)
            .padding(Space.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(kind.icon, null, tint = if (selected) OnAccentWash else TextHigh, modifier = Modifier.size(32.dp))
        Text(
            kind.label,
            color = if (selected) OnAccentWash else TextHigh,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private fun Modifier.heightIn128(): Modifier = this.then(Modifier.height(112.dp))

@Composable
private fun LinkRow(index: Int, count: Int, icon: ImageVector, title: String, supporting: String, onClick: () -> Unit) {
    GroupedRow(index, count, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(icon, null, tint = TextHigh, modifier = Modifier.padding(horizontal = 6.dp))
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Text(supporting, color = TextMid, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextHigh)
        }
    }
}

// ------------------------------------------------------------------ report text

/** The device block appended to a report. No key, ever. */
fun deviceDetails(
    versionName: String,
    versionCode: Int,
    androidRelease: String,
    sdk: Int,
    manufacturer: String,
    model: String,
    engineLine: String
): String = buildString {
    append("\n\n---\n")
    append("RMBLR $versionName ($versionCode)\n")
    append("Android $androidRelease (API $sdk)\n")
    append("$manufacturer $model\n")
    append("Dictation: $engineLine\n")
}

fun composeReport(body: String, details: String): String = body.trim() + details

/**
 * GitHub's prefilled-issue URL.
 *
 * Both parameters are encoded, because a report is free text and a stray ampersand in it
 * would otherwise truncate everything after it, which would look, to the person sending
 * it, exactly like the app silently losing half of what they wrote.
 */
fun newIssueUrl(kind: FeedbackKind, report: String): String {
    val title = enc("${kind.titlePrefix}: ${report.lineSequence().firstOrNull().orEmpty().trim().take(70)}")
    return "${GitHubReleases.NEW_ISSUE_URL}?title=$title&body=${enc(report)}"
}

private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
