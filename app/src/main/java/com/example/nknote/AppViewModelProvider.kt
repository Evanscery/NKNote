package com.example.nknote

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nknote.ui.pages.NoteEditViewModel
import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nknote.ui.pages.MainFrameViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory{
            initializer {
                NoteEditViewModel(inventoryApplication().container.itemsRepository)
            }

        // Initializer for MainFrameViewModel
        initializer {
            MainFrameViewModel(inventoryApplication().container.itemsRepository)
        }
    }
}

fun CreationExtras.inventoryApplication(): NKNoteApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as NKNoteApplication)
