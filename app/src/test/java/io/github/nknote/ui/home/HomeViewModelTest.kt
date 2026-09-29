package io.github.nknote.ui.home

import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.NoteTag
import io.github.nknote.data.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pure-JVM tests of the Home search/filter pipeline over [FakeNoteRepository]. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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

    private fun note(id: Int, title: String, searchText: String, date: String = "2026-07-27") =
        Note(
            id = id, title = title, excerpt = "", content = "[]", searchText = searchText,
            date = date, monthDay = date.substring(5), createdAt = 0, updatedAt = 0
        )

    private fun collectNotes(vm: HomeViewModel, scope: kotlinx.coroutines.CoroutineScope): Pair<Job, () -> List<Note>> {
        var latest: List<Note> = emptyList()
        val job = scope.launch { vm.notes.collect { latest = it } }
        return job to { latest }
    }

    @Test
    fun blankQuery_bypassesDebounce_showsAllInstantly() = runTest {
        repo.notesFlow.value = listOf(note(1, "a", "alpha"), note(2, "b", "beta"))
        val vm = HomeViewModel(repo)
        val (job, latest) = collectNotes(vm, this)

        runCurrent()
        assertEquals(2, latest().size)
        assertTrue("blank query must not hit FTS", repo.searchQueries.isEmpty())
        job.cancel()
    }

    @Test
    fun typedQuery_isDebounced_andSanitized() = runTest {
        repo.notesFlow.value = listOf(note(1, "a", "alpha diary"), note(2, "b", "beta"))
        val vm = HomeViewModel(repo)
        val (job, latest) = collectNotes(vm, this)
        runCurrent()

        // Three rapid keystrokes: only the last survives the 300 ms debounce.
        vm.setQuery("a")
        advanceTimeBy(100)
        vm.setQuery("al")
        advanceTimeBy(100)
        vm.setQuery("alpha")
        advanceTimeBy(301)
        runCurrent()

        assertEquals("only the final query reaches FTS", listOf("\"alpha*\""), repo.searchQueries)
        assertEquals(listOf(1), latest().map { it.id })
        job.cancel()
    }

    @Test
    fun clearingQuery_restoresFullListInstantly() = runTest {
        repo.notesFlow.value = listOf(note(1, "a", "alpha"), note(2, "b", "beta"))
        val vm = HomeViewModel(repo)
        val (job, latest) = collectNotes(vm, this)
        runCurrent()

        vm.setQuery("alpha")
        advanceTimeBy(301); runCurrent()
        assertEquals(1, latest().size)

        vm.setQuery("")
        runCurrent()
        assertEquals("clear must bypass the debounce", 2, latest().size)
        job.cancel()
    }

    @Test
    fun tagFilter_intersectsWithSearch() = runTest {
        repo.notesFlow.value = listOf(
            note(1, "a", "alpha work"),
            note(2, "b", "alpha life"),
            note(3, "c", "beta work")
        )
        repo.tagsFlow.value = listOf(Tag("work", "work", "#5E7A6E", 0))
        repo.noteTagsFlow.value = listOf(NoteTag(1, "work"), NoteTag(3, "work"))
        val vm = HomeViewModel(repo)
        val (job, latest) = collectNotes(vm, this)
        runCurrent()

        vm.selectTag("work")
        runCurrent()
        assertEquals(listOf(1, 3), latest().map { it.id }.sorted())

        vm.setQuery("alpha")
        advanceTimeBy(301); runCurrent()
        assertEquals("tag ∧ search intersection", listOf(1), latest().map { it.id })
        job.cancel()
    }

    @Test
    fun selectingActiveTag_clearsTheFilter() = runTest {
        val vm = HomeViewModel(repo)
        vm.selectTag("work")
        assertEquals("work", vm.selectedTagId.value)
        vm.selectTag("work")
        assertEquals(null, vm.selectedTagId.value)
    }

    @Test
    fun restore_undoesMoveToTrash() = runTest {
        repo.notesFlow.value = listOf(note(1, "a", "alpha"))
        val vm = HomeViewModel(repo)

        vm.moveToTrash(1); runCurrent()
        assertEquals(listOf(1), repo.trashedIds)

        vm.restore(1); runCurrent()
        assertEquals(listOf(1), repo.restoredIds)
        assertTrue(repo.notesFlow.value.none { it.isDeleted })
    }
}
