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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Markdown shortcuts: a single keystroke completing a line-start prefix on a BODY paragraph
 * converts the paragraph style and strips the prefix — two commands, so one undo restores the
 * literal prefix and a second undo restores the state before typing it. Paste never triggers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorMarkdownShortcutTest {

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

    /** Simulate typing [text] one character at a time (each keystroke a single-char insert). */
    private fun typeChars(v: EditorViewModel, index: Int, text: String) {
        for (i in text.indices) {
            val t = text.substring(0, i + 1)
            v.onTextChange(index, TextFieldValue(t, TextRange(t.length, t.length)))
        }
    }

    @Test
    fun detector_mapsAllTriggers() {
        assertEquals(ParagraphStyle.BULLET to 2, detectMarkdownShortcut("- x"))
        assertEquals(ParagraphStyle.BULLET to 2, detectMarkdownShortcut("* "))
        assertEquals(ParagraphStyle.QUOTE to 2, detectMarkdownShortcut("> quote"))
        assertEquals(ParagraphStyle.TITLE to 2, detectMarkdownShortcut("# big"))
        assertEquals(ParagraphStyle.HEADING to 3, detectMarkdownShortcut("## mid"))
        assertEquals(ParagraphStyle.SUBHEADING to 4, detectMarkdownShortcut("### small"))
        assertEquals(ParagraphStyle.CODE to 3, detectMarkdownShortcut("```"))
        assertEquals(ParagraphStyle.CODE to 2, detectMarkdownShortcut("` code"))
        assertEquals(ParagraphStyle.CHECKBOX to 3, detectMarkdownShortcut("[] todo"))
        assertEquals(ParagraphStyle.NUMBERED to 3, detectMarkdownShortcut("1. item"))
        assertEquals(ParagraphStyle.NUMBERED to 4, detectMarkdownShortcut("12. item"))
        assertNull(detectMarkdownShortcut("plain text"))
        assertNull(detectMarkdownShortcut("-not a list"))
        assertNull(detectMarkdownShortcut("1.no space"))
    }

    @Test
    fun typingDashSpace_convertsToBullet_andStripsPrefix() {
        val v = vm()
        typeChars(v, 0, "- ")

        assertEquals(ParagraphStyle.BULLET, v.paragraphs[0].style)
        assertEquals("", v.paragraphs[0].text)
        assertEquals(0, v.fields[0].rawSelection.start)
    }

    @Test
    fun numberedTrigger_convertsWithTextRetained() {
        val v = vm()
        // Existing text, then the user types "1. " at the start? Simpler real-world case:
        // type the trigger, then text flows into the numbered item.
        typeChars(v, 0, "1. ")
        assertEquals(ParagraphStyle.NUMBERED, v.paragraphs[0].style)
        assertEquals("", v.paragraphs[0].text)

        typeChars(v, 0, "item")
        assertEquals("item", v.paragraphs[0].text)
        assertEquals(ParagraphStyle.NUMBERED, v.paragraphs[0].style)
    }

    @Test
    fun undo_restoresLiteralPrefix_thenPreTypingState() {
        val v = vm()
        typeChars(v, 0, "- ")
        assertEquals(ParagraphStyle.BULLET, v.paragraphs[0].style)

        v.undo()  // undo the conversion → literal "- " as BODY
        assertEquals(ParagraphStyle.BODY, v.paragraphs[0].style)
        assertEquals("- ", v.paragraphs[0].text)

        v.undo()  // undo the typing burst (coalesced "- "... note ' ' broke coalescing: "-" then " ")
        v.undo()
        assertEquals("", v.paragraphs[0].text)
    }

    @Test
    fun trigger_doesNotFire_onNonBodyParagraph() {
        val v = vm()
        v.setParagraphStyle(0, ParagraphStyle.QUOTE)
        typeChars(v, 0, "- ")
        assertEquals("a quote paragraph must not convert", ParagraphStyle.QUOTE, v.paragraphs[0].style)
        assertEquals("- ", v.paragraphs[0].text)
    }

    @Test
    fun trigger_doesNotFire_onPaste() {
        val v = vm()
        // Multi-char insert ending in a trigger shape — pasted, not typed.
        v.onTextChange(0, TextFieldValue("- pasted", TextRange(8, 8)))
        assertEquals(ParagraphStyle.BODY, v.paragraphs[0].style)
        assertEquals("- pasted", v.paragraphs[0].text)
    }

    @Test
    fun trigger_doesNotFire_midParagraph() {
        val v = vm()
        typeChars(v, 0, "abc")
        // Continue typing "- " after existing text — the caret is not at a trigger prefix end.
        v.onTextChange(0, TextFieldValue("abc-", TextRange(4, 4)))
        v.onTextChange(0, TextFieldValue("abc- ", TextRange(5, 5)))
        assertEquals(ParagraphStyle.BODY, v.paragraphs[0].style)
        assertEquals("abc- ", v.paragraphs[0].text)
    }

    @Test
    fun checkboxTrigger_createsUncheckedItem() {
        val v = vm()
        typeChars(v, 0, "[] ")
        assertEquals(ParagraphStyle.CHECKBOX, v.paragraphs[0].style)
        assertEquals(false, v.paragraphs[0].checked)
    }

    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}
