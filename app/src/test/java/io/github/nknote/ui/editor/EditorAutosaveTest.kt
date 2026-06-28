package io.github.nknote.ui.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the todo-12 autosave: a `LifecycleEventEffect(ON_STOP)` in [EditorPage] calls
 * [EditorViewModel.save]; moving a [LifecycleRegistry] to STOPPED (simulating the activity /
 * NavHost-entry stop) writes a DB row. The load-bearing assertion is the race guard: a concurrent
 * manual save + ON_STOP autosave on a brand-new note MUST NOT create two rows — the second save
 * observes [EditorViewModel]'s `savedNoteId` (set by the first) and updates instead of inserting.
 *
 * The undo/redo stacks are NOT touched by save (session-scoped, not persisted) — verified too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorAutosaveTest {

    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl
    private val imageStore = NoopImageStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NkNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepositoryImpl(db.noteDao(), db.tagDao(), db.noteTagDao(), imageStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun onStop_save_writesRowToDb() = runTest {
        val owner = TestLifecycleOwner()
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.updateTitle("Autosave title")
        vm.onTextChange(0, TextFieldValue("auto body", TextRange(9, 9)))

        // Wire ON_STOP → save, exactly as LifecycleEventEffect does in EditorPage.
        var job: Job? = null
        owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) job = vm.save()
        })

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        job?.join()

        val notes = repo.observeAllNotes().first()
        assertEquals(1, notes.size)
        assertEquals("Autosave title", notes[0].title)
    }

    @Test
    fun concurrentManualAndOnStopSave_doesNotDuplicateRows() = runTest {
        val owner = TestLifecycleOwner()
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.updateTitle("Race guard")
        vm.onTextChange(0, TextFieldValue("body", TextRange(4, 4)))

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Fire a manual save and an ON_STOP save "simultaneously" — both land in viewModelScope
        // and are serialized by the saveMutex. The first to acquire the mutex inserts; the second
        // observes savedNoteId != null and updates the SAME row. Net result: exactly one row.
        val manual = vm.save { }
        owner.lifecycle.addObserver(LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) vm.save()
        })
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        manual.join()

        val notes = repo.observeAllNotes().first()
        assertEquals("manual + ON_STOP save must produce exactly one row, not two", 1, notes.size)
        assertEquals("Race guard", notes[0].title)
    }

    @Test
    fun secondSave_updatesExistingRow_doesNotInsertAgain() = runTest {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.updateTitle("First")
        vm.onTextChange(0, TextFieldValue("v1", TextRange(2, 2)))

        vm.save { }.join()
        val afterFirst = repo.observeAllNotes().first()
        assertEquals(1, afterFirst.size)
        val firstId = afterFirst[0].id
        assertEquals(1, afterFirst[0].version)

        // Edit + save again → the SAME row is updated (version bumps), no new row inserted.
        vm.updateTitle("Second")
        vm.save { }.join()

        val afterSecond = repo.observeAllNotes().first()
        assertEquals(1, afterSecond.size)
        assertEquals(firstId, afterSecond[0].id)
        assertEquals("Second", afterSecond[0].title)
        assertNotEquals(1, afterSecond[0].version)
    }

    @Test
    fun save_doesNotTouchUndoStack() = runTest {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        // Make one edit so there's something to undo.
        vm.onTextChange(0, TextFieldValue("x", TextRange(1, 1)))
        assertEquals(true, vm.uiState.value.canUndo)

        vm.save { }.join()

        // Undo stack is session-scoped: saving does NOT clear it; undo still reverses the edit.
        assertEquals(true, vm.uiState.value.canUndo)
        vm.undo()
        assertEquals("", vm.paragraphs[0].text)
        assertEquals(false, vm.uiState.value.canUndo)
    }

    /** A minimal [LifecycleOwner] backed by a real [LifecycleRegistry]. */
    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    /** No-op ImageStore stand-in — the autosave tests never touch disk images. */
    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
    }
}
