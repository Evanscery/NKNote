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
import io.github.nknote.model.ParagraphAlignment
import io.github.nknote.model.ParagraphStyle
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
 * Verifies the todo-10 FormatBar wiring: paragraph-style / alignment / indent mutations on the
 * focused paragraph surface through [EditorUiState] (paragraph-style / paragraph-alignment /
 * indent-level at the cursor), so the FormatBar's active-state highlighting can read them.
 *
 * The FormatBar itself is a thin Compose layer over these ViewModel calls; this Robolectric test
 * asserts the VM-side contract that the bar reads to set `toggled` — covering the heading
 * hierarchy picker (TITLE / HEADING / SUBHEADING / BODY), the numbered-list toggle, the alignment
 * group, and the indent/outdent clamp. The bold active-state path (styleAtCursor) is also covered
 * so the span-level highlight has a regression test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorToolbarTest {

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
    fun setParagraphStyle_heading_updatesParagraphStyleAtCursor() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Fresh editor: focused paragraph is BODY (default), highlighted state is BODY.
        assertEquals(ParagraphStyle.BODY, vm.uiState.value.paragraphStyleAtCursor)

        vm.setParagraphStyle(0, ParagraphStyle.HEADING)
        assertEquals(ParagraphStyle.HEADING, vm.uiState.value.paragraphStyleAtCursor)
        assertEquals(ParagraphStyle.HEADING, vm.paragraphs[0].style)
    }

    @Test
    fun setParagraphStyle_numbered_updatesParagraphStyleAtCursor() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.setParagraphStyle(0, ParagraphStyle.NUMBERED)

        assertEquals(ParagraphStyle.NUMBERED, vm.uiState.value.paragraphStyleAtCursor)
        assertEquals(ParagraphStyle.NUMBERED, vm.paragraphs[0].style)
    }

    @Test
    fun setParagraphStyle_fullHeadingHierarchy_picksEachLevel() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.setParagraphStyle(0, ParagraphStyle.TITLE)
        assertEquals(ParagraphStyle.TITLE, vm.uiState.value.paragraphStyleAtCursor)

        vm.setParagraphStyle(0, ParagraphStyle.SUBHEADING)
        assertEquals(ParagraphStyle.SUBHEADING, vm.uiState.value.paragraphStyleAtCursor)

        vm.setParagraphStyle(0, ParagraphStyle.BODY)
        assertEquals(ParagraphStyle.BODY, vm.uiState.value.paragraphStyleAtCursor)
    }

    @Test
    fun setParagraphStyle_undoRevertsTheHeadingChange() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.setParagraphStyle(0, ParagraphStyle.HEADING)
        assertEquals(ParagraphStyle.HEADING, vm.uiState.value.paragraphStyleAtCursor)

        vm.undo()
        assertEquals(ParagraphStyle.BODY, vm.uiState.value.paragraphStyleAtCursor)
    }

    @Test
    fun setParagraphAlignment_updatesParagraphAlignmentAtCursor() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        assertEquals(ParagraphAlignment.START, vm.uiState.value.paragraphAlignmentAtCursor)

        vm.setParagraphAlignment(0, ParagraphAlignment.CENTER)
        assertEquals(ParagraphAlignment.CENTER, vm.uiState.value.paragraphAlignmentAtCursor)
        assertEquals(ParagraphAlignment.CENTER, vm.paragraphs[0].alignment)

        vm.setParagraphAlignment(0, ParagraphAlignment.END)
        assertEquals(ParagraphAlignment.END, vm.uiState.value.paragraphAlignmentAtCursor)

        vm.setParagraphAlignment(0, ParagraphAlignment.START)
        assertEquals(ParagraphAlignment.START, vm.uiState.value.paragraphAlignmentAtCursor)
    }

    @Test
    fun setIndentLevel_increment_updatesIndentLevelAtCursor() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        assertEquals(0, vm.uiState.value.indentLevelAtCursor)

        vm.setIndentLevel(0, +1)
        assertEquals(1, vm.uiState.value.indentLevelAtCursor)
        assertEquals(1, vm.paragraphs[0].indentLevel)

        vm.setIndentLevel(0, +1)
        assertEquals(2, vm.uiState.value.indentLevelAtCursor)

        vm.setIndentLevel(0, +1)
        assertEquals(3, vm.uiState.value.indentLevelAtCursor)
    }

    @Test
    fun setIndentLevel_clampsAtZeroAndThree() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Outdent at 0 → no-op (clamps to 0, never negative).
        vm.setIndentLevel(0, -1)
        assertEquals(0, vm.uiState.value.indentLevelAtCursor)
        assertEquals(0, vm.paragraphs[0].indentLevel)

        // Outdent again at 0 → still 0 (no crash, no negative).
        vm.setIndentLevel(0, -1)
        assertEquals(0, vm.uiState.value.indentLevelAtCursor)

        // Indent past 3 → clamps to 3.
        vm.setIndentLevel(0, +1)
        vm.setIndentLevel(0, +1)
        vm.setIndentLevel(0, +1)
        vm.setIndentLevel(0, +1) // already at 3 → no-op
        assertEquals(3, vm.uiState.value.indentLevelAtCursor)
        assertEquals(3, vm.paragraphs[0].indentLevel)
    }

    @Test
    fun setIndentLevel_outdentFromOneReturnsToZero() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.setIndentLevel(0, +1)
        assertEquals(1, vm.uiState.value.indentLevelAtCursor)

        vm.setIndentLevel(0, -1)
        assertEquals(0, vm.uiState.value.indentLevelAtCursor)
    }

    @Test
    fun styleAtCursor_inBoldSpan_returnsBoldWeight() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Build a bold "x" via sticky style (empty selection → sticky BOLD armed, then type "x").
        vm.toggleBold()
        vm.onTextChange(0, TextFieldValue("x", TextRange(1, 1)))

        val style = vm.uiState.value.styleAtCursor
        assertNotNull("styleAtCursor must be non-null inside a non-empty paragraph", style)
        assertEquals(FontWeight.Bold, style!!.fontWeight)

        // The FormatBar reads `styleAtCursor?.fontWeight == FontWeight.Bold` → Bold button toggled.
        assertTrue(
            "bold span at cursor must read as toggled by the FormatBar",
            vm.uiState.value.styleAtCursor?.fontWeight == FontWeight.Bold
        )
    }

    @Test
    fun styleAtCursor_unboldedParagraph_isNotBold() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.onTextChange(0, TextFieldValue("hello", TextRange(5, 5)))

        val style = vm.uiState.value.styleAtCursor
        assertNotNull(style)
        assertFalse(
            "a plain paragraph must NOT read as bold (Bold button untoggled)",
            style!!.fontWeight == FontWeight.Bold
        )
    }

    /** No-op ImageStore stand-in — the toolbar tests never touch disk images. */
    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}
