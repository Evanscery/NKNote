package io.github.nknote.data

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.entity.Note
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepositoryImpl
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichParagraph
import io.github.nknote.model.RichSpan
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Blank excerpts are auto-derived from the content's first non-blank line (80 chars). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExcerptDerivationTest {

    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl

    private object NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = null
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NkNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepositoryImpl(db.noteDao(), db.tagDao(), db.noteTagDao(), NoopImageStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun contentOf(vararg lines: String): String =
        json.encodeToString(
            RichDocument.serializer(),
            RichDocument(lines.map { RichParagraph(spans = listOf(RichSpan(text = it))) })
        )

    private fun note(excerpt: String, content: String): Note {
        val now = System.currentTimeMillis()
        return Note(title = "t", excerpt = excerpt, content = content, searchText = "", date = "2026-07-27", monthDay = "07-27", createdAt = now, updatedAt = now)
    }

    @Test
    fun blankExcerpt_isDerivedFromFirstNonBlankLine() = runBlocking {
        val id = repo.insertNote(note(excerpt = "", content = contentOf("", "  ", "first real line", "second"))).toInt()
        assertEquals("first real line", repo.getNote(id)!!.excerpt)
    }

    @Test
    fun derivedExcerpt_isCappedAt80Chars() = runBlocking {
        val long = "x".repeat(200)
        val id = repo.insertNote(note(excerpt = "", content = contentOf(long))).toInt()
        assertEquals(80, repo.getNote(id)!!.excerpt.length)
    }

    @Test
    fun manualExcerpt_isNeverOverwritten() = runBlocking {
        val id = repo.insertNote(note(excerpt = "my summary", content = contentOf("body text"))).toInt()
        assertEquals("my summary", repo.getNote(id)!!.excerpt)
    }

    @Test
    fun emptyContent_leavesExcerptEmpty() = runBlocking {
        val id = repo.insertNote(note(excerpt = "", content = contentOf(""))).toInt()
        assertEquals("", repo.getNote(id)!!.excerpt)
    }
}
