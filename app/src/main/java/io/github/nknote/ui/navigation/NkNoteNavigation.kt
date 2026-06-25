package io.github.nknote.ui.navigation

/** Navigation actions passed down to screens. Keeps screens free of NavController coupling. */
class NkNoteNavigation(
    val back: () -> Unit,
    val toEditor: (Int?) -> Unit,
    val toViewer: (String) -> Unit,
    val toTrash: () -> Unit,
    val toRandom: () -> Unit,
    val toExplore: () -> Unit,
    val toImport: () -> Unit,
    val toHomeAndClear: () -> Unit
)