package io.github.nknote.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.nknote.NkNoteApplication
import io.github.nknote.R
import io.github.nknote.core.ThemeMode
import io.github.nknote.ui.calendar.CalendarPage
import io.github.nknote.ui.components.LocalSnackbarHostState
import io.github.nknote.ui.components.NkDrawerItem
import io.github.nknote.ui.components.NkDrawerSection
import io.github.nknote.ui.components.NkDrawerSheet
import io.github.nknote.ui.components.NkSnackbarHost
import io.github.nknote.ui.editor.EditorPage
import io.github.nknote.ui.explore.ExplorePage
import io.github.nknote.ui.home.HomePage
import io.github.nknote.ui.import_.ImportPage
import io.github.nknote.ui.random.RandomPage
import io.github.nknote.ui.reader.NoteReadPage
import io.github.nknote.ui.settings.SettingsPage
import io.github.nknote.ui.theme.LocalDarkTheme
import io.github.nknote.ui.theme.NkMotion
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.trash.TrashPage
import io.github.nknote.ui.viewer.ImageViewerPage
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun NkNoteApp() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as NkNoteApplication).container

    // Single source of truth for theme mode (read synchronously in AppContainer's constructor,
    // so cold start has the right value with no flash). Resolved here into the boolean NkNoteTheme
    // needs, then published via LocalDarkTheme so per-screen NkNoteTheme { ... } calls inherit it
    // instead of re-reading isSystemInDarkTheme() (which would ignore the user's Settings choice).
    val themeMode by appContainer.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val navController = rememberNavController()
    val nav = remember(navController) {
        NkNoteNavigation(
            back = { navController.popBackStack() },
            toEditor = { id -> navController.navigate("editor/${id ?: Destination.Editor.ARG_NOTE_ID_DEFAULT}") },
            toReader = { id -> navController.navigate("reader/$id") },
            toViewer = { path ->
                val encoded = URLEncoder.encode(path, "UTF-8")
                navController.navigate("viewer/$encoded")
            },
            toTrash = { navController.navigate(Destination.Trash.route) },
            toExplore = { navController.navigate(Destination.Explore.route) },
            toCalendar = { navController.navigate(Destination.Calendar.route) },
            toRandom = { navController.navigate(Destination.Random.route) },
            toSettings = { navController.navigate(Destination.Settings.route) },
            toImport = { navController.navigate(Destination.Import.route) },
            toHomeAndClear = {
                navController.navigate(Destination.Home.route) {
                    popUpTo(Destination.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    // App-scoped snackbar host: a snackbar (and its undo action) survives navigating away
    // from the screen that showed it.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    val journalTitle = stringResource(R.string.drawer_section_journal)
    val toolsTitle = stringResource(R.string.drawer_section_tools)
    val systemTitle = stringResource(R.string.drawer_section_system)
    val homeLabel = stringResource(R.string.nav_home)
    val calendarLabel = stringResource(R.string.calendar_title)
    val exploreLabel = stringResource(R.string.nav_explore)
    val randomLabel = stringResource(R.string.random_title)
    val importLabel = stringResource(R.string.import_title)
    val trashLabel = stringResource(R.string.nav_trash)
    val settingsLabel = stringResource(R.string.nav_settings)

    val sections = listOf(
        NkDrawerSection(
            title = journalTitle,
            items = listOf(
                NkDrawerItem(homeLabel, Icons.Filled.Home) { closeDrawer(); nav.toHomeAndClear() },
                NkDrawerItem(calendarLabel, Icons.Filled.CalendarMonth) { closeDrawer(); nav.toCalendar() },
                NkDrawerItem(exploreLabel, Icons.Filled.Explore) { closeDrawer(); nav.toExplore() }
            )
        ),
        NkDrawerSection(
            title = toolsTitle,
            items = listOf(
                NkDrawerItem(randomLabel, Icons.Filled.Casino) { closeDrawer(); nav.toRandom() },
                NkDrawerItem(importLabel, Icons.Filled.FileOpen) { closeDrawer(); nav.toImport() }
            )
        ),
        NkDrawerSection(
            title = systemTitle,
            items = listOf(
                NkDrawerItem(trashLabel, Icons.Filled.DeleteOutline) { closeDrawer(); nav.toTrash() },
                NkDrawerItem(settingsLabel, Icons.Filled.Settings) { closeDrawer(); nav.toSettings() }
            )
        )
    )

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalDrawerState provides drawerState,
        LocalSnackbarHostState provides snackbarHostState
    ) {
        NkNoteTheme(darkTheme = darkTheme) {
            // gesturesEnabled = false: the swipe-open gesture would otherwise intercept horizontal
            // drags on inner composables (editor FormatBar, ImageViewer pinch/pan). The hamburger
            // icon (wired via LocalDrawerState in each primary screen's NkTopAppBar) opens the drawer.
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = false,
                drawerContent = {
                    ModalDrawerSheet(drawerContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                        NkDrawerSheet(sections = sections)
                    }
                }
            ) {
                Box {
                // Fade-through with a subtle directional slide (1/16 width) — replaces
                // navigation-compose's default 700 ms cross-fade with the app's motion language.
                NavHost(
                    navController = navController,
                    startDestination = Destination.Home.route,
                    enterTransition = {
                        fadeIn(tween(NkMotion.DurationMedium, easing = NkMotion.StandardEasing)) +
                            slideInHorizontally(tween(NkMotion.DurationMedium, easing = NkMotion.StandardEasing)) { it / 16 }
                    },
                    exitTransition = { fadeOut(tween(NkMotion.DurationShort)) },
                    popEnterTransition = { fadeIn(tween(NkMotion.DurationMedium, easing = NkMotion.StandardEasing)) },
                    popExitTransition = {
                        fadeOut(tween(NkMotion.DurationShort)) +
                            slideOutHorizontally(tween(NkMotion.DurationMedium, easing = NkMotion.StandardEasing)) { it / 16 }
                    }
                ) {
                    composable(Destination.Home.route) {
                        HomePage(nav = nav, openNote = { id -> nav.toReader(id) })
                    }
                    composable(
                        route = Destination.Reader.routeWithArg,
                        arguments = listOf(navArgument(Destination.Reader.ARG_NOTE_ID) { type = NavType.IntType })
                    ) { backStack ->
                        val noteId = backStack.arguments?.getInt(Destination.Reader.ARG_NOTE_ID) ?: return@composable
                        NoteReadPage(noteId = noteId, nav = nav)
                    }
                    composable(
                        route = Destination.Editor.routeWithArg,
                        arguments = listOf(navArgument(Destination.Editor.ARG_NOTE_ID) {
                            type = NavType.IntType; defaultValue = -1
                        })
                    ) { backStack ->
                        val noteId = backStack.arguments?.getInt(Destination.Editor.ARG_NOTE_ID) ?: -1
                        EditorPage(noteId = if (noteId <= 0) null else noteId, nav = nav)
                    }
                    composable(
                        route = Destination.Viewer.routeWithArg,
                        arguments = listOf(navArgument(Destination.Viewer.ARG_PATH) { type = NavType.StringType })
                    ) { backStack ->
                        val raw = backStack.arguments?.getString(Destination.Viewer.ARG_PATH).orEmpty()
                        val path = java.net.URLDecoder.decode(raw, "UTF-8")
                        ImageViewerPage(imagePath = path, nav = nav)
                    }
                    composable(Destination.Trash.route) { TrashPage(nav = nav) }
                    composable(Destination.Explore.route) { ExplorePage(nav = nav, openNote = { nav.toReader(it) }) }
                    composable(Destination.Calendar.route) { CalendarPage(nav = nav, openNote = { nav.toReader(it) }) }
                    composable(Destination.Random.route) { RandomPage(nav = nav) }
                    composable(Destination.Settings.route) { SettingsPage(nav = nav) }
                    composable(Destination.Import.route) { ImportPage(nav = nav) }
                }
                NkSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                )
                }
            }
        }
    }
}