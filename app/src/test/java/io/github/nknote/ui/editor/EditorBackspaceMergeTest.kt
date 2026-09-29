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
import io.github.nknote.ui.editor.richtext.GUARD_CHAR
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The zero-width-sentinel backspace-merge path: soft keyboards delete via
 * `deleteSurroundingText` (no KEYCODE_DEL), so a backspace at raw position 0 is detected by the
 * sentinel guard disappearing from the field text. These tests simulate exactly what the IME
 * hands `onValueChange` in that case: the field text WITHOUT the leading sentinel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorBackspaceMergeTest {

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

    private fun assertGuardInvariant(vm: EditorViewModel) {
        vm.fields.forEachIndexed { i, f ->
            assertTrue("field[$i] must carry the sentinel guard", f.text.firstOrNull() == GUARD_CHAR)
            assertTrue("field[$i] selection must sit behind the guard", f.selection.start >= 1)
        }
        assertTrue(vm.fieldParagraphParity())
    }

    /** Build a two-paragraph doc "hello" / "world" with the caret at the start of "world". */
    private fun twoParagraphDoc(): EditorViewModel {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("hello", TextRange(5, 5)))
        vm.splitParagraph(0, "hello", "")
        vm.onTextChange(1, TextFieldValue("world", TextRange(5, 5)))
        // Move the caret to raw position 0 of paragraph 1 (guarded coordinates).
        vm.onTextChange(1, fieldOf("world", 0))
        return vm
    }

    @Test
    fun sentinelDeletion_atParagraphStart_mergesWithPrevious() {
        val vm = twoParagraphDoc()
        assertEquals(2, vm.paragraphs.size)

        // IME backspace at raw 0: the sentinel is deleted → text arrives WITHOUT the guard.
        vm.onTextChange(1, TextFieldValue("world", TextRange(0, 0)))

        assertEquals(1, vm.paragraphs.size)
        assertEquals("helloworld", vm.paragraphs[0].text)
        // Caret sits at the join point.
        assertEquals(5, vm.fields[0].rawSelection.start)
        assertGuardInvariant(vm)
    }

    @Test
    fun merge_isUndoable() {
        val vm = twoParagraphDoc()
        vm.onTextChange(1, TextFieldValue("world", TextRange(0, 0)))
        assertEquals(1, vm.paragraphs.size)

        vm.undo()
        assertEquals(2, vm.paragraphs.size)
        assertEquals("hello", vm.paragraphs[0].text)
        assertEquals("world", vm.paragraphs[1].text)
        assertGuardInvariant(vm)

        vm.redo()
        assertEquals(1, vm.paragraphs.size)
        assertEquals("helloworld", vm.paragraphs[0].text)
        assertGuardInvariant(vm)
    }

    @Test
    fun sentinelDeletion_atFirstParagraph_restoresGuardWithoutMerging() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("hello", TextRange(5, 5)))
        vm.onTextChange(0, fieldOf("hello", 0))

        // Backspace at the very start of the document: nothing to merge into.
        vm.onTextChange(0, TextFieldValue("hello", TextRange(0, 0)))

        assertEquals(1, vm.paragraphs.size)
        assertEquals("hello", vm.paragraphs[0].text)
        assertGuardInvariant(vm)
    }

    @Test
    fun selectAllDelete_replacesContent_doesNotMerge() {
        val vm = twoParagraphDoc()

        // Select-all + delete on paragraph 1: the whole guarded text (incl. sentinel) is
        // removed. The paragraph empties but must NOT merge (the caret was not at raw 0 in a
        // collapsed state — the edit replaced a selection).
        vm.onTextChange(1, fieldOf("world", 0, 5))       // select all of "world"
        vm.onTextChange(1, TextFieldValue("", TextRange(0, 0)))

        assertEquals("select-all delete must keep both paragraphs", 2, vm.paragraphs.size)
        assertEquals("", vm.paragraphs[1].text)
        assertGuardInvariant(vm)
    }

    @Test
    fun mergeAcrossImageParagraph_removesTheImage() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("before", TextRange(6, 6)))
        vm.insertImageAfter(0, Uri.parse("content://test/img"))
        // Layout now: [0]="before", [1]=image, [2]="" (auto-inserted, focused).
        assertEquals(3, vm.paragraphs.size)
        vm.onTextChange(2, TextFieldValue("after", TextRange(5, 5)))
        vm.onTextChange(2, fieldOf("after", 0))

        // Backspace at the start of "after": the previous paragraph is an image → it is removed.
        vm.onTextChange(2, TextFieldValue("after", TextRange(0, 0)))

        assertEquals(2, vm.paragraphs.size)
        assertTrue(vm.paragraphs.none { it.image != null })
        assertGuardInvariant(vm)
    }

    @Test
    fun typingAfterGuardRestore_behavesNormally() {
        val vm = twoParagraphDoc()
        vm.onTextChange(1, TextFieldValue("world", TextRange(0, 0)))  // merge
        // Continue typing at the join.
        vm.onTextChange(0, fieldOf("helloXworld", 6))
        assertEquals("helloXworld", vm.paragraphs[0].text)
        assertGuardInvariant(vm)
    }

    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}
