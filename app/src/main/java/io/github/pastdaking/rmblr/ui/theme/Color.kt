package io.github.pastdaking.rmblr.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * RMBLR's palette, in two skins.
 *
 * Every token below is a `get()` rather than a constant, reading one piece of state. That
 * is the whole light-mode mechanism: nothing else in the app changed. Screens still write
 * `Ink` and `TextHigh` exactly as before, and because a composable that reads these is
 * reading snapshot state, flipping [Skin.dark] recomposes the lot.
 *
 * Colour still carries meaning and only meaning: indigo is "ours / selected", red is
 * "recording or failed", green is "done". Nothing is tinted for decoration.
 */
object Skin {
    var dark by mutableStateOf(true)
}

// Surfaces, furthest back to closest
val Ink: Color get() = if (Skin.dark) Color(0xFF0A0B0D) else Color(0xFFF7F8FA)
val Surface: Color get() = if (Skin.dark) Color(0xFF15171B) else Color(0xFFFFFFFF)
val Raised: Color get() = if (Skin.dark) Color(0xFF1E2127) else Color(0xFFECEEF2)
val Line: Color get() = if (Skin.dark) Color(0xFF2B2F37) else Color(0xFFDCE0E6)

// Text
val TextHigh: Color get() = if (Skin.dark) Color(0xFFEDEFF3) else Color(0xFF14161A)
val TextMid: Color get() = if (Skin.dark) Color(0xFF9AA1AD) else Color(0xFF5C6472)
val TextLow: Color get() = if (Skin.dark) Color(0xFF666D7A) else Color(0xFF8B93A1)

// Signals. The accent darkens slightly on light so it still passes against white.
val Accent: Color get() = if (Skin.dark) Color(0xFF7C6CFF) else Color(0xFF5A46F0)
val AccentWash: Color get() = if (Skin.dark) Color(0xFF23204A) else Color(0xFFE7E4FF)
val Alert: Color get() = if (Skin.dark) Color(0xFFE05561) else Color(0xFFD03A48)
val Good: Color get() = if (Skin.dark) Color(0xFF4FBF87) else Color(0xFF2E9B67)

/** Text and glyphs drawn ON the accent. Always light: the accent stays dark enough. */
val OnAccent: Color get() = Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// Older names, kept so nothing has to be renamed to keep compiling.
// ---------------------------------------------------------------------------
val KeyFace: Color get() = Raised
val KeyMod: Color get() = Surface
val KeyPressed: Color get() = Line

val SlateDarkBg: Color get() = Ink
val SlateSurface: Color get() = Surface
val SlateSurfaceVariant: Color get() = Raised
val SlateCard: Color get() = Surface

val StarCyanPrimary: Color get() = Accent
val StarCyanVariant: Color get() = Accent
val StarVioletSecondary: Color get() = Accent
val StarVioletDark: Color get() = Raised
val StarAccentGold: Color get() = Accent

val TextPrimaryDark: Color get() = TextHigh
val TextSecondaryDark: Color get() = TextMid
val TextMutedDark: Color get() = TextLow

val WaveformCyan: Color get() = Accent
val WaveformViolet: Color get() = Accent
val WaveformPink: Color get() = Alert

val SuccessGreen: Color get() = Good
val WarningAmber: Color get() = Accent
val ErrorRed: Color get() = Alert
