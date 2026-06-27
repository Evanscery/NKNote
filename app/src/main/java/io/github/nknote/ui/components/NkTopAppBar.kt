package io.github.nknote.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Theme-unified top app bar.
 *
 * - `containerColor = MaterialTheme.colorScheme.background` (warm paper, not the M3 surface tint).
 * - If [drawerState] is non-null, the navigation icon is a hamburger that opens the drawer;
 *   otherwise, if [onBack] is non-null, it is a back arrow.
 * - [actions] is rendered in the M3 actions slot.
 *
 * Editor and Viewer keep the default `drawerState = null` + `onBack = nav.back` so they show a
 * back arrow, not a drawer hamburger (they are reached by direct navigation, not from the drawer).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NkTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    drawerState: DrawerState? = null
) {
    val scope = rememberCoroutineScope()
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            when {
                drawerState != null -> IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                }
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}
