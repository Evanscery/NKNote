package io.github.nknote.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.nknote.ui.theme.NkShapes

/**
 * The single app-scoped [SnackbarHostState], provided at `NkNoteApp` level so a snackbar
 * (and its undo action) survives navigating away from the screen that showed it.
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState not provided")
}

/** "Ink on paper" inverted snackbar, on-palette, rendered once above the NavHost. */
@Composable
fun NkSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = NkShapes.smallMedium,
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
            actionColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
