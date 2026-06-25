package io.github.nknote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.nknote.ui.editor.EditorPage
import io.github.nknote.ui.explore.ExplorePage
import io.github.nknote.ui.home.HomePage
import io.github.nknote.ui.import_.ImportPage
import io.github.nknote.ui.random.RandomPage
import io.github.nknote.ui.settings.SettingsPage
import io.github.nknote.ui.tools.ToolsPage
import io.github.nknote.ui.trash.TrashPage
import io.github.nknote.ui.viewer.ImageViewerPage
import java.net.URLEncoder

@Composable
fun NkNoteApp() {
    val navController = rememberNavController()
    val nav = remember(navController) {
        NkNoteNavigation(
            back = { navController.popBackStack() },
            toEditor = { id -> navController.navigate("editor/${id ?: Destination.Editor.ARG_NOTE_ID_DEFAULT}") },
            toViewer = { path ->
                val encoded = URLEncoder.encode(path, "UTF-8")
                navController.navigate("viewer/$encoded")
            },
            toTrash = { navController.navigate(Destination.Trash.route) },
            toExplore = { navController.navigate(Destination.Explore.route) },
            toTools = { navController.navigate(Destination.Tools.route) },
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

    NavHost(navController = navController, startDestination = Destination.Home.route) {
        composable(Destination.Home.route) {
            HomePage(nav = nav, openNote = { id -> nav.toEditor(id) })
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
        composable(Destination.Explore.route) { ExplorePage(nav = nav, openNote = { nav.toEditor(it) }) }
        composable(Destination.Tools.route) { ToolsPage(nav = nav) }
        composable(Destination.Random.route) { RandomPage(nav = nav) }
        composable(Destination.Settings.route) { SettingsPage(nav = nav) }
        composable(Destination.Import.route) { ImportPage(nav = nav) }
    }
}