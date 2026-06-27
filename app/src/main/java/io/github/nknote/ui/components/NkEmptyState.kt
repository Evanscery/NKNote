package io.github.nknote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * The shared "fillMaxSize + centered message" empty-state pattern used by Home and Trash.
 *
 * Inline empty states inside a `LazyColumn` (e.g. Explore's "on this day" empty block) use a
 * `fillMaxWidth` box and stay inline — they are NOT replaced by this composable.
 */
@Composable
fun NkEmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
