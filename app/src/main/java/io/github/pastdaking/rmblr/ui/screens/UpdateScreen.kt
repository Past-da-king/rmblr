package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.PanelTone
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.QuietAction
import io.github.pastdaking.rmblr.ui.components.ReleaseNotes
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.SectionLabel
import io.github.pastdaking.rmblr.ui.components.SettingRow
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.ValueRow
import io.github.pastdaking.rmblr.ui.components.copyToClipboard
import io.github.pastdaking.rmblr.ui.components.openUrl
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.Good
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import io.github.pastdaking.rmblr.update.Changelog
import io.github.pastdaking.rmblr.update.ChangelogEntry
import io.github.pastdaking.rmblr.update.GitHubReleases
import io.github.pastdaking.rmblr.update.ReleaseInfo
import io.github.pastdaking.rmblr.update.UpdateNotifier
import io.github.pastdaking.rmblr.update.UpdateRepository
import io.github.pastdaking.rmblr.update.UpdateStatus
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * The Updates room in Settings.
 *
 * RMBLR is not on an app store, so nothing tells anybody a new one exists — until this,
 * the only route to a newer version was remembering to go and look at a GitHub page. This
 * asks on your behalf, shows you what is in it before you commit to anything, and hands
 * the APK to your browser. It cannot install the thing; an app that could silently
 * replace itself is an app you should not trust with a microphone.
 */
@Composable
fun UpdatesSettings(
    repo: UpdateRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val status by repo.status.collectAsState()
    val checking by repo.checking.collectAsState()
    val auto by repo.autoCheck.collectAsState()

    var sheet by remember { mutableStateOf<UpdateSheet?>(null) }

    // Ask on arrival, but only if the answer we are holding is stale. Someone who opened
    // this page came here to find out, and making them press a button to be told
    // something we could have fetched while they were reading the title is theatre.
    LaunchedEffect(Unit) { repo.check(force = false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = modifier.fillMaxWidth().testTag("updates_settings")
    ) {
        val available = (status as? UpdateStatus.Available)?.release

        Panel(tone = if (available != null) PanelTone.HERO else PanelTone.PLAIN) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (available != null) "RMBLR ${available.version} is out"
                        else "RMBLR ${repo.installedVersion}",
                        color = TextHigh,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = statusLine(status, checking, repo),
                        color = when {
                            status is UpdateStatus.Failed -> Alert
                            available != null -> Accent
                            status is UpdateStatus.UpToDate -> Good
                            else -> TextMid
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (checking) {
                    Spacer(Modifier.width(Space.md))
                    CircularProgressIndicator(
                        color = Accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (available != null) {
                    PrimaryAction(
                        text = "What's in it",
                        icon = Icons.Default.Download,
                        onClick = { sheet = UpdateSheet.Available(available) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    PrimaryAction(
                        text = if (checking) "Checking…" else "Check now",
                        onClick = { if (!checking) scope.launch { repo.check(force = true) } },
                        enabled = !checking,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // The notes for the build you are running, always reachable — not only in the
        // one-shot sheet that appears after an update and is gone if you dismiss it.
        val installedNotes = repo.currentNotes()
        if (installedNotes != null) {
            ValueRow(
                label = "What's new in ${repo.installedVersion}",
                value = "Read",
                supporting = installedNotes.headline,
                onClick = { sheet = UpdateSheet.WhatsNew(installedNotes) }
            )
        }

        Panel {
            SettingRow(
                label = "Check automatically",
                supporting = "Once a day, and tells you when there is something new",
                trailing = {
                    Switch(
                        checked = auto,
                        onCheckedChange = { repo.setAutoCheck(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnAccent,
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = TextLow,
                            uncheckedTrackColor = Raised
                        )
                    )
                }
            )
            Hairline()
            SettingRow(
                label = "All releases",
                supporting = "Every version, on GitHub",
                icon = Icons.Default.OpenInNew,
                onClick = { openUrl(context, GitHubReleases.RELEASES_URL) }
            )
        }

        Spacer(Modifier.height(Space.sm))
        Text(
            text = "RMBLR is not on an app store, so updates arrive as an APK you install " +
                "yourself. Android will warn you it came from an unknown source — that " +
                "warning is about where the file came from, not what is in it. Every " +
                "release is signed with the same key, which is what makes it install over " +
                "the one you have instead of asking you to uninstall first.",
            color = TextLow,
            style = MaterialTheme.typography.bodySmall
        )
    }

    when (val open = sheet) {
        is UpdateSheet.Available -> UpdateAvailableSheet(
            release = open.release,
            onDownload = {
                UpdateNotifier.dismiss(context)
                if (!openUrl(context, open.release.downloadUrl)) {
                    copyToClipboard(context, "RMBLR download", open.release.downloadUrl)
                }
                sheet = null
            },
            onSkip = {
                repo.skip(open.release.version)
                UpdateNotifier.dismiss(context)
                sheet = null
            },
            onDismiss = { sheet = null }
        )

        is UpdateSheet.WhatsNew -> WhatsNewSheet(
            entry = open.entry,
            onDismiss = { sheet = null }
        )

        null -> Unit
    }
}

private sealed interface UpdateSheet {
    data class Available(val release: ReleaseInfo) : UpdateSheet
    data class WhatsNew(val entry: ChangelogEntry) : UpdateSheet
}

private fun statusLine(
    status: UpdateStatus,
    checking: Boolean,
    repo: UpdateRepository
): String = when {
    checking -> "Asking GitHub…"
    status is UpdateStatus.Available ->
        "You are on ${repo.installedVersion}" +
            (status.release.apkSize?.let { " · $it download" } ?: "")
    status is UpdateStatus.UpToDate -> "This is the newest release · checked ${when_(status.checkedAt)}"
    status is UpdateStatus.Failed -> status.message
    else -> "Not checked yet"
}

private fun when_(millis: Long): String {
    if (millis <= 0) return "never"
    val ago = System.currentTimeMillis() - millis
    return when {
        ago < 60_000 -> "just now"
        ago < 3_600_000 -> "${ago / 60_000}m ago"
        ago < 86_400_000 -> "${ago / 3_600_000}h ago"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
    }
}

/**
 * What is in the version you have not installed yet.
 *
 * This is the sheet the whole feature exists for. Sideloading is an act of trust — you
 * are being asked to take a file off the internet and let it replace an app that reads
 * your text fields. Being told exactly what changed first, before the browser opens, is
 * the least the app can do in exchange.
 */
@Composable
fun UpdateAvailableSheet(
    release: ReleaseInfo,
    onDownload: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    RmblrSheet(title = "RMBLR ${release.version}", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = release.title,
                color = Accent,
                style = MaterialTheme.typography.titleMedium
            )
            release.apkSize?.let {
                Spacer(Modifier.height(Space.xs))
                Text("$it · APK", color = TextLow, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(Space.lg))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (release.notes.isBlank()) {
                    Text(
                        text = "No release notes were published for this one.",
                        color = TextMid,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    ReleaseNotes(release.notes)
                }
            }

            Spacer(Modifier.height(Space.xl))

            PrimaryAction(
                text = "Download",
                icon = Icons.Default.Download,
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.sm))
            QuietAction(
                text = "Skip this version",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * What changed in the version you are now running, plus where it is going.
 *
 * Shown once, automatically, the first time the app opens after an update. A changelog on
 * its own only ever answers "what was fixed"; the vision underneath it answers "why is
 * this still on my phone", and that is the question a person actually has three updates
 * into an app they installed once out of curiosity.
 */
@Composable
fun WhatsNewSheet(
    entry: ChangelogEntry,
    onDismiss: () -> Unit
) {
    RmblrSheet(title = "What's new in ${entry.version}", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Good,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Space.sm))
                Text(
                    text = entry.headline,
                    color = Accent,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(Space.lg))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ReleaseNotes(entry.notes)

                Spacer(Modifier.height(Space.xxl))
                SectionLabel("Where this is going")
                Panel {
                    ReleaseNotes(Changelog.VISION)
                }
            }

            Spacer(Modifier.height(Space.xl))
            PrimaryAction(
                text = "Got it",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
