package ru.ritm.planner.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import ru.ritm.planner.data.Appearance

val LocalAppearance = staticCompositionLocalOf { Appearance() }
val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
val OnAccent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
val Background: Color @Composable get() = MaterialTheme.colorScheme.background
val Card: Color @Composable get() = MaterialTheme.colorScheme.surface
val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Line: Color @Composable get() = MaterialTheme.colorScheme.outline
val Danger: Color @Composable get() = MaterialTheme.colorScheme.error

fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
}

/** Keep the selected hue while making text legible in either theme, including black/white accents. */
fun readableTone(color: Color, background: Color): Color {
    if (contrastRatio(color, background) >= 4.5f) return color
    val target = if (background.luminance() > .5f) Color.Black else Color.White
    for (step in 1..100) {
        val candidate = lerp(color, target, step / 100f)
        if (contrastRatio(candidate, background) >= 4.5f) return candidate
    }
    return target
}

@Composable
fun RitmTheme(appearance: Appearance = LocalAppearance.current, content: @Composable () -> Unit) {
    val light = appearance.lightTheme
    val background = if (light) Color.White else Color(0xFF090A0B)
    val surface = if (light) Color(0xFFF2F4F7) else Color(0xFF191A1E)
    val foreground = if (light) Color(0xFF191C22) else Color(0xFFF5F5F1)
    val muted = if (light) Color(0xFF626771) else Color(0xFF999BA3)
    val line = if (light) Color(0xFFD5D9E0) else Color(0xFF2B2D32)
    val primary = readableTone(Color(appearance.accent), background)
    val onPrimary = if (contrastRatio(primary, Color.Black) >= contrastRatio(primary, Color.White)) Color.Black else Color.White
    val base = if (light) lightColorScheme() else darkColorScheme()
    val scheme = base.copy(
        primary = primary, onPrimary = onPrimary, primaryContainer = surface, onPrimaryContainer = foreground,
        secondary = primary, onSecondary = onPrimary, secondaryContainer = surface, onSecondaryContainer = foreground,
        tertiary = primary, onTertiary = onPrimary, tertiaryContainer = surface, onTertiaryContainer = foreground,
        background = background, onBackground = foreground, surface = surface, onSurface = foreground,
        surfaceVariant = surface, onSurfaceVariant = muted, outline = line, outlineVariant = line,
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surface, surfaceContainerHighest = surface, surfaceBright = surface, surfaceDim = background,
        surfaceTint = primary, error = if (light) Color(0xFFBA3028) else Color(0xFFFF8D87),
    )
    val view = LocalView.current
    val context = LocalContext.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: context.activity()?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
    }
    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
