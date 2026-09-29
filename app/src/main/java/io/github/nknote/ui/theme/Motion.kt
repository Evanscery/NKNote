package io.github.nknote.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Motion tokens — one duration/easing vocabulary for the whole app, so navigation
 * transitions, month flips, and expand/collapse all share the same feel.
 */
object NkMotion {
    /** Exits and small fades. */
    const val DurationShort = 200

    /** Enters, content transitions. */
    const val DurationMedium = 250

    val StandardEasing: Easing = FastOutSlowInEasing
}
