package io.github.nknote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.nknote.data.entity.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Dao
abstract class NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY date DESC, updatedAt DESC")
    abstract fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    abstract fun observeDeleted(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract fun observeById(id: Int): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun getById(id: Int): Note?

    /**
     * Full-text search via the `note_fts` contentEntity table. Room's `contentEntity` triggers
     * keep `note_fts` in sync with `notes`, so no manual FTS maintenance is needed.
     *
     * Guard: `MATCH ''` throws `SQLiteDoneException`, so empty/blank queries return an empty
     * flow instead of reaching the FTS query. This is the [search] entry point — callers
     * never need to blank-check.
     */
    fun search(query: String): Flow<List<Note>> =
        if (query.isBlank()) flowOf(emptyList()) else searchFts(query)

    @Query(
        """
        SELECT notes.* FROM notes
        JOIN note_fts ON notes.rowid = note_fts.rowid
        WHERE notes.isDeleted = 0 AND note_fts MATCH :query
        ORDER BY notes.date DESC, notes.updatedAt DESC
        """
    )
    protected abstract fun searchFts(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND date = :date")
    abstract fun observeByDate(date: String): Flow<List<Note>>

    /** Uses the denormalized [io.github.nknote.data.entity.Note.monthDay] column (no `substr()`). */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND monthDay = :monthDay ORDER BY date DESC")
    abstract fun observeByMonthDay(monthDay: String): Flow<List<Note>>

    @Query("SELECT DISTINCT date FROM notes WHERE isDeleted = 0")
    abstract suspend fun allDates(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(note: Note): Long

    @Update
    abstract suspend fun update(note: Note)

    @Delete
    abstract suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :now WHERE id = :id")
    abstract suspend fun moveToTrash(id: Int, now: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    abstract suspend fun restore(id: Int)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    abstract suspend fun emptyTrash()

    /** Returns the ids of all soft-deleted notes — used to clean up image files before [emptyTrash]. */
    @Query("SELECT id FROM notes WHERE isDeleted = 1")
    abstract suspend fun getDeletedIds(): List<Int>
}