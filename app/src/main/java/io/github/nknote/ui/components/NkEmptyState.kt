package io.github.nknote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.nknote.ui.theme.NkSpacing

/**
 * The shared empty-state pattern.
 *
 * - [fillMaxSize] = true (default): full-screen centered message — Home / Trash style.
 * - [fillMaxSize] = false: an inline `fillMaxWidth` block with vertical padding, for use
 *   inside a `LazyColumn`/`Column` (Explore's "on this day" block, Calendar's day list).
 * - [icon] renders above the message, muted, when provided.
 */
@Composable
fun NkEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fillMaxSize: Boolean = true
) {
    val boxModifier =
        if (fillMaxSize) modifier.fillMaxSize()
        else modifier.fillMaxWidth().padding(vertical = NkSpacing.xl)
    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(NkSpacing.md))
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
