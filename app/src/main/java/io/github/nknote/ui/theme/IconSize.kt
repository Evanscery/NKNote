package io.github.nknote.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Icon-size tokens. Collapses the ad-hoc 15/18/20/24/28 dp icon sizes to four named steps
 * so every `Modifier.size(...)` on an [androidx.compose.material3.Icon] / IconButton resolves
 * to a design token. The 1–2 dp drift on the former 15/18 values is visually imperceptible.
 */
object NkIconSize {
    val sm = 16.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 28.dp
}
