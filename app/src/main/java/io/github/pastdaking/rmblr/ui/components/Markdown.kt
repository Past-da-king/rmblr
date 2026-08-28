package io.github.pastdaking.rmblr.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid

/**
 * Release notes, rendered rather than dumped.
 *
 * Not a markdown library — deliberately. The only markdown this app ever displays is its
 * own release notes, written by the person who writes the app, and they use exactly three
 * things: paragraphs, `- ` bullets, and `**bold**` for the lead of a point. Pulling in a
 * parser to handle tables and footnotes nobody will ever write would cost more than the
 * feature. What it will not do, it ignores: an unsupported construct comes out as the
 * plain text it was, which is a far better failure than a crash or a stack of asterisks.
 */
@Composable
fun ReleaseNotes(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val blocks = markdown.trim().lines()
        var previousWasBullet = false

        blocks.forEach { raw ->
            val line = raw.trim()

            if (line.isEmpty()) {
                // Blank lines are the only paragraph break the format has. One gap, not
                // one per consecutive blank, so sloppy spacing in a release body cannot
                // open a hole in the middle of the sheet.
                if (!previousWasBullet) Spacer(Modifier.height(Space.md))
                previousWasBullet = false
                return@forEach
            }

            when {
                line.startsWith("- ") || line.startsWith("* ") -> {
                    if (!previousWasBullet) Spacer(Modifier.height(Space.sm))
                    Bullet(inlineMarkdown(line.drop(2)))
                    previousWasBullet = true
                }

                line.startsWith("#") -> {
                    Spacer(Modifier.height(Space.md))
                    Text(
                        text = line.dropWhile { it == '#' }.trim(),
                        color = TextHigh,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Space.xs))
                    previousWasBullet = false
                }

                else -> {
                    if (previousWasBullet) Spacer(Modifier.height(Space.md))
                    Text(
                        text = inlineMarkdown(line),
                        color = TextMid,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    previousWasBullet = false
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: AnnotatedString) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = "•",
            color = Accent,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(Space.md))
        Text(
            text = text,
            color = TextMid,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * `**bold**` spans, and nothing else.
 *
 * Splits on the delimiter and alternates, which is correct for well-formed input and
 * degrades to plain text for input that is not — an unclosed `**` leaves the rest of the
 * line bold rather than throwing, and that is a typo somebody can see and fix.
 */
internal fun inlineMarkdown(source: String): AnnotatedString = buildAnnotatedString {
    // Bold also lifts to the high-contrast ink. In notes written as "**the point** and
    // then the detail", weight alone is not enough separation at body size.
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = TextHigh)
    source.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) {
            val handle = pushStyle(bold)
            append(part)
            pop(handle)
        } else {
            append(part)
        }
    }
}
