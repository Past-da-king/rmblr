package io.github.pastdaking.rmblr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.pastdaking.rmblr.R

/**
 * Google Sans Flex, bundled as one variable font (SIL OFL, see docs/m3-build).
 *
 * One file serves every weight: each [Font] entry below pins the weight axis. Below
 * Android 8 variation settings are ignored and the file renders at its default weight,
 * which is a graceful fall back rather than a crash.
 */
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex: FontFamily = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w))
        )
    }
)

private fun style(
    weight: FontWeight,
    size: Int,
    line: Int,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = GoogleSansFlex,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

/**
 * The expressive scale from the design: display-s 36/42, headline 24 to 32, emphasised
 * titles. Weight does the work, so the paragraph sizes stay calm.
 */
val Typography = Typography(
    displaySmall = style(FontWeight.Bold, 36, 42, -0.8),
    headlineMedium = style(FontWeight.Bold, 32, 38, -0.6),
    headlineSmall = style(FontWeight.SemiBold, 24, 30, -0.3),
    titleLarge = style(FontWeight.SemiBold, 22, 28, -0.2),
    titleMedium = style(FontWeight.SemiBold, 16, 22),
    titleSmall = style(FontWeight.SemiBold, 14, 20),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.SemiBold, 14, 20),
    labelMedium = style(FontWeight.SemiBold, 12, 16, 0.5),
    labelSmall = style(FontWeight.SemiBold, 11, 16, 0.3),
)

/** Tracked-out caps used for eyebrows ("TONE", "LIVE TRANSCRIPT"). */
val EyebrowStyle: TextStyle = style(FontWeight.SemiBold, 12, 16, 1.2)

/** Big tabular timer digits on the recording screen. */
val TimerStyle: TextStyle = style(FontWeight.Bold, 56, 60, -1.0).copy(fontFeatureSettings = "tnum")
