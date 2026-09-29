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
import io.github.nknote.data.templates.NoteTemplates
import io.github.nknote.model.ParagraphStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the todo-12 templates: picking a built-in [NoteTemplates] entry and applying it via
 * [EditorViewModel.applyTemplate] pre-fills the editor with the template's paragraphs.
 *
 * Acceptance: picking "Gratitude" opens the editor with 3 bullet paragraphs (the heading is a
 * separate leading paragraph, so the editor carries 1 HEADING + 3 BULLET paragraphs total).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorTemplateTest {

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
    fun pickGratitude_appliesThreeBulletParagraphs() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Simulate the user picking "Gratitude" from the TemplatePickerDialog: the dialog hands the
        // NoteTemplate back, the editor calls applyTemplate(template.document).
        val gratitude = NoteTemplates.byKey("gratitude")
        assertEquals(NoteTemplates.Gratitude, gratitude)
        vm.applyTemplate(gratitude!!.document)

        // The Gratitude template is 1 HEADING + 3 BULLET paragraphs.
        assertEquals(4, vm.paragraphs.size)
        assertEquals(ParagraphStyle.HEADING, vm.paragraphs[0].style)
        assertEquals(ParagraphStyle.BULLET, vm.paragraphs[1].style)
        assertEquals(ParagraphStyle.BULLET, vm.paragraphs[2].style)
        assertEquals(ParagraphStyle.BULLET, vm.paragraphs[3].style)

        // The editing buffer is rebuilt to match the template (one field per paragraph).
        assertEquals(4, vm.fields.size)

        // Applying a template clears the undo stack (wholesale document reset).
        assertEquals(false, vm.uiState.value.canUndo)
        assertEquals(false, vm.uiState.value.canRedo)
    }

    @Test
    fun pickDailyLog_appliesHeadingAndBody() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.applyTemplate(NoteTemplates.DailyLog.document)

        assertEquals(2, vm.paragraphs.size)
        assertEquals(ParagraphStyle.HEADING, vm.paragraphs[0].style)
        assertEquals(ParagraphStyle.BODY, vm.paragraphs[1].style)
    }

    @Test
    fun pickFreeWrite_appliesSingleEmptyBody() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.applyTemplate(NoteTemplates.FreeWrite.document)

        assertEquals(1, vm.paragraphs.size)
        assertEquals(ParagraphStyle.BODY, vm.paragraphs[0].style)
        assertEquals("", vm.paragraphs[0].text)
    }

    @Test
    fun applyTemplate_thenEdit_marksEditedForDebouncedAutosave() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        val before = vm.editVersion
        vm.applyTemplate(NoteTemplates.Gratitude.document)
        // applyTemplate routes through afterMutation → markEdited, so editVersion bumps. This arms
        // the EditorPage debounced-autosave LaunchedEffect (3 s after the template apply).
        val after = vm.editVersion
        assertEquals(true, after > before)
    }

    /** No-op ImageStore stand-in — the template tests never touch disk images. */
    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}
