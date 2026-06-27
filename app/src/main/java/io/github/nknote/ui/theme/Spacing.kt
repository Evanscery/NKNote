package io.github.nknote.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 4-based spacing tokens. Use these instead of literal `dp` for padding, `spacedBy`,
 * and `Spacer.height` so layout density is consistent across the app.
 *
 * Values not on this grid (e.g. 6/10/20/28) are component-specific sizing, not spacing,
 * and are intentionally left as literals to keep visuals pixel-identical.
 */
object NkSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}
