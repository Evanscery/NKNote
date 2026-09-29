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

/** CHECKBOX task-list behavior: toggle, Enter continuation, empty-item exit. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorCheckboxTest {

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

    private fun vm() = EditorViewModel(null, imageStore, repo, SavedStateHandle())

    @Test
    fun toggleChecked_flips_andIsUndoable() {
        val v = vm()
        v.setParagraphStyle(0, ParagraphStyle.CHECKBOX)
        v.onTextChange(0, TextFieldValue("buy milk", TextRange(8, 8)))
        assertFalse(v.paragraphs[0].checked)

        v.toggleChecked(0)
        assertTrue(v.paragraphs[0].checked)

        v.undo()
        assertFalse(v.paragraphs[0].checked)
        v.redo()
        assertTrue(v.paragraphs[0].checked)
    }

    @Test
    fun toggleChecked_onNonCheckbox_isNoOp() {
        val v = vm()
        v.onTextChange(0, TextFieldValue("body", TextRange(4, 4)))
        val undoBefore = v.uiState.value.canUndo
        v.toggleChecked(0)
        assertFalse(v.paragraphs[0].checked)
        assertEquals(undoBefore, v.uiState.value.canUndo)
    }

    @Test
    fun enterOnCheckedItem_continuationIsUnchecked() {
        val v = vm()
        v.setParagraphStyle(0, ParagraphStyle.CHECKBOX)
        v.onTextChange(0, TextFieldValue("done", TextRange(4, 4)))
        v.toggleChecked(0)
        assertTrue(v.paragraphs[0].checked)

        v.onTextChange(0, TextFieldValue("done\n", TextRange(5, 5)))

        assertEquals(2, v.paragraphs.size)
        assertEquals(ParagraphStyle.CHECKBOX, v.paragraphs[1].style)
        assertTrue("original item stays checked", v.paragraphs[0].checked)
        assertFalse("continuation starts unchecked", v.paragraphs[1].checked)
    }

    @Test
    fun enterOnEmptyCheckboxItem_exitsToBody() {
        val v = vm()
        v.setParagraphStyle(0, ParagraphStyle.CHECKBOX)
        v.onTextChange(0, TextFieldValue("\n", TextRange(1, 1)))
        assertEquals(1, v.paragraphs.size)
        assertEquals(ParagraphStyle.BODY, v.paragraphs[0].style)
    }

    @Test
    fun switchingStyle_resetsChecked() {
        val v = vm()
        v.setParagraphStyle(0, ParagraphStyle.CHECKBOX)
        v.onTextChange(0, TextFieldValue("x", TextRange(1, 1)))
        v.toggleChecked(0)
        assertTrue(v.paragraphs[0].checked)

        v.setParagraphStyle(0, ParagraphStyle.BODY)
        assertFalse("leaving CHECKBOX clears the checked flag", v.paragraphs[0].checked)
    }

    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}
