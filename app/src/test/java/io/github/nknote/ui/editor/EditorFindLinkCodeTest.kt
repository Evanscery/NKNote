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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the todo-11 editor toolbar B features:
 *
 * - **Link insertion** ([EditorViewModel.setLinkOnSelection]) sets `RichSpan.url` on the span
 *   covering the current selection. The renderer underlines the span + emits a `UrlAnnotation`
 *   (todo 4); in the editor the underline is the visible cue.
 * - **Code block** ([EditorViewModel.toggleCode]) toggles `ParagraphStyle.CODE` on the focused
 *   paragraph; the renderer applies a monospace `SpanStyle` + code-block background (todo 4).
 * - **Find-in-page** ([EditorViewModel.searchInDocument]) returns one [MatchLocation] per
 *   occurrence via plain `String.indexOf` (no regex engine — plan constraint). An empty query
 *   returns zero matches (no crash).
 *
 * Robolectric is a test-only dependency (lightweight runtime constraint honored — not shipped
 * in the APK). The [NoopImageStore] stand-in keeps these tests off disk.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorFindLinkCodeTest {

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
    fun setLinkOnSelection_setsUrlOnTheSelectedSpan() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // Type "hello", then select the whole word.
        vm.onTextChange(0, TextFieldValue("hello", TextRange(5, 5)))
        vm.fields[0] = TextFieldValue("hello", TextRange(0, 5))
        vm.setLinkOnSelection("https://x")

        val linked = vm.paragraphs[0].spans.firstOrNull { it.url != null }
        assertNotNull("a span must carry the url after setLinkOnSelection", linked)
        assertEquals("https://x", linked!!.url)
        assertEquals("hello", linked.text)
    }

    @Test
    fun setLinkOnSelection_emptySelection_isNoOp() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("hello", TextRange(5, 5)))
        // Empty selection (collapsed cursor) → linking zero chars is a no-op.
        vm.setLinkOnSelection("https://x")
        assertFalse(
            "no span should carry a url on an empty selection",
            vm.paragraphs[0].spans.any { it.url != null }
        )
    }

    @Test
    fun toggleCode_setsAndClearsCodeParagraphStyle() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("val x = 1"))

        assertEquals(ParagraphStyle.BODY, vm.paragraphs[0].style)

        vm.toggleCode()
        assertEquals(ParagraphStyle.CODE, vm.paragraphs[0].style)
        // Active-state flag mirrors the paragraph style for the toolbar.
        assertTrue(vm.uiState.value.codeAtCursor)

        vm.toggleCode()
        assertEquals(ParagraphStyle.BODY, vm.paragraphs[0].style)
        assertFalse(vm.uiState.value.codeAtCursor)
    }

    @Test
    fun searchInDocument_twoParagraphsBothContaining_returnsTwoMatches() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        vm.onTextChange(0, TextFieldValue("my diary entry"))
        vm.splitParagraph(0, "my diary entry", "") // para[0]="my diary entry", para[1]=""
        vm.onTextChange(1, TextFieldValue("another diary"))

        val matches = vm.searchInDocument("diary")
        assertEquals(2, matches.size)
        assertEquals(0, matches[0].paragraphIndex)
        assertEquals(3, matches[0].start)
        assertEquals(8, matches[0].end)
        assertEquals(1, matches[1].paragraphIndex)
        assertEquals(8, matches[1].start)
        assertEquals(13, matches[1].end)
    }

    @Test
    fun searchInDocument_emptyQuery_returnsEmptyAndDoesNotCrash() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("hello world"))
        assertEquals(emptyList<MatchLocation>(), vm.searchInDocument(""))
    }

    @Test
    fun updateFind_jumpsToFirstMatchAndNextAdvances() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("my diary entry"))
        vm.splitParagraph(0, "my diary entry", "")
        vm.onTextChange(1, TextFieldValue("another diary"))

        vm.updateFind("diary")
        val st = vm.uiState.value
        assertEquals(2, st.findMatches.size)
        assertEquals(0, st.findIndex)
        // navigateToMatch selects the first match in paragraph 0 ("my diary entry" → "diary" at [3,8)).
        assertEquals(0, vm.focusedIndex)
        assertEquals(3, vm.fields[0].selection.start)
        assertEquals(8, vm.fields[0].selection.end)

        vm.findNext()
        val st2 = vm.uiState.value
        assertEquals(1, st2.findIndex)
        // The second match is in paragraph 1; focus + selection moved there.
        assertEquals(1, vm.focusedIndex)
        assertEquals(8, vm.fields[1].selection.start)
        assertEquals(13, vm.fields[1].selection.end)

        vm.findNext()
        // Wraps around back to the first match.
        assertEquals(0, vm.uiState.value.findIndex)
    }

    @Test
    fun linkAtCursor_reflectsSpanUnderCursor() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.onTextChange(0, TextFieldValue("hello", TextRange(5, 5)))
        vm.fields[0] = TextFieldValue("hello", TextRange(0, 5))
        vm.setLinkOnSelection("https://x")

        // Cursor sits inside the linked span → active state true.
        assertTrue(vm.uiState.value.linkAtCursor)
    }

    /** No-op ImageStore stand-in — the link/code/find tests never touch disk images. */
    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
    }
}
