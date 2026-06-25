package io.github.nknote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun NkNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Intentionally NOT using dynamicColor: the brand palette is part of the identity,
    // and Material You defaults are exactly the "default compose" look we avoid.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NkTypography,
        shapes = NkShapes,
        content = content
    )
}