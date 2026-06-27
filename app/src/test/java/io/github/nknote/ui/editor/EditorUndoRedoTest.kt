package io.github.nknote.ui.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the todo-7 editor engine: undo/redo command stack, sticky style, active-style query
 * (`styleAtCursor`), and word count.
 *
 * The undo/redo engine stores *commands* (per-paragraph diffs), NOT raw document snapshots —
 * each command carries exactly the before/after state of the region it touched. Image removal is
 * intentionally not reversible (the file is deleted). Sticky style fires when a style toggle is
 * invoked with an empty selection; the next typed run picks up the style and the sticky set clears.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorUndoRedoTest {

    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl
    private val imageStore = NoopImageStore()

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

    @Test
    fun undoRedo_roundtrip_textChangeAndBoldToggle() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Initial state: one empty paragraph, nothing to undo/redo.
        assertEquals(1, vm.paragraphs.size)
        assertEquals("", vm.paragraphs[0].text)
        assertFalse(vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)

        // 1. Type "a".
        vm.onTextChange(0, TextFieldValue("a", TextRange(1, 1)))
        assertEquals("a", vm.paragraphs[0].text)
        assertTrue(vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)

        // 2. Select "a" and toggle bold (a non-empty selection → real span toggle, not sticky).
        vm.fields[0] = TextFieldValue("a", TextRange(0, 1))
        vm.toggleBold()
        assertTrue(vm.paragraphs[0].spans.any { it.bold })

        // Undo twice → back to the initial empty document.
        vm.undo() // undo bold toggle
        assertFalse(vm.paragraphs[0].spans.any { it.bold })
        vm.undo() // undo text change
        assertEquals("", vm.paragraphs[0].text)
        assertEquals(1, vm.paragraphs.size)
        assertFalse(vm.uiState.value.canUndo)
        assertTrue(vm.uiState.value.canRedo)

        // Redo twice → back to the edited (bold "a") state.
        vm.redo() // redo text change
        assertEquals("a", vm.paragraphs[0].text)
        vm.redo() // redo bold toggle
        assertTrue(vm.paragraphs[0].spans.any { it.bold })
        assertTrue(vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)
    }

    @Test
    fun stickyStyle_toggleBoldWithEmptySelectionThenType_appliesBoldToInsertedRun() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Empty paragraph, cursor at 0 → empty selection → toggleBold sets the sticky flag.
        assertEquals(0, vm.fields[0].selection.start)
        vm.toggleBold()

        // Type "x" — the inserted run should pick up the sticky bold style.
        // (afterTextChange alone would leave "x" unstyled since the paragraph was empty and there
        // is no predecessor char to inherit from — so a bold "x" proves the sticky path fired.)
        vm.onTextChange(0, TextFieldValue("x", TextRange(1, 1)))

        val span = vm.paragraphs[0].spans.firstOrNull()
        assertNotNull("paragraph must have a span after typing", span)
        assertEquals("x", span!!.text)
        assertTrue("the inserted 'x' must be bold (sticky style applied)", span.bold)
    }

    @Test
    fun stickyStyle_clearsAfterTheFirstRun() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Type "a" (no bold), arm sticky bold, type "b" → "b" bold (sticky). Sticky clears.
        vm.onTextChange(0, TextFieldValue("a", TextRange(1, 1)))
        vm.toggleBold() // empty selection at end of "a" → sticky BOLD armed
        vm.onTextChange(0, TextFieldValue("ab", TextRange(2, 2)))
        assertTrue("b must be bold (sticky applied)", vm.paragraphs[0].spans.any { it.text.contains("b") && it.bold })

        // Split after "b" → new empty paragraph[1], cursor there. Type "c" with no sticky and no
        // bold predecessor → "c" must NOT be bold. This proves sticky was cleared (a still-armed
        // sticky would have made "c" bold).
        vm.splitParagraph(0, "ab", "") // para[0]="ab", para[1]=""
        vm.onTextChange(1, TextFieldValue("c", TextRange(1, 1)))
        val cSpan = vm.paragraphs[1].spans.firstOrNull()
        assertNotNull(cSpan)
        assertFalse("c must not be bold (sticky was cleared after the first run)", cSpan!!.bold)
    }

    @Test
    fun styleAtCursor_returnsBoldWhenCursorIsInBoldSpan() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Build a bold "x" via sticky style.
        vm.toggleBold()
        vm.onTextChange(0, TextFieldValue("x", TextRange(1, 1)))

        // Cursor sits at position 1 (end of "x"); spanAt(1) falls back to char 0 → the bold span.
        val style = vm.uiState.value.styleAtCursor
        assertNotNull("styleAtCursor must be non-null inside a non-empty paragraph", style)
        assertEquals(FontWeight.Bold, style!!.fontWeight)
    }

    @Test
    fun styleAtCursor_nullForEmptyParagraph() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        // Empty paragraph → no span under the cursor.
        assertEquals(null, vm.uiState.value.styleAtCursor)
    }

    @Test
    fun wordCount_threeParagraphDoc_equalsSum() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        assertEquals(0, vm.uiState.value.wordCount)

        vm.onTextChange(0, TextFieldValue("alpha", TextRange(5, 5)))
        vm.splitParagraph(0, "alpha", "") // para[0]="alpha", para[1]=""
        vm.onTextChange(1, TextFieldValue("beta", TextRange(4, 4)))
        vm.splitParagraph(1, "beta", "")  // para[1]="beta", para[2]=""
        vm.onTextChange(2, TextFieldValue("gamma", TextRange(5, 5)))

        assertEquals(3, vm.paragraphs.size)
        assertEquals("alpha", vm.paragraphs[0].text)
        assertEquals("beta", vm.paragraphs[1].text)
        assertEquals("gamma", vm.paragraphs[2].text)
        assertEquals(3, vm.uiState.value.wordCount)
        // charCount = "alpha\nbeta\ngamma".length = 5+1+4+1+5 = 16
        assertEquals(16, vm.uiState.value.charCount)
    }

    @Test
    fun undo_atEmptyStack_isNoOpAndDoesNotCrash() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        assertFalse(vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)

        // Undo / redo on empty stacks: no crash, stacks stay empty, document unchanged.
        vm.undo()
        vm.redo()
        vm.undo()

        assertFalse(vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)
        assertEquals(1, vm.paragraphs.size)
        assertEquals("", vm.paragraphs[0].text)
    }

    @Test
    fun canUndoCanRedo_reflectStackStateAcrossEdits() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.onTextChange(0, TextFieldValue("a", TextRange(1, 1)))
        assertTrue(vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)

        vm.undo()
        assertFalse(vm.uiState.value.canUndo)
        assertTrue(vm.uiState.value.canRedo)

        // A new edit clears the redo stack.
        vm.onTextChange(0, TextFieldValue("b", TextRange(1, 1)))
        assertTrue(vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)
    }

    /** No-op ImageStore stand-in — the undo/redo tests never touch disk images. */
    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
    }
}
