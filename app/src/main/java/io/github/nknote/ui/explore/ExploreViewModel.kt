package io.github.nknote.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExploreViewModel(private val repo: NoteRepository) : ViewModel() {

    private val _todayThisDay = MutableStateFlow<List<Note>>(emptyList())
    val todayThisDay: StateFlow<List<Note>> = _todayThisDay.asStateFlow()

    val totalNotes: StateFlow<Int> =
        repo.observeAllNotes().map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            val md = monthDay()
            repo.observeNotesOnMonthDay(md).collect { _todayThisDay.value = it.filter { n -> n.date != today() } }
        }
    }

    private fun monthDay(): String = LocalDate.now().let { "%02d-%02d".format(it.monthValue, it.dayOfMonth) }
    private fun today(): String = LocalDate.now().toString()
}