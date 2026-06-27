package io.github.nknote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SageLight,
    onPrimary = Color.White,
    primaryContainer = SageContainerLight,
    onPrimaryContainer = OnSageContainerLight,
    secondary = ClayLight,
    onSecondary = Color.White,
    secondaryContainer = CardAltLight,
    onSecondaryContainer = InkLight,
    tertiary = SageLight,
    onTertiary = Color.White,
    background = PaperLight,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = CardAltLight,
    onSurfaceVariant = InkMutedLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = CardAltLight,
    surfaceContainerHighest = CardAltLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
    error = ErrorLight,
    onError = OnErrorLight,
    scrim = Color(0x99000000)
)

private val DarkColors = darkColorScheme(
    primary = SageDark,
    onPrimary = Color(0xFF16211C),
    primaryContainer = SageContainerDark,
    onPrimaryContainer = OnSageContainerDark,
    secondary = ClayDark,
    onSecondary = Color(0xFF241510),
    secondaryContainer = CardAltDark,
    onSecondaryContainer = InkDark,
    tertiary = SageDark,
    onTertiary = Color(0xFF16211C),
    background = PaperDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = CardAltDark,
    onSurfaceVariant = InkMutedDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardAltDark,
    surfaceContainerHighest = CardAltDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
    scrim = Color(0xCC000000)
)

/**
 * Resolved dark-theme flag provided by [io.github.nknote.ui.navigation.NkNoteApp] from
 * [io.github.nknote.core.AppContainer.themeMode]. Per-screen [NkNoteTheme] calls read this as
 * their default so they inherit the user's choice instead of re-reading the system value (which
 * would override the Settings toggle). `null` (no provider present, e.g. previews / tests) falls
 * back to the system dark setting, preserving standalone `NkNoteTheme { ... }` behavior.
 */
val LocalDarkTheme = staticCompositionLocalOf<Boolean?> { null }

@Composable
fun NkNoteTheme(
    darkTheme: Boolean = LocalDarkTheme.current ?: isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Intentionally NOT using dynamicColor: the brand palette is part of the identity,
    // and Material You defaults are exactly the "default compose" look we avoid.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NkTypography,
        shapes = NkShapes.material,
        content = content
    )
}