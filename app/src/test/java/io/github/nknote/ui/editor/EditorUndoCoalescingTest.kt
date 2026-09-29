package io.github.nknote.ui.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepositoryImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Undo coalescing: consecutive single-run insertions (or deletions) on the same paragraph
 * within the 800 ms window merge into ONE undo step; whitespace, pauses, cursor moves, and
 * undo/redo break the chain. Time is injected via [EditorViewModel.nowProvider].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorUndoCoalescingTest {

    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl
    private val imageStore = NoopImageStore()
    private var fakeNow = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NkNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepositoryImpl(db.noteDao(), db.tagDao(), db.noteTagDao(), imageStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun vm(): EditorViewModel =
        EditorViewModel(null, imageStore, repo, SavedStateHandle()).also {
            it.nowProvider = { fakeNow }
        }

    private fun type(vm: EditorViewModel, index: Int, text: String, advanceMs: Long = 50) {
        fakeNow += advanceMs
        vm.onTextChange(index, TextFieldValue(text, TextRange(text.length, text.length)))
    }

    @Test
    fun typingBurst_isOneUndoStep() {
        val v = vm()
        type(v, 0, "h"); type(v, 0, "he"); type(v, 0, "hel"); type(v, 0, "hell"); type(v, 0, "hello")

        assertEquals("hello", v.paragraphs[0].text)
        v.undo()
        assertEquals("one undo must remove the whole burst", "", v.paragraphs[0].text)
        assertFalse(v.uiState.value.canUndo)
    }

    @Test
    fun whitespace_breaksIntoWordSteps() {
        val v = vm()
        "hello".indices.forEach { type(v, 0, "hello".substring(0, it + 1)) }
        type(v, 0, "hello ")   // space starts a new command
        type(v, 0, "hello w"); type(v, 0, "hello wo"); type(v, 0, "hello wor")
        type(v, 0, "hello worl"); type(v, 0, "hello world")

        v.undo()
        assertEquals("first undo removes ' world'", "hello", v.paragraphs[0].text)
        v.undo()
        assertEquals("second undo removes 'hello'", "", v.paragraphs[0].text)
        assertFalse(v.uiState.value.canUndo)
    }

    @Test
    fun pauseBeyondWindow_breaksCoalescing() {
        val v = vm()
        type(v, 0, "a"); type(v, 0, "ab")
        type(v, 0, "abc", advanceMs = 2_000)  // > 800 ms pause

        v.undo()
        assertEquals("ab", v.paragraphs[0].text)
        v.undo()
        assertEquals("", v.paragraphs[0].text)
    }

    @Test
    fun selectionOnlyChange_pushesNoCommand_andBreaksCoalescing() {
        val v = vm()
        type(v, 0, "ab")
        val undoDepthBefore = v.uiState.value.canUndo

        // Pure cursor move: same text, different selection.
        v.onTextChange(0, fieldOf("ab", 1))
        assertEquals(undoDepthBefore, v.uiState.value.canUndo)

        // Typing after the move starts a NEW step (no coalescing with the pre-move burst).
        type(v, 0, "aXb")
        v.undo()
        assertEquals("ab", v.paragraphs[0].text)
        assertTrue("the pre-move burst is still a separate step", v.uiState.value.canUndo)
    }

    @Test
    fun deletions_coalesceTogether_butNotWithInsertions() {
        val v = vm()
        type(v, 0, "abcd")
        // Delete chars one by one — one coalesced deletion step.
        type(v, 0, "abc"); type(v, 0, "ab"); type(v, 0, "a")

        v.undo()
        assertEquals("deletion burst undone as one step", "abcd", v.paragraphs[0].text)
        v.undo()
        assertEquals("", v.paragraphs[0].text)
    }

    @Test
    fun undo_breaksCoalescing_redoStackCleared() {
        val v = vm()
        type(v, 0, "ab")
        v.undo()
        assertTrue(v.uiState.value.canRedo)
        type(v, 0, "x")
        assertFalse("a new edit clears the redo stack", v.uiState.value.canRedo)
        v.undo()
        assertEquals("", v.paragraphs[0].text)
    }

    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}
