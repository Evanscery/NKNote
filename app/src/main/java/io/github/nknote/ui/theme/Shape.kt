package io.github.nknote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Soft, generous radii — a key part of the non-default "gentle" visual identity.
// The Material3 `Shapes` slots (extraSmall/small/medium/large/extraLarge) stay 6/10/16/22/30;
// the in-between tokens (smallMedium/mediumSmall/mediumLarge/largeSmall) cover the radii that
// appear across the UI but don't map onto a Material3 slot, so every call site can resolve to
// a named token instead of a hardcoded `RoundedCornerShape(<n>.dp)`.
object NkShapes {
    val extraSmall = RoundedCornerShape(6.dp)
    val small = RoundedCornerShape(10.dp)
    val smallMedium = RoundedCornerShape(14.dp)
    val mediumSmall = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(16.dp)
    val mediumLarge = RoundedCornerShape(18.dp)
    val largeSmall = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(22.dp)
    val extraLarge = RoundedCornerShape(30.dp)

    /** Material3 `Shapes` binding consumed by [NkNoteTheme]. */
    val material: Shapes = Shapes(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge
    )
}
