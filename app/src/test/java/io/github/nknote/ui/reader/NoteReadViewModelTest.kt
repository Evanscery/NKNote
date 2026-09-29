package io.github.nknote.ui.reader

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.entity.Note
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepositoryImpl
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichParagraph
import io.github.nknote.model.RichSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteReadViewModelTest {

    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = null
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NkNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepositoryImpl(db.noteDao(), db.tagDao(), db.noteTagDao(), NoopImageStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun insert(doc: RichDocument, title: String = "t"): Int {
        val now = System.currentTimeMillis()
        return repo.insertNote(
            Note(
                title = title, excerpt = "", content = json.encodeToString(RichDocument.serializer(), doc),
                searchText = "", date = "2026-07-27", monthDay = "07-27",
                createdAt = now, updatedAt = now
            )
        ).toInt()
    }

    @Test
    fun uiState_loadsAndDecodesDocument() = runTest {
        val id = insert(
            RichDocument(listOf(RichParagraph(spans = listOf(RichSpan("hello reader")))))
        )
        val vm = NoteReadViewModel(id, repo)

        val st = vm.uiState.first { it.loaded && it.note != null }
        assertEquals("t", st.note!!.title)
        assertEquals("hello reader", st.document.plainText())
    }

    @Test
    fun toggleChecked_flipsOnlyTargetParagraph_andPersists() = runTest {
        val id = insert(
            RichDocument(
                listOf(
                    RichParagraph(spans = listOf(RichSpan("a")), style = ParagraphStyle.CHECKBOX),
                    RichParagraph(spans = listOf(RichSpan("b")), style = ParagraphStyle.CHECKBOX, checked = true)
                )
            )
        )
        val vm = NoteReadViewModel(id, repo)
        val versionBefore = repo.getNote(id)!!.version

        vm.toggleChecked(0).join()

        val note = repo.getNote(id)!!
        val doc = json.decodeFromString(RichDocument.serializer(), note.content)
        assertTrue("target flipped", doc.paragraphs[0].checked)
        assertTrue("other untouched", doc.paragraphs[1].checked)
        assertEquals("version bumped", versionBefore + 1, note.version)
    }

    @Test
    fun toggleChecked_onNonCheckboxParagraph_isNoOp() = runTest {
        val id = insert(RichDocument(listOf(RichParagraph(spans = listOf(RichSpan("body"))))))
        val vm = NoteReadViewModel(id, repo)
        val before = repo.getNote(id)!!

        vm.toggleChecked(0).join()

        assertEquals(before.content, repo.getNote(id)!!.content)
    }

    @Test
    fun malformedContent_yieldsEmptyDocument_noCrash() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.insertNote(
            Note(title = "broken", excerpt = "", content = "{not json", searchText = "", date = "2026-07-27", monthDay = "07-27", createdAt = now, updatedAt = now)
        ).toInt()
        val vm = NoteReadViewModel(id, repo)

        val st = vm.uiState.first { it.loaded && it.note != null }
        assertTrue(st.document.paragraphs.isEmpty())
        assertFalse(st.note == null)
    }
}
