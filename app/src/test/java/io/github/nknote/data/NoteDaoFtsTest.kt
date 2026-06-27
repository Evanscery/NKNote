package io.github.nknote.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.entity.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the DB hardening (todo 1): FTS4 search via `note_fts` contentEntity, the empty-query
 * guard, and the `monthDay` column replacing `substr(date, 6)`.
 *
 * Uses an in-memory Room DB under Robolectric (test-only deps — not shipped in the APK).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteDaoFtsTest {

    private lateinit var db: NkNoteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NkNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun note(
        title: String,
        excerpt: String,
        searchText: String,
        date: String,
        monthDay: String
    ): Note {
        val now = System.currentTimeMillis()
        return Note(
            title = title,
            excerpt = excerpt,
            content = "[]",
            searchText = searchText,
            date = date,
            monthDay = monthDay,
            createdAt = now,
            updatedAt = now
        )
    }

    @Test
    fun search_returnsOnlyMatchingNote() = runBlocking {
        val diary = note(
            title = "Diary entry",
            excerpt = "my diary log",
            searchText = "today's diary log about the weather",
            date = "2024-06-26",
            monthDay = "06-26"
        )
        val meeting = note(
            title = "Meeting notes",
            excerpt = "standup summary",
            searchText = "team standup and project planning",
            date = "2024-07-01",
            monthDay = "07-01"
        )
        db.noteDao().insert(diary)
        db.noteDao().insert(meeting)

        val results = db.noteDao().search("diary").first()

        assertEquals(1, results.size)
        assertEquals("Diary entry", results.first().title)
    }

    @Test
    fun search_emptyQuery_returnsEmptyWithoutThrowing() = runBlocking {
        db.noteDao().insert(
            note(
                title = "Diary entry",
                excerpt = "my diary log",
                searchText = "today's diary log",
                date = "2024-06-26",
                monthDay = "06-26"
            )
        )

        val results = db.noteDao().search("").first()

        assertTrue("empty query must return an empty list, not throw", results.isEmpty())
    }

    @Test
    fun search_blankQuery_returnsEmptyWithoutThrowing() = runBlocking {
        db.noteDao().insert(
            note(
                title = "Diary entry",
                excerpt = "my diary log",
                searchText = "today's diary log",
                date = "2024-06-26",
                monthDay = "06-26"
            )
        )

        val results = db.noteDao().search("   ").first()

        assertTrue("blank query must return an empty list, not throw", results.isEmpty())
    }

    @Test
    fun observeByMonthDay_returnsMatchingRows() = runBlocking {
        db.noteDao().insert(
            note(
                title = "Diary entry",
                excerpt = "my diary log",
                searchText = "today's diary log",
                date = "2024-06-26",
                monthDay = "06-26"
            )
        )
        db.noteDao().insert(
            note(
                title = "Other day",
                excerpt = "other",
                searchText = "other content",
                date = "2023-06-26",
                monthDay = "06-26"
            )
        )
        db.noteDao().insert(
            note(
                title = "July note",
                excerpt = "july",
                searchText = "july content",
                date = "2024-07-01",
                monthDay = "07-01"
            )
        )

        val results = db.noteDao().observeByMonthDay("06-26").first()

        assertEquals(2, results.size)
        assertTrue("every returned row's date ends with -06-26", results.all { it.date.endsWith("-06-26") })
    }

    @Test
    fun permanentlyDelete_usesGetDeletedIds_viaRepository() = runBlocking {
        // Sanity: the new DAO query exists and returns ids of soft-deleted notes.
        db.noteDao().insert(
            note(
                title = "Alive",
                excerpt = "",
                searchText = "",
                date = "2024-06-26",
                monthDay = "06-26"
            ).copy(isDeleted = true)
        )
        val ids = db.noteDao().getDeletedIds()
        assertEquals(1, ids.size)
    }
}
