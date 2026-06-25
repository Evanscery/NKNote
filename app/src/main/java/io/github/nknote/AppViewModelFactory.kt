package io.github.nknote

import android.content.Context
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nknote.core.AppContainer
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepository
import io.github.nknote.ui.editor.EditorViewModel
import io.github.nknote.ui.explore.ExploreViewModel
import io.github.nknote.ui.home.HomeViewModel
import io.github.nknote.ui.trash.TrashViewModel

/**
 * Single [viewModelFactory] for the app. Keeps DI manual & lightweight (no Hilt/Koin).
 * Uses [CreationExtras] to reach the [NkNoteApplication] / [AppContainer].
 */
object AppViewModelFactory {

    private fun container(extras: CreationExtras): AppContainer {
        val app = extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as NkNoteApplication
        return app.container
    }

    val factory = viewModelFactory {
        initializer { HomeViewModel(container(this).noteRepository) }
        initializer { TrashViewModel(container(this).noteRepository) }
        initializer { ExploreViewModel(container(this).noteRepository) }
    }

    fun editorFactory(noteId: Int, imageStore: ImageStore, repo: NoteRepository) =
        viewModelFactory {
            initializer { EditorViewModel(noteId, imageStore, repo) }
        }
}

/** Convenience to reach the container from a Composable context. */
fun Context.appContainer(): AppContainer =
    (applicationContext as NkNoteApplication).container