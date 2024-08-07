package com.example.nknote.ui.pages

import ItemsRepository
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nknote.data.NoteItem
import com.example.nknote.ui.navigation.Destinations
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NoteCheckViewModel(savedStateHandle: SavedStateHandle,
                         private val itemsRepository: ItemsRepository):ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle[Destinations.NoteCheckPage.args])

    val uiState: StateFlow<NoteCheckUiState> =
        itemsRepository.getItemStream(itemId)
            .filterNotNull()
            .map {
                NoteCheckUiState(noteItem = it)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = NoteCheckUiState()
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * UI state for ItemDetailsScreen
 */
data class NoteCheckUiState(
    val noteItem: NoteItem = NoteItem()
)