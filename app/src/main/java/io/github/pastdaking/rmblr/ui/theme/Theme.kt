package io.github.pastdaking.rmblr.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class ThemeChoice(val label: String, val blurb: String) {
    SYSTEM("System", "Follow the phone."),
    LIGHT("Light", "Always light."),
    DARK("Dark", "Always dark."),
    AMOLED("OLED black", "True black. Switches the pixels off on an OLED screen.")
}

/** Which skin to wear. Stored on its own so the overlay service can read it too. */
class ThemePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("rmblr_theme", Context.MODE_PRIVATE)

    var choice: ThemeChoice
        get() = ThemeState.choice ?: stored()
        set(value) {
            prefs.edit().putString("choice", value.name).apply()
            ThemeState.choice = value
        }

    /** Material You: take the palette from the wallpaper. Android 12 and up. */
    var dynamic: Boolean
        get() = ThemeState.dynamic ?: prefs.getBoolean("dynamic", DYNAMIC_SUPPORTED)
        set(value) {
            prefs.edit().putBoolean("dynamic", value).apply()
            ThemeState.dynamic = value
        }

    private fun stored(): ThemeChoice =
        runCatching { ThemeChoice.valueOf(prefs.getString("choice", "SYSTEM")!!) }
            .getOrDefault(ThemeChoice.SYSTEM)
}

/**
 * The chosen skin, as snapshot state.
 *
 * The picker used to write to SharedPreferences and nothing recomposed, because a
 * preference read is not observable — which is why the whole light/dark mechanism sat
 * in the codebase unreachable, with no way to reach it from Settings and no way for a
 * change to take effect if there had been. Holding the choice here as well means
 * tapping an option repaints the app on the spot.
 */
object ThemeState {
    var choice by mutableStateOf<ThemeChoice?>(null)
    var dynamic by mutableStateOf<Boolean?>(null)
}

/** Material You only exists from Android 12. Below that the switch is not offered. */
val DYNAMIC_SUPPORTED: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePrefs(context) }

    val choice = ThemeState.choice ?: themePrefs.choice
    val useDark = when (choice) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK, ThemeChoice.AMOLED -> true
        ThemeChoice.SYSTEM -> darkTheme
    }

    // Set before anything reads a token, so the first frame is already the right skin.
    Skin.dark = useDark
    Skin.amoled = choice == ThemeChoice.AMOLED

    val wallpaper = if (DYNAMIC_SUPPORTED && (ThemeState.dynamic ?: themePrefs.dynamic)) {
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    Skin.dynamic = wallpaper?.let {
        DynamicPalette(
            ink = it.background,
            surface = it.surface,
            raised = it.surfaceVariant,
            line = it.outlineVariant,
            textHigh = it.onBackground,
            textMid = it.onSurfaceVariant,
            textLow = it.outline,
            accent = it.primary,
            accentWash = it.primaryContainer,
            onAccent = it.onPrimary
        )
    }

    val scheme = wallpaper ?: if (useDark) {
        darkColorScheme(
            primary = Accent,
            onPrimary = OnAccent,
            primaryContainer = AccentWash,
            onPrimaryContainer = Accent,
            secondary = TextMid,
            onSecondary = Ink,
            secondaryContainer = Raised,
            onSecondaryContainer = TextHigh,
            tertiary = Good,
            onTertiary = Ink,
            background = Ink,
            onBackground = TextHigh,
            surface = Surface,
            onSurface = TextHigh,
            surfaceVariant = Raised,
            onSurfaceVariant = TextMid,
            outline = Line,
            outlineVariant = Line,
            error = Alert,
            onError = OnAccent
        )
    } else {
        lightColorScheme(
            primary = Accent,
            onPrimary = OnAccent,
            primaryContainer = AccentWash,
            onPrimaryContainer = Accent,
            secondary = TextMid,
            onSecondary = OnAccent,
            secondaryContainer = Raised,
            onSecondaryContainer = TextHigh,
            tertiary = Good,
            onTertiary = OnAccent,
            background = Ink,
            onBackground = TextHigh,
            surface = Surface,
            onSurface = TextHigh,
            surfaceVariant = Raised,
            onSurfaceVariant = TextMid,
            outline = Line,
            outlineVariant = Line,
            error = Alert,
            onError = OnAccent
        )
    }

    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
