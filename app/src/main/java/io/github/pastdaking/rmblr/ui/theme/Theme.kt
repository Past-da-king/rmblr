package io.github.pastdaking.rmblr.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/** Which skin to wear. Stored on its own so the overlay service can read it too. */
class ThemePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("rmblr_theme", Context.MODE_PRIVATE)

    var choice: ThemeChoice
        get() = runCatching { ThemeChoice.valueOf(prefs.getString("choice", "SYSTEM")!!) }
            .getOrDefault(ThemeChoice.SYSTEM)
        set(value) = prefs.edit().putString("choice", value.name).apply()
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePrefs(context) }

    val useDark = when (themePrefs.choice) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
        ThemeChoice.SYSTEM -> darkTheme
    }

    // Set before anything reads a token, so the first frame is already the right skin.
    Skin.dark = useDark

    val scheme = if (useDark) {
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
