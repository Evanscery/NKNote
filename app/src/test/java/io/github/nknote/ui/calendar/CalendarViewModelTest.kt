package io.github.nknote.ui.calendar

import io.github.nknote.data.entity.Note
import io.github.nknote.ui.home.FakeNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Verifies the collector-leak fix (flatMapLatest) and the live day-marker flow. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeNoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeNoteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun note(id: Int, date: String) =
        Note(
            id = id, title = "n$id", excerpt = "", content = "[]", searchText = "",
            date = date, monthDay = date.substring(5), createdAt = 0, updatedAt = 0
        )

    @Test
    fun selectingDates_neverStacksCollectors() = runTest {
        repo.notesFlow.value = listOf(note(1, "2026-07-01"), note(2, "2026-07-02"), note(3, "2026-07-03"))
        val vm = CalendarViewModel(repo)
        var latest: List<Note> = emptyList()
        val job = launch { vm.notesForSelectedDate.collect { latest = it } }
        runCurrent()

        // Tap through several days — the old implementation leaked one collector per tap.
        vm.selectDate(LocalDate.parse("2026-07-01")); runCurrent()
        vm.selectDate(LocalDate.parse("2026-07-02")); runCurrent()
        vm.selectDate(LocalDate.parse("2026-07-03")); runCurrent()

        assertEquals(listOf(3), latest.map { it.id })
        assertTrue(
            "flatMapLatest must cancel the previous date's collector (max seen: ${repo.maxConcurrentDateCollectors})",
            repo.maxConcurrentDateCollectors <= 1
        )
        job.cancel()
    }

    @Test
    fun reselectingSameDate_togglesSelectionOff() = runTest {
        val vm = CalendarViewModel(repo)
        val d = LocalDate.parse("2026-07-01")
        vm.selectDate(d)
        assertEquals(d, vm.selectedDate.value)
        vm.selectDate(d)
        assertEquals(null, vm.selectedDate.value)
    }

    @Test
    fun noteDates_isLive() = runTest {
        val vm = CalendarViewModel(repo)
        var latest: Set<String> = emptySet()
        val job = launch { vm.noteDates.collect { latest = it } }
        runCurrent()
        assertTrue(latest.isEmpty())

        repo.datesFlow.value = listOf("2026-07-01", "2026-07-02")
        runCurrent()
        assertEquals(setOf("2026-07-01", "2026-07-02"), latest)
        job.cancel()
    }

    @Test
    fun goToToday_resetsMonthAndSelectsToday() = runTest {
        val vm = CalendarViewModel(repo)
        vm.nextMonth(); vm.nextMonth()
        vm.goToToday()
        assertEquals(YearMonth.now(), vm.currentMonth.value)
        assertEquals(LocalDate.now(), vm.selectedDate.value)
    }
}
