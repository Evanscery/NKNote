package io.github.nknote.data

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tag identity + lifecycle: normalized-name ids, transactional [NoteRepositoryImpl.setNoteTags],
 * automatic orphan cleanup, and CASCADE behavior on permanent delete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TagRepositoryTest {

    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl

    private object NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = null
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }

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

    private suspend fun insertNote(title: String): Int {
        val now = System.currentTimeMillis()
        return repo.insertNote(
            Note(title = title, excerpt = "", content = "[]", searchText = "", date = "2026-07-27", monthDay = "07-27", createdAt = now, updatedAt = now)
        ).toInt()
    }

    private fun tagOf(name: String) =
        Tag(id = Tag.idFor(name), name = name.trim(), color = Tag.colorFor(name), createdAt = 0L)

    @Test
    fun idFor_dedupesCaseAndWhitespaceVariants() {
        assertEquals(Tag.idFor("Work"), Tag.idFor(" work "))
        assertEquals(Tag.idFor("Daily  Log"), Tag.idFor("daily log"))
        assertNotEquals(Tag.idFor("work"), Tag.idFor("life"))
    }

    @Test
    fun colorFor_isDeterministic_andFromPalette() {
        assertEquals(Tag.colorFor("work"), Tag.colorFor(" Work "))
        assertTrue(io.github.nknote.model.NkPalette.tagPalette.contains(Tag.colorFor("anything")))
    }

    @Test
    fun setNoteTags_roundTrips() = runBlocking {
        val noteId = insertNote("a")
        repo.upsertTag(tagOf("work"))
        repo.upsertTag(tagOf("life"))
        repo.setNoteTags(noteId, listOf(Tag.idFor("work"), Tag.idFor("life")))

        val tags = repo.observeTagsForNote(noteId).first()
        assertEquals(listOf("life", "work"), tags.map { it.name }.sorted())
    }

    @Test
    fun unassigningLastNote_deletesOrphanTag() = runBlocking {
        val noteId = insertNote("a")
        repo.upsertTag(tagOf("work"))
        repo.setNoteTags(noteId, listOf(Tag.idFor("work")))
        assertEquals(1, repo.observeTags().first().size)

        repo.setNoteTags(noteId, emptyList())

        assertTrue("orphan tag must be auto-deleted", repo.observeTags().first().isEmpty())
    }

    @Test
    fun permanentDelete_cascadesJoins_andCleansOrphans() = runBlocking {
        val noteId = insertNote("a")
        repo.upsertTag(tagOf("work"))
        repo.setNoteTags(noteId, listOf(Tag.idFor("work")))

        repo.permanentlyDelete(noteId)

        assertTrue(repo.getAllNoteTags().isEmpty())
        assertTrue(repo.observeTags().first().isEmpty())
    }

    @Test
    fun observeNotesForTag_returnsOnlyTaggedNotes() = runBlocking {
        val tagged = insertNote("tagged")
        insertNote("untagged")
        repo.upsertTag(tagOf("work"))
        repo.setNoteTags(tagged, listOf(Tag.idFor("work")))

        val notes = repo.observeNotesForTag(Tag.idFor("work")).first()
        assertEquals(listOf("tagged"), notes.map { it.title })
    }

    @Test
    fun sharedTag_survivesWhileAnyNoteKeepsIt() = runBlocking {
        val a = insertNote("a")
        val b = insertNote("b")
        repo.upsertTag(tagOf("work"))
        repo.setNoteTags(a, listOf(Tag.idFor("work")))
        repo.setNoteTags(b, listOf(Tag.idFor("work")))

        repo.setNoteTags(a, emptyList())

        assertEquals(1, repo.observeTags().first().size)
    }
}
