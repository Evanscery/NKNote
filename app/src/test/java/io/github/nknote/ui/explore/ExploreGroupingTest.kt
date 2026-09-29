package io.github.nknote.ui.explore

import io.github.nknote.data.entity.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Pure-JVM tests for the "on this day" year-grouping. */
class ExploreGroupingTest {

    private fun note(id: Int, date: String, updatedAt: Long = 0) =
        Note(
            id = id, title = "n$id", excerpt = "", content = "[]", searchText = "",
            date = date, monthDay = if (date.length >= 10) date.substring(5) else date,
            createdAt = 0, updatedAt = updatedAt
        )

    private val today = LocalDate.parse("2026-07-27")

    @Test
    fun excludesTodaysOwnEntries() {
        val sections = ExploreViewModel.groupByYear(
            listOf(note(1, "2026-07-27"), note(2, "2025-07-27")),
            today
        )
        assertEquals(1, sections.size)
        assertEquals(2025, sections[0].year)
    }

    @Test
    fun groupsByYear_newestYearFirst() {
        val sections = ExploreViewModel.groupByYear(
            listOf(note(1, "2023-07-27"), note(2, "2025-07-27"), note(3, "2024-07-27")),
            today
        )
        assertEquals(listOf(2025, 2024, 2023), sections.map { it.year })
        assertEquals(listOf(1, 2, 3), sections.map { it.yearsAgo })
    }

    @Test
    fun withinAYear_notesSortNewestEditFirst() {
        val sections = ExploreViewModel.groupByYear(
            listOf(note(1, "2025-07-27", updatedAt = 10), note(2, "2025-07-27", updatedAt = 99)),
            today
        )
        assertEquals(listOf(2, 1), sections.single().notes.map { it.id })
    }

    @Test
    fun malformedDates_areDropped() {
        val sections = ExploreViewModel.groupByYear(
            listOf(note(1, "bad"), note(2, "2025-07-27")),
            today
        )
        assertEquals(1, sections.size)
        assertTrue(sections.single().notes.single().id == 2)
    }

    @Test
    fun emptyInput_yieldsNoSections() {
        assertTrue(ExploreViewModel.groupByYear(emptyList(), today).isEmpty())
    }
}
