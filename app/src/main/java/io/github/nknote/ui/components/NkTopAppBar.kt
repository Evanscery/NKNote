package io.github.nknote.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.nknote.R
import kotlinx.coroutines.launch

/**
 * Theme-unified top app bar.
 *
 * - [containerColor] defaults to `MaterialTheme.colorScheme.background` (warm paper, not the
 *   M3 surface tint); the image viewer passes `Color.Transparent`.
 * - If [drawerState] is non-null, the navigation icon is a hamburger that opens the drawer;
 *   otherwise, if [onBack] is non-null, it is a back arrow.
 * - [centerTitle] switches to [CenterAlignedTopAppBar] (viewer style).
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
    drawerState: DrawerState? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    centerTitle: Boolean = false,
    titleColor: Color = Color.Unspecified
) {
    val scope = rememberCoroutineScope()
    val titleContent: @Composable () -> Unit = { Text(title, color = titleColor) }
    val navigationIcon: @Composable () -> Unit = {
        when {
            drawerState != null -> IconButton(onClick = { scope.launch { drawerState.open() } }) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.a11y_open_menu),
                    tint = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor
                )
            }
            onBack != null -> IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor
                )
            }
        }
    }
    if (centerTitle) {
        CenterAlignedTopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = containerColor)
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
        )
    }
}
