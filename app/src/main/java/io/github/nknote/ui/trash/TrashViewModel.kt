package io.github.nknote.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(private val repo: NoteRepository) : ViewModel() {

    val deletedNotes: StateFlow<List<Note>> =
        repo.observeDeletedNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: Int) = viewModelScope.launch { repo.restoreNote(id) }
    fun deleteForever(id: Int) = viewModelScope.launch { repo.permanentlyDelete(id) }
    fun emptyTrash() = viewModelScope.launch { repo.emptyTrash() }
}