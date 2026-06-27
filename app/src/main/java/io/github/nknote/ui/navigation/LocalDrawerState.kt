package io.github.nknote.ui.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The single [DrawerState] for the global [androidx.compose.material3.ModalNavigationDrawer]
 * hoisted to [NkNoteApp]. Primary screens read it to wire their hamburger [io.github.nknote.ui.components.NkTopAppBar]
 * (or a custom top bar, as in [io.github.nknote.ui.home.HomePage]); the Editor and Viewer screens
 * do NOT read it — they keep a back arrow since they are reached by direct navigation, not from the drawer.
 */
val LocalDrawerState = staticCompositionLocalOf<DrawerState> { error("no DrawerState") }
