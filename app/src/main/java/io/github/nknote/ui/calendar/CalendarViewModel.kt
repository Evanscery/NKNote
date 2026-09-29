package io.github.nknote.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

/**
 * Calendar state, replacing the previous shared-HomeViewModel arrangement, which had two
 * defects: the day markers were loaded once (stale after any note write) and every day tap
 * launched a NEW never-cancelled collector into viewModelScope (a leak). Here the markers are
 * a live [NoteRepository.observeAllDates] flow and the selected-day list is a `flatMapLatest`
 * pipeline — selecting a new date cancels the previous date's collector.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repo: NoteRepository) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    /** Live — creating/deleting a note updates the dots without leaving the screen. */
    val noteDates: StateFlow<Set<String>> =
        repo.observeAllDates().map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val notesForSelectedDate: StateFlow<List<Note>> =
        _selectedDate.flatMapLatest { d ->
            if (d == null) flowOf(emptyList()) else repo.observeNotesOnDate(d.toString())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun nextMonth() = _currentMonth.update { it.plusMonths(1) }
    fun previousMonth() = _currentMonth.update { it.minusMonths(1) }

    /** Selecting the already-selected date clears the selection (toggle). */
    fun selectDate(date: LocalDate) = _selectedDate.update { if (it == date) null else date }

    /** Jump back to the current month and select today. */
    fun goToToday() {
        _currentMonth.value = YearMonth.now()
        _selectedDate.value = LocalDate.now()
    }
}
