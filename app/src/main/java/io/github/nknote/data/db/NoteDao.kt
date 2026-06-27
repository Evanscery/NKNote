package io.github.nknote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.nknote.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY date DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Int): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Int): Note?

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0 AND (
            title LIKE '%' || :query || '%' OR
            excerpt LIKE '%' || :query || '%' OR
            content LIKE '%' || :query || '%'
        )
        ORDER BY date DESC, updatedAt DESC
        """
    )
    fun search(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND date = :date")
    fun observeByDate(date: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND substr(date, 6) = :monthDay ORDER BY date DESC")
    fun observeByMonthDay(monthDay: String): Flow<List<Note>>

    @Query("SELECT DISTINCT date FROM notes WHERE isDeleted = 0")
    suspend fun allDates(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun moveToTrash(id: Int, now: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Int)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()
}