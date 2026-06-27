package io.github.nknote

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nknote.core.AppContainer
import io.github.nknote.ui.editor.EditorViewModel
import io.github.nknote.ui.explore.ExploreViewModel
import io.github.nknote.ui.home.HomeViewModel
import io.github.nknote.ui.import_.ImportViewModel
import io.github.nknote.ui.trash.TrashViewModel
import io.github.nknote.ui.viewer.ImageViewerViewModel

/**
 * Single [viewModelFactory] for the app. Keeps DI manual & lightweight (no Hilt/Koin).
 * Uses [CreationExtras] to reach the [NkNoteApplication] / [AppContainer] and the
 * [SavedStateHandle] (for the editor draft), so no Composable resolves the container itself.
 */
object AppViewModelFactory {

    private fun container(extras: CreationExtras): AppContainer {
        val app = extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as NkNoteApplication
        return app.container
    }

    private fun CreationExtras.application(): Application =
        this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

    val factory = viewModelFactory {
        initializer { HomeViewModel(container(this).noteRepository) }
        initializer { TrashViewModel(container(this).noteRepository) }
        initializer { ExploreViewModel(container(this).noteRepository) }
        initializer { ImportViewModel(application(), container(this).noteRepository) }
        initializer { ImageViewerViewModel(application(), container(this).imageStore) }
    }

    /**
     * Factory for [EditorViewModel]. Resolves the [AppContainer] deps (imageStore + repository)
     * internally via [CreationExtras] and creates a [SavedStateHandle] for the editor draft —
     * the Composable only passes the [noteId], so it never resolves the container itself.
     */
    fun editorFactory(noteId: Int) = viewModelFactory {
        initializer {
            val c = container(this)
            EditorViewModel(noteId, c.imageStore, c.noteRepository, createSavedStateHandle())
        }
    }
}
