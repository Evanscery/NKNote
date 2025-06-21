package com.example.nknote.ui.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nknote.data.entities.Note
import com.example.nknote.data.entities.Tag
import com.example.nknote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel to retrieve all items in the Room database.
 */
class MainFrameViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    init {
        viewModelScope.launch {
            noteRepository.getAllNotes().collect { noteList ->
                _notes.value = noteList
            }
        }

        viewModelScope.launch {
            noteRepository.getAllTags().collect { tagList ->
                _tags.value = tagList
            }
        }
    }

    suspend fun deleteNote(id: Int) {
        noteRepository.deleteNoteById(id)
    }

    suspend fun searchNotes(query: String) {
        viewModelScope.launch {
            noteRepository.searchNotes(query).collect { noteList ->
                _notes.value = noteList
            }
        }
    }

    suspend fun getNotesByTag(tagId: String) {
        viewModelScope.launch {
            noteRepository.getNotesForTag(tagId).collect { noteList ->
                _notes.value = noteList
            }
        }
    }

    suspend fun refreshNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes().collect { noteList ->
                _notes.value = noteList
            }
        }
    }
}

/**
 * Ui State for HomeScreen
 */
data class MainFrameUiState(val itemList: List<Note> = listOf())