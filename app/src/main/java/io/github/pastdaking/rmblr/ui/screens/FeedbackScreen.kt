package io.github.pastdaking.rmblr.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
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
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.IconChip
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.QuietAction
import io.github.pastdaking.rmblr.ui.components.Radius
import io.github.pastdaking.rmblr.ui.components.SectionLabel
import io.github.pastdaking.rmblr.ui.components.SettingRow
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.copyToClipboard
import io.github.pastdaking.rmblr.ui.components.openUrl
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.update.GitHubReleases
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import java.net.URLEncoder

/** What kind of thing is being sent. Decides the issue title and the label on it. */
private enum class FeedbackKind(
    val label: String,
    val blurb: String,
    val icon: ImageVector,
    val titlePrefix: String
) {
    BUG("Something's broken", "It did the wrong thing, or nothing at all", Icons.Default.BugReport, "Bug"),
    IDEA("An idea", "Something it should do and does not", Icons.Default.Lightbulb, "Idea"),
    QUESTION("A question", "You cannot work out how something works", Icons.Default.HelpOutline, "Question")
}

/**
 * The Feedback room in Settings.
 *
 * There is no server behind RMBLR and there is not going to be one, so there is nowhere
 * for a "Send" button to send anything to. Rather than pretend — a contact form that
 * quietly posts into a void is worse than no contact form — this writes the report and
 * hands it to GitHub, where the repository already is and where an answer can be public
 * and useful to the next person with the same problem.
 *
 * The version and phone details are attached because they are the first two things anyone
 * triaging a bug has to ask for, and asking costs a round trip through someone who has
 * already lost interest. The switch is there because they are still that person's details
 * to withhold.
 */
@Composable
fun FeedbackSettings(
    prefsManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine by prefsManager.engineFlow.collectAsState()

    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var body by remember { mutableStateOf("") }
    var attachDetails by remember { mutableStateOf(true) }

    val details = remember(attachDetails, engine) {
        if (!attachDetails) "" else buildString {
            append("\n\n---\n")
            append("RMBLR ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("${Build.MANUFACTURER} ${Build.MODEL}\n")
            // The engine, because half of everything reported as "dictation is broken" is
            // one provider having a bad day rather than the app. No key, obviously.
            append("Dictation: ${engine.label} via ${engine.provider.label}\n")
        }
    }

    val report = body.trim() + details
    val canSend = body.isNotBlank()

    Column(
        verticalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = modifier.fillMaxWidth().testTag("feedback_settings")
    ) {
        SectionLabel("What is it")

        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            FeedbackKind.values().forEach { entry ->
                KindRow(entry, selected = entry == kind) { kind = entry }
            }
        }

        Spacer(Modifier.height(Space.lg))
        SectionLabel("In your own words")

        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .testTag("feedback_body"),
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
                    text = when (kind) {
                        FeedbackKind.BUG -> "What were you doing, and what happened instead?"
                        FeedbackKind.IDEA -> "What should it do?"
                        FeedbackKind.QUESTION -> "What is not making sense?"
                    },
                    color = TextLow,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )

        Spacer(Modifier.height(Space.sm))

        Panel {
            SettingRow(
                label = "Attach version and phone",
                supporting = if (attachDetails)
                    "RMBLR ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} · ${Build.MODEL}"
                else
                    "Nothing about your device will be included",
                trailing = {
                    Switch(
                        checked = attachDetails,
                        onCheckedChange = { attachDetails = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnAccent,
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = TextLow,
                            uncheckedTrackColor = Raised
                        )
                    )
                }
            )
        }

        Spacer(Modifier.height(Space.sm))

        PrimaryAction(
            text = "Open a GitHub issue",
            icon = Icons.Default.OpenInNew,
            enabled = canSend,
            onClick = {
                val url = newIssueUrl(kind, report)
                if (!openUrl(context, url)) {
                    copyToClipboard(context, "RMBLR feedback", report)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("feedback_send")
        )
        Spacer(Modifier.height(Space.sm))
        QuietAction(
            text = "Copy it instead",
            icon = Icons.Default.ContentCopy,
            onClick = { copyToClipboard(context, "RMBLR feedback", report) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Space.lg))

        Panel {
            SettingRow(
                label = "Read what others have said",
                supporting = "Open issues on GitHub — yours may already be there",
                icon = Icons.Default.OpenInNew,
                onClick = { openUrl(context, GitHubReleases.ISSUES_URL) }
            )
            Hairline()
            SettingRow(
                label = "The code itself",
                supporting = "github.com/${GitHubReleases.OWNER}/${GitHubReleases.REPO}",
                icon = Icons.Default.OpenInNew,
                onClick = {
                    openUrl(context, "https://github.com/${GitHubReleases.OWNER}/${GitHubReleases.REPO}")
                }
            )
        }

        Spacer(Modifier.height(Space.sm))
        Text(
            text = "Opening an issue needs a free GitHub account. If you would rather not " +
                "have one, copy the report and send it however you like — it is the same " +
                "text either way, and nothing is sent anywhere until you press one of " +
                "these two buttons.",
            color = TextLow,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun KindRow(kind: FeedbackKind, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.panel))
            .background(if (selected) AccentWash else Surface)
            .padding(Space.lg)
    ) {
        IconChip(
            icon = kind.icon,
            tint = if (selected) Accent else TextMid,
            container = if (selected) Surface else Raised,
            size = 36.dp
        )
        Spacer(Modifier.width(Space.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                kind.label,
                color = if (selected) Accent else TextHigh,
                style = MaterialTheme.typography.titleMedium
            )
            Text(kind.blurb, color = TextMid, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * GitHub's prefilled-issue URL.
 *
 * Both parameters are encoded, because a report is free text and a stray ampersand in it
 * would otherwise truncate everything after it — which would look, to the person sending
 * it, exactly like the app silently losing half of what they wrote.
 */
private fun newIssueUrl(kind: FeedbackKind, report: String): String {
    val title = enc("${kind.titlePrefix}: ${report.lineSequence().firstOrNull().orEmpty().trim().take(70)}")
    return "${GitHubReleases.NEW_ISSUE_URL}?title=$title&body=${enc(report)}"
}

private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
