package io.github.nknote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class HomeViewModel(private val repo: NoteRepository) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val query = MutableStateFlow("")
    private val _mode = MutableStateFlow(ViewMode.ALL)
    val mode: ViewMode get() = _mode.value

    val notes: StateFlow<List<Note>> =
        combine(repo.observeAllNotes(), _mode, query) { all, m, q ->
            val byMode = when (m) {
                ViewMode.ALL -> all
                ViewMode.TODAY -> all.filter { it.date == todayStr() }
            }
            if (q.isBlank()) byMode else byMode.filter { matchesQuery(it, q) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _todayThisDay = MutableStateFlow<List<Note>>(emptyList())
    val todayThisDay: StateFlow<List<Note>> = _todayThisDay.asStateFlow()

    private val _noteCount = MutableStateFlow(0)
    val noteCount: StateFlow<Int> = _noteCount.asStateFlow()

    private val _notesForDate = MutableStateFlow<List<Note>>(emptyList())
    val notesForDate: StateFlow<List<Note>> = _notesForDate.asStateFlow()

    init {
        viewModelScope.launch { repo.observeAllNotes().collect { _noteCount.value = it.size } }
        viewModelScope.launch {
            repo.observeNotesOnMonthDay(monthDayStr()).collect { _todayThisDay.value = it }
        }
    }

    suspend fun allDates(): List<String> = repo.allDates()

    fun loadNotesForDate(date: String) {
        viewModelScope.launch {
            repo.observeNotesOnDate(date).collect { _notesForDate.value = it }
        }
    }

    fun setQuery(q: String) { query.value = q }
    fun toggleMode() { _mode.value = if (_mode.value == ViewMode.ALL) ViewMode.TODAY else ViewMode.ALL }

    fun moveToTrash(id: Int) = viewModelScope.launch { repo.moveToTrash(id) }

    private fun matchesQuery(note: Note, q: String): Boolean {
        val needle = q.trim()
        if (needle.isEmpty()) return true
        if (note.title.contains(needle, true) || note.excerpt.contains(needle, true)) return true
        // Search the plain text of the RichDocument, not raw JSON
        val plain = runCatching {
            json.decodeFromString<io.github.nknote.model.RichDocument>(note.content).plainText()
        }.getOrNull().orEmpty()
        return plain.contains(needle, true)
    }

    enum class ViewMode { ALL, TODAY }

    companion object {
        private fun todayStr(): String =
            java.time.LocalDate.now().toString() // ISO yyyy-MM-dd
        private fun monthDayStr(): String =
            java.time.LocalDate.now().let { "%02d-%02d".format(it.monthValue, it.dayOfMonth) }
    }
}