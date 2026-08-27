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

    /**
     * True black rather than nearly black.
     *
     * On an OLED panel a pixel showing #000000 is simply switched off, so the app stops
     * being a dark grey rectangle in a dark room and the edges of it disappear into the
     * screen. It also draws less power. [dark] stays true when this is on: AMOLED is a
     * darker dark, not a third unrelated skin, so every other token keeps its meaning.
     */
    var amoled by mutableStateOf(false)

    /**
     * Colours pulled off the wallpaper, or null when that is switched off.
     *
     * Material You (Android calls it dynamic colour, the engine behind it is Monet) hands
     * every app a palette derived from whatever the user has set as their wallpaper. This
     * app cannot simply pass that to MaterialTheme and be done, because almost nothing
     * here reads `MaterialTheme.colorScheme` — the screens read the tokens below. So the
     * palette is parked here and every token prefers it when it exists.
     */
    var dynamic by mutableStateOf<DynamicPalette?>(null)
}

/** The wallpaper's answer to each token. */
data class DynamicPalette(
    val ink: Color,
    val surface: Color,
    val raised: Color,
    val line: Color,
    val textHigh: Color,
    val textMid: Color,
    val textLow: Color,
    val accent: Color,
    val accentWash: Color,
    val onAccent: Color
)

// Surfaces, furthest back to closest
val Ink: Color get() = when {
    Skin.amoled -> Color(0xFF000000)
    Skin.dynamic != null -> Skin.dynamic!!.ink
    Skin.dark -> Color(0xFF0A0B0D)
    else -> Color(0xFFF7F8FA)
}
val Surface: Color get() = when {
    Skin.amoled -> Color(0xFF0B0B0D)
    Skin.dynamic != null -> Skin.dynamic!!.surface
    Skin.dark -> Color(0xFF15171B)
    else -> Color(0xFFFFFFFF)
}
val Raised: Color get() = when {
    Skin.amoled -> Color(0xFF16171A)
    Skin.dynamic != null -> Skin.dynamic!!.raised
    Skin.dark -> Color(0xFF1E2127)
    else -> Color(0xFFECEEF2)
}
val Line: Color get() = when {
    Skin.amoled -> Color(0xFF262A31)
    Skin.dynamic != null -> Skin.dynamic!!.line
    Skin.dark -> Color(0xFF2B2F37)
    else -> Color(0xFFDCE0E6)
}

// Text
val TextHigh: Color get() = Skin.dynamic?.textHigh
    ?: if (Skin.dark) Color(0xFFEDEFF3) else Color(0xFF14161A)
val TextMid: Color get() = Skin.dynamic?.textMid
    ?: if (Skin.dark) Color(0xFF9AA1AD) else Color(0xFF5C6472)
val TextLow: Color get() = Skin.dynamic?.textLow
    ?: if (Skin.dark) Color(0xFF666D7A) else Color(0xFF8B93A1)

// Signals. The accent darkens slightly on light so it still passes against white.
// The accent follows the wallpaper even in OLED black, which is the nicest of the two
// together: a true black backdrop carrying your own colour.
val Accent: Color get() = Skin.dynamic?.accent
    ?: if (Skin.dark) Color(0xFF7C6CFF) else Color(0xFF5A46F0)
val AccentWash: Color get() = Skin.dynamic?.accentWash
    ?: if (Skin.dark) Color(0xFF23204A) else Color(0xFFE7E4FF)
val Alert: Color get() = if (Skin.dark) Color(0xFFE05561) else Color(0xFFD03A48)
val Good: Color get() = if (Skin.dark) Color(0xFF4FBF87) else Color(0xFF2E9B67)

/**
 * The wash behind the orb's arc and the translation bubble.
 *
 * Always dark, never derived from the theme. It used to be `Ink`, which is very nearly
 * white in light mode — so the thing meant to dim the screen behind a menu instead threw
 * a 55% white sheet over it. In a dark room at night that is a flashbang, and it is the
 * exact opposite of what a scrim is for. It is also deliberately gentler than it was:
 * enough to push the page back, not enough to hide it.
 */
val Scrim: Color get() = Color(0xFF07080A)

/** Text and glyphs drawn ON the accent. Always light: the accent stays dark enough. */
val OnAccent: Color get() = Skin.dynamic?.onAccent ?: Color(0xFFFFFFFF)

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
