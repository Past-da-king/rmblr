package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.components.ReleaseNotes
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.ExpressiveShapes
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.SheetButton
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentText
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import io.github.pastdaking.rmblr.update.Changelog
import io.github.pastdaking.rmblr.update.ChangelogEntry
import io.github.pastdaking.rmblr.update.ReleaseInfo

/**
 * What is in the version you have not installed yet.
 *
 * This is the sheet the whole update feature exists for. Sideloading is an act of trust:
 * you are being asked to take a file off the internet and let it replace an app that reads
 * your text fields. Being told exactly what changed first, before the browser opens, is
 * the least the app can do in exchange. It cannot install the thing itself, and an app
 * that could silently replace itself is one you should not trust with a microphone.
 */
@Composable
fun UpdateAvailableSheet(
    release: ReleaseInfo,
    onDownload: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    RmblrSheet(title = "", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().testTag("update_sheet")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapedIcon(Icons.Default.AutoAwesome, ExpressiveShapes.Cookie9, Accent, OnAccent, size = 64.dp)
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text("RMBLR ${release.version} is out", color = TextHigh, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        InfoPill("Update")
                        release.apkSize?.let { size ->
                            InfoPill(
                                "${size.replace(".0 MB", " MB")} · APK",
                                container = RaisedHigh,
                                content = TextMid
                            )
                        }
                    }
                }
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    ReleaseNotes(release.notes)
                }
            }

            Spacer(Modifier.height(Space.md))
            Text(
                text = "RMBLR is not on an app store, so updates arrive as an APK that you install yourself. " +
                    "Android will warn that it came from an unknown source; that warning is about where the file came from, not what is in it.",
                color = TextMid,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(Space.xl))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                SheetButton(
                    label = "Skip this version",
                    onClick = onSkip,
                    container = Color.Transparent,
                    content = AccentText,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .pressable(onClick = onDownload)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Accent)
                        .testTag("update_download")
                ) {
                    Icon(Icons.Default.Download, null, tint = OnAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Space.sm))
                    Text("Download", color = OnAccent, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * What changed in the version you are now running, plus where it is going.
 *
 * Shown once, automatically, the first time the app opens after an update, and always
 * reachable from About in Settings. A changelog on its own only ever answers what was
 * fixed; the vision underneath it answers why this is still on the phone.
 */
@Composable
fun WhatsNewSheet(
    entry: ChangelogEntry,
    onDismiss: () -> Unit
) {
    RmblrSheet(title = "", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().testTag("whats_new_sheet")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapedIcon(Icons.Default.AutoAwesome, ExpressiveShapes.Cookie9, Accent, OnAccent, size = 64.dp)
                Spacer(Modifier.width(Space.lg))
                Column {
                    Text("What is new in ${entry.version}", color = TextHigh, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(Space.xs))
                    Text(entry.headline, color = TextMid, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(Space.lg))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ReleaseNotes(entry.notes)

                Spacer(Modifier.height(Space.xl))
                Eyebrow("Where this is going")
                Spacer(Modifier.height(Space.sm))
                TonalCard(padding = PaddingValues(Space.lg)) {
                    ReleaseNotes(Changelog.VISION)
                }
            }

            Spacer(Modifier.height(Space.xl))
            SheetButton("Got it", onDismiss, Accent, OnAccent, Modifier.fillMaxWidth())
        }
    }
}
