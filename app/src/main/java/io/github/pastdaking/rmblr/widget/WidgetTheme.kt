package io.github.pastdaking.rmblr.widget

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

/**
 * Widget colours. Android 12 and up gets the wallpaper colours, which is what a widget
 * is expected to do; older versions get the RMBLR indigo palette, light and dark with
 * the system.
 */
object RmblrWidgetColors {
    val providers = ColorProviders(
        light = lightColorScheme(
            primary = Color(0xFF5A46F0), onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE3DEFF), onPrimaryContainer = Color(0xFF160A70),
            secondary = Color(0xFF5E5C78), onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE3E1F7), onSecondaryContainer = Color(0xFF1A1933),
            tertiary = Color(0xFF0F6A78), onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFC3EAF1), onTertiaryContainer = Color(0xFF001F25),
            error = Color(0xFFD6362F), onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD5), onErrorContainer = Color(0xFF410003),
            background = Color(0xFFF7F8FA), onBackground = Color(0xFF0A0B0D),
            surface = Color(0xFFECEEF6), onSurface = Color(0xFF0A0B0D),
            surfaceVariant = Color(0xFFE0E2EC), onSurfaceVariant = Color(0xFF46474F),
            outline = Color(0xFF777882)
        ),
        dark = darkColorScheme(
            primary = Color(0xFFA79CFF), onPrimary = Color(0xFF1C0F8A),
            primaryContainer = Color(0xFF3A2FB4), onPrimaryContainer = Color(0xFFE3DEFF),
            secondary = Color(0xFFC8C5E4), onSecondary = Color(0xFF2E2D49),
            secondaryContainer = Color(0xFF3F3E5C), onSecondaryContainer = Color(0xFFE3E1F7),
            tertiary = Color(0xFF8FD3E0), onTertiary = Color(0xFF00363F),
            tertiaryContainer = Color(0xFF004E5A), onTertiaryContainer = Color(0xFFC3EAF1),
            error = Color(0xFFFF8A80), onError = Color(0xFF5A0508),
            errorContainer = Color(0xFF8C1A1A), onErrorContainer = Color(0xFFFFDAD5),
            background = Color(0xFF0A0B0D), onBackground = Color(0xFFE6E6EE),
            surface = Color(0xFF16171D), onSurface = Color(0xFFE6E6EE),
            surfaceVariant = Color(0xFF292A32), onSurfaceVariant = Color(0xFFC5C6D0),
            outline = Color(0xFF8F909B)
        )
    )
}

@Composable
fun RmblrGlanceTheme(dynamic: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S, content: @Composable () -> Unit) {
    GlanceTheme(colors = if (dynamic) GlanceTheme.colors else RmblrWidgetColors.providers, content = content)
}
