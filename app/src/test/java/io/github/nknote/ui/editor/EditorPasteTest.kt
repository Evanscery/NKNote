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
import io.github.nknote.model.ParagraphStyle
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
 * Multi-paragraph paste: the pasted characters (newlines included) are absorbed by the span
 * algebra FIRST, then the combined paragraph splits on every newline — one [MultiSplitCmd],
 * one undo step. The old implementation discarded pasted content entirely.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorPasteTest {

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

    private fun assertParity(vm: EditorViewModel) {
        assertTrue("fields.size must equal paragraphs.size", vm.fieldParagraphParity())
    }

    @Test
    fun pasteThreeLines_intoEmptyDoc_createsThreeParagraphs() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.onTextChange(0, TextFieldValue("a\nb\nc", TextRange(5, 5)))

        assertEquals(3, vm.paragraphs.size)
        assertEquals(listOf("a", "b", "c"), vm.paragraphs.map { it.text })
        assertParity(vm)
        // Caret landed at the end of the last line.
        assertEquals(2, vm.focusedIndex)
    }

    @Test
    fun pasteMidText_preservesSurroundingSpans() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Build "bold" as a bold run.
        vm.toggleBold()
        vm.onTextChange(0, TextFieldValue("bold", TextRange(4, 4)))
        assertTrue(vm.paragraphs[0].spans.all { it.bold })

        // Paste "x\ny" in the middle: "bo" + "x\ny" + "ld" → "box" / "yld".
        vm.onTextChange(0, TextFieldValue("box\nyld", TextRange(5, 5)))

        assertEquals(2, vm.paragraphs.size)
        assertEquals("box", vm.paragraphs[0].text)
        assertEquals("yld", vm.paragraphs[1].text)
        assertParity(vm)
        // The original bold chars survive on both sides of the paste.
        assertTrue("'bo' must stay bold", vm.paragraphs[0].spans.first().bold)
        assertTrue("'ld' must stay bold", vm.paragraphs[1].spans.last().bold)
    }

    @Test
    fun paste_isOneUndoStep_andRedoRestores() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("start", TextRange(5, 5)))

        vm.onTextChange(0, TextFieldValue("start a\nb\nc", TextRange(11, 11)))
        assertEquals(3, vm.paragraphs.size)

        vm.undo()  // single undo reverses the whole paste
        assertEquals(1, vm.paragraphs.size)
        assertEquals("start", vm.paragraphs[0].text)
        assertParity(vm)

        vm.redo()
        assertEquals(3, vm.paragraphs.size)
        assertEquals(listOf("start a", "b", "c"), vm.paragraphs.map { it.text })
        assertParity(vm)
    }

    @Test
    fun plainEnter_splitsWithCursorAtLineStart() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("hello", TextRange(5, 5)))

        // Enter at position 2: "he" + "\n" + "llo", caret right after the newline.
        vm.onTextChange(0, TextFieldValue("he\nllo", TextRange(3, 3)))

        assertEquals(listOf("he", "llo"), vm.paragraphs.map { it.text })
        assertEquals(1, vm.focusedIndex)
        assertEquals(0, vm.fields[1].rawSelection.start)
        assertParity(vm)
    }

    @Test
    fun enterOnEmptyBullet_exitsTheList() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.setParagraphStyle(0, ParagraphStyle.BULLET)
        assertEquals(ParagraphStyle.BULLET, vm.paragraphs[0].style)

        // Pure Enter on the empty bullet → paragraph converts to BODY, no split.
        vm.onTextChange(0, TextFieldValue("\n", TextRange(1, 1)))

        assertEquals(1, vm.paragraphs.size)
        assertEquals(ParagraphStyle.BODY, vm.paragraphs[0].style)
        assertParity(vm)

        // Undoable: undo restores the bullet.
        vm.undo()
        assertEquals(ParagraphStyle.BULLET, vm.paragraphs[0].style)
    }

    @Test
    fun enterOnNonEmptyBullet_continuesTheList() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.setParagraphStyle(0, ParagraphStyle.BULLET)
        vm.onTextChange(0, TextFieldValue("item", TextRange(4, 4)))

        vm.onTextChange(0, TextFieldValue("item\n", TextRange(5, 5)))

        assertEquals(2, vm.paragraphs.size)
        assertEquals(ParagraphStyle.BULLET, vm.paragraphs[0].style)
        assertEquals("continuation keeps the list style", ParagraphStyle.BULLET, vm.paragraphs[1].style)
        assertParity(vm)
    }

    @Test
    fun splitUndoRedo_withThreeParagraphs_keepsParity() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("one", TextRange(3, 3)))
        vm.splitParagraph(0, "one", "")
        vm.onTextChange(1, TextFieldValue("two", TextRange(3, 3)))
        vm.splitParagraph(1, "two", "")
        vm.onTextChange(2, TextFieldValue("three", TextRange(5, 5)))
        assertEquals(3, vm.paragraphs.size)
        assertParity(vm)

        // Undo everything, asserting parity after every step (the old double-field-removal
        // desync appeared exactly here with >= 3 paragraphs).
        repeat(10) {
            vm.undo()
            assertParity(vm)
        }
        assertEquals(1, vm.paragraphs.size)
        assertEquals("", vm.paragraphs[0].text)

        repeat(10) {
            vm.redo()
            assertParity(vm)
        }
        assertEquals(3, vm.paragraphs.size)
        assertEquals(listOf("one", "two", "three"), vm.paragraphs.map { it.text })
    }

    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}
