package com.example.nknote.ui.pages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nknote.data.entities.Note
import com.example.nknote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteCheckViewModel(
    private val noteId: Int,
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val _noteUiState = MutableStateFlow(NoteCheckUiState())
    val noteUiState: StateFlow<NoteCheckUiState> = _noteUiState.asStateFlow()

    init {
        viewModelScope.launch {
            noteRepository.getNoteById(noteId).collect { note ->
                if (note != null) {
                    _noteUiState.update { currentState ->
                        currentState.copy(
                            title = note.title,
                            description = note.description,
                            content = note.content,
                            date = note.date,
                            weather = note.weather,
                            coverImageId = note.coverImageId,
                            syncStatus = note.syncStatus,
                            version = note.version,
                            createdAt = note.createdAt,
                            updatedAt = note.updatedAt
                        )
                    }
                }
            }
        }
    }

    suspend fun deleteNote() {
        noteRepository.getNoteById(noteId).collect { note ->
            note?.let {
                noteRepository.deleteNote(it)
            }
        }
    }

    companion object {
        fun provideFactory(
            noteId: Int,
            noteRepository: NoteRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NoteCheckViewModel(noteId, noteRepository) as T
            }
        }
    }
}

data class NoteCheckUiState(
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val date: String = "",
    val weather: String = "",
    val coverImageId: String? = null,
    val syncStatus: Int = 0,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)