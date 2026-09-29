package io.github.nknote.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class ExploreViewModel(private val repo: NoteRepository) : ViewModel() {

    /** "On this day" grouped by year (newest year first), today's own entries excluded. */
    data class YearSection(val yearsAgo: Int, val year: Int, val notes: List<Note>)

    val totalNotes: StateFlow<Int> =
        repo.observeAllNotes().map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val sections: StateFlow<List<YearSection>> =
        repo.observeNotesOnMonthDay(monthDay())
            .map { groupByYear(it, LocalDate.now()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        /**
         * Pure + unit-testable: excludes today's date, groups by note year, newest year first,
         * notes within a year newest-edit first.
         */
        fun groupByYear(notes: List<Note>, today: LocalDate): List<YearSection> =
            notes.asSequence()
                .filter { it.date != today.toString() && it.date.length >= 4 }
                .groupBy { it.date.take(4).toIntOrNull() ?: 0 }
                .filterKeys { it > 0 }
                .map { (year, ns) ->
                    YearSection(
                        yearsAgo = today.year - year,
                        year = year,
                        notes = ns.sortedByDescending { it.updatedAt }
                    )
                }
                .sortedByDescending { it.year }
    }
}

private fun monthDay(): String =
    LocalDate.now().let { "%02d-%02d".format(it.monthValue, it.dayOfMonth) }
