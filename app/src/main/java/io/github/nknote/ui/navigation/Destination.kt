package io.github.nknote.ui.navigation

/** Type-safe-ish route definitions. */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Editor : Destination("editor") {
        const val ARG_NOTE_ID = "noteId"
        const val ARG_NOTE_ID_DEFAULT = "-1"
        const val routeWithArg = "editor/{noteId}"
    }
    data object Viewer : Destination("viewer") {
        const val ARG_PATH = "imagePath"
        const val routeWithArg = "viewer/{imagePath}"
    }
    data object Trash : Destination("trash")
    data object Explore : Destination("explore")
    data object Tools : Destination("tools")
    data object Random : Destination("random")
    data object Settings : Destination("settings")
    data object Import : Destination("import")
}