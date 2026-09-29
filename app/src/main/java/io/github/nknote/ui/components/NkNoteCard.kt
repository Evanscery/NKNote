package io.github.nknote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

/**
 * The shared "note card" surface (tier-1 card: [NkShapes.mediumLarge] radius,
 * [NkSpacing.lg] padding) used by Home, Trash, Explore, and the calendar's
 * selected-day rows.
 *
 * - `clip` is applied BEFORE the click modifier so the ripple is bounded by the
 *   rounded shape.
 * - When [onLongClick] is null the card uses a plain `clickable`, so no long-press
 *   gesture is consumed (rows without a long-press action stay transparent to it).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NkNoteCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = NkShapes.mediumLarge,
    color: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(NkSpacing.lg),
    content: @Composable () -> Unit
) {
    val clickModifier =
        if (onLongClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        else Modifier.clickable(onClick = onClick)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color, shape)
            .then(clickModifier)
            .padding(contentPadding)
    ) { content() }
}
