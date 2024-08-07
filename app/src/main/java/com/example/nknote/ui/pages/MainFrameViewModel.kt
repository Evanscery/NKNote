package com.example.nknote.ui.pages

import ItemsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nknote.data.NoteItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel to retrieve all items in the Room database.
 */
class MainFrameViewModel(private val itemsRepository: ItemsRepository) : ViewModel() {
    /**
     * Holds main frame ui state. The list of items are retrieved from [ItemsRepository] and mapped to
     * [MainFrameUiState]
     */
    val mainFrameUiState: StateFlow<MainFrameUiState> =
        itemsRepository.getAllItemsStream().map{ MainFrameUiState(it)}
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = MainFrameUiState()
            )
    suspend fun getItem(id:String) : Flow<NoteItem?> {
        return itemsRepository.getItemStream(id)
    }

    suspend fun deleteItemById(id : String) = itemsRepository.deleteById(id)

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Ui State for HomeScreen
 */
data class MainFrameUiState(val itemList: List<NoteItem> = listOf())