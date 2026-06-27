package io.github.nknote.ui.components

import androidx.compose.foundation.layout.PaddingValues
import io.github.nknote.ui.theme.NkSpacing

/**
 * Shared list `PaddingValues`: top from the Scaffold, bottom = scaffold bottom + one extra
 * `NkSpacing.xl` (24dp) for breathing room, sides = `NkSpacing.lg` (16dp).
 *
 * Callers pair this with `verticalArrangement = Arrangement.spacedBy(NkSpacing.md)` to adopt
 * the shared list pattern. Screens with a FAB needing extra bottom clearance (Home) keep their
 * own contentPadding so the FAB doesn't overlap the last item.
 */
fun nkListPadding(padding: PaddingValues): PaddingValues = PaddingValues(
    top = padding.calculateTopPadding(),
    bottom = padding.calculateBottomPadding() + NkSpacing.xl,
    start = NkSpacing.lg,
    end = NkSpacing.lg
)
