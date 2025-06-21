package com.example.nknote

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nknote.data.NoteItemRoomDatabase
import com.example.nknote.data.repository.NoteRepository
import com.example.nknote.data.repository.NoteRepositoryImpl
import com.example.nknote.ui.pages.MainFrameViewModel
import com.example.nknote.ui.pages.NoteEditViewModel
import com.example.nknote.ui.pages.NoteCheckViewModel
import com.example.nknote.ui.navigation.Destinations

class AppViewModelProvider(
    private val noteRepository: NoteRepository,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainFrameViewModel::class.java) -> {
                MainFrameViewModel(noteRepository) as T
            }
            modelClass.isAssignableFrom(NoteEditViewModel::class.java) -> {
                NoteEditViewModel(noteRepository) as T
            }
            modelClass.isAssignableFrom(NoteCheckViewModel::class.java) -> {
                val noteId = savedStateHandle?.get<Int>(Destinations.NoteCheckPage.args) ?: 0
                NoteCheckViewModel(noteId, noteRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        fun provide(context: Context): AppViewModelProvider {
            val database = NoteItemRoomDatabase.getDatabaseObj(context)
            return AppViewModelProvider(
                noteRepository = NoteRepositoryImpl(
                    noteDao = database.noteDao(),
                    tagDao = database.tagDao(),
                    noteTagDao = database.noteTagDao(),
                    imageDao = database.imageDao(),
                    syncRecordDao = database.syncRecordDao()
                )
            )
        }

        val Factory: (Context) -> ViewModelProvider.Factory = { context ->
            provide(context)
        }
    }
}
