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

// Surfaces, furthest back to closest (M3 Expressive tonal steps).
// Ink = surface, Surface = container-low, Raised = container, RaisedHigh and
// RaisedHighest step further up. A tonal step replaces an outline wherever it can.
val Ink: Color get() = when {
    Skin.amoled -> Color(0xFF000000)
    Skin.dynamic != null -> Skin.dynamic!!.ink
    Skin.dark -> Color(0xFF0A0B0D)
    else -> Color(0xFFF7F8FA)
}
val Surface: Color get() = when {
    Skin.amoled -> Color(0xFF0B0B0D)
    Skin.dynamic != null -> Skin.dynamic!!.surface
    Skin.dark -> Color(0xFF111217)
    else -> Color(0xFFF1F2F8)
}
val Raised: Color get() = when {
    Skin.amoled -> Color(0xFF16171A)
    Skin.dynamic != null -> Skin.dynamic!!.raised
    Skin.dark -> Color(0xFF16171D)
    else -> Color(0xFFECEEF6)
}
val RaisedHigh: Color get() = when {
    Skin.amoled -> Color(0xFF1C1D21)
    Skin.dynamic != null -> Skin.dynamic!!.raised
    Skin.dark -> Color(0xFF1F2027)
    else -> Color(0xFFE6E8F1)
}
val RaisedHighest: Color get() = when {
    Skin.amoled -> Color(0xFF26272C)
    Skin.dynamic != null -> Skin.dynamic!!.line
    Skin.dark -> Color(0xFF292A32)
    else -> Color(0xFFE0E2EC)
}
val Line: Color get() = when {
    Skin.amoled -> Color(0xFF2E2F36)
    Skin.dynamic != null -> Skin.dynamic!!.line
    Skin.dark -> Color(0xFF3D3E48)
    else -> Color(0xFFC8C9D3)
}

// Text
val TextHigh: Color get() = Skin.dynamic?.textHigh
    ?: if (Skin.dark) Color(0xFFE6E6EE) else Color(0xFF0A0B0D)
val TextMid: Color get() = Skin.dynamic?.textMid
    ?: if (Skin.dark) Color(0xFFC5C6D0) else Color(0xFF46474F)
val TextLow: Color get() = Skin.dynamic?.textLow
    ?: if (Skin.dark) Color(0xFF8F909B) else Color(0xFF777882)

// Signals. Accent is the brand fill; AccentText is the lighter indigo used for text and
// small controls on dark surfaces. The accent follows the wallpaper when dynamic colour is on.
val Accent: Color get() = Skin.dynamic?.accent
    ?: if (Skin.dark) Color(0xFF7C6CFF) else Color(0xFF5A46F0)
val AccentText: Color get() = Skin.dynamic?.accent
    ?: if (Skin.dark) Color(0xFFA79CFF) else Color(0xFF5A46F0)
val AccentWash: Color get() = Skin.dynamic?.accentWash
    ?: if (Skin.dark) Color(0xFF3A2FB4) else Color(0xFFE3DEFF)
val OnAccentWash: Color get() = Skin.dynamic?.textHigh
    ?: if (Skin.dark) Color(0xFFE3DEFF) else Color(0xFF160A70)
val SecondaryWash: Color get() = if (Skin.dark) Color(0xFF3F3E5C) else Color(0xFFE3E1F7)
val OnSecondaryWash: Color get() = if (Skin.dark) Color(0xFFE3E1F7) else Color(0xFF1A1933)
val Teal: Color get() = if (Skin.dark) Color(0xFF8FD3E0) else Color(0xFF0F6A78)
val TealWash: Color get() = if (Skin.dark) Color(0xFF004E5A) else Color(0xFFC3EAF1)

/** Recording red. Same in light and dark so white glyphs keep their contrast. */
val RecRed: Color get() = Color(0xFFD6362F)
val Alert: Color get() = if (Skin.dark) Color(0xFFFF8A80) else Color(0xFFD6362F)
val AlertWash: Color get() = if (Skin.dark) Color(0xFF8C1A1A) else Color(0xFFFFDAD5)
val OnAlertWash: Color get() = if (Skin.dark) Color(0xFFFFDAD5) else Color(0xFF410003)
val Good: Color get() = if (Skin.dark) Color(0xFF7FD9A1) else Color(0xFF1D7A4B)
val GoodWash: Color get() = if (Skin.dark) Color(0xFF0B5230) else Color(0xFFC4F0D5)
val OnGoodWash: Color get() = if (Skin.dark) Color(0xFFC4F0D5) else Color(0xFF00210F)

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
