package io.github.pastdaking.rmblr.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

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
 * The chosen skin, as snapshot state, so tapping an option repaints the app on the spot.
 */
object ThemeState {
    var choice by mutableStateOf<ThemeChoice?>(null)
    var dynamic by mutableStateOf<Boolean?>(null)
}

/** Material You only exists from Android 12. Below that the switch is not offered. */
val DYNAMIC_SUPPORTED: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The shape scale from the design: 4, 8, 12, 16, 20, 28, 32, 48, full. */
val RmblrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

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
            surface = it.surfaceContainerLow,
            raised = it.surfaceContainer,
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
            primary = AccentText,
            onPrimary = Color(0xFF1C0F8A),
            primaryContainer = AccentWash,
            onPrimaryContainer = OnAccentWash,
            secondary = Color(0xFFC8C5E4),
            onSecondary = Color(0xFF2E2D49),
            secondaryContainer = SecondaryWash,
            onSecondaryContainer = OnSecondaryWash,
            tertiary = Teal,
            onTertiary = Color(0xFF00363F),
            tertiaryContainer = TealWash,
            onTertiaryContainer = Color(0xFFC3EAF1),
            background = Ink,
            onBackground = TextHigh,
            surface = Ink,
            onSurface = TextHigh,
            surfaceVariant = RaisedHigh,
            onSurfaceVariant = TextMid,
            surfaceContainerLowest = Color(0xFF050608),
            surfaceContainerLow = Surface,
            surfaceContainer = Raised,
            surfaceContainerHigh = RaisedHigh,
            surfaceContainerHighest = RaisedHighest,
            outline = TextLow,
            outlineVariant = Line,
            error = Alert,
            onError = Color(0xFF5A0508),
            errorContainer = AlertWash,
            onErrorContainer = OnAlertWash,
            inverseSurface = Color(0xFFE6E6EE),
            inverseOnSurface = Color(0xFF2E2F35),
            scrim = Color(0xFF000000)
        )
    } else {
        lightColorScheme(
            primary = AccentText,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = AccentWash,
            onPrimaryContainer = OnAccentWash,
            secondary = Color(0xFF5E5C78),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = SecondaryWash,
            onSecondaryContainer = OnSecondaryWash,
            tertiary = Teal,
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = TealWash,
            onTertiaryContainer = Color(0xFF001F25),
            background = Ink,
            onBackground = TextHigh,
            surface = Ink,
            onSurface = TextHigh,
            surfaceVariant = RaisedHigh,
            onSurfaceVariant = TextMid,
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Surface,
            surfaceContainer = Raised,
            surfaceContainerHigh = RaisedHigh,
            surfaceContainerHighest = RaisedHighest,
            outline = TextLow,
            outlineVariant = Line,
            error = Alert,
            onError = Color(0xFFFFFFFF),
            errorContainer = AlertWash,
            onErrorContainer = OnAlertWash,
            inverseSurface = Color(0xFF2E2F35),
            inverseOnSurface = Color(0xFFF1F0F7),
            scrim = Color(0xFF0A0B14)
        )
    }

    MaterialTheme(colorScheme = scheme, typography = Typography, shapes = RmblrShapes, content = content)
}
