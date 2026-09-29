package io.github.nknote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.NoteTag
import io.github.nknote.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteTagDao {
    @Query("SELECT t.* FROM tags t INNER JOIN note_tags nt ON t.id = nt.tagId WHERE nt.noteId = :noteId ORDER BY t.name ASC")
    abstract fun observeTagsForNote(noteId: Int): Flow<List<Tag>>

    @Query("SELECT n.* FROM notes n INNER JOIN note_tags nt ON n.id = nt.noteId WHERE nt.tagId = :tagId AND n.isDeleted = 0 ORDER BY n.date DESC")
    abstract fun observeNotesForTag(tagId: String): Flow<List<Note>>

    @Query("SELECT * FROM note_tags")
    abstract fun observeAll(): Flow<List<NoteTag>>

    @Query("SELECT * FROM note_tags")
    abstract suspend fun getAll(): List<NoteTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(noteTag: NoteTag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(noteTags: List<NoteTag>)

    @Delete
    abstract suspend fun delete(noteTag: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    abstract suspend fun clearForNote(noteId: Int)

    /**
     * Atomically replace the set of tags on a note: clear existing joins then insert the new
     * ones, all inside one Room transaction. Callers (e.g. [io.github.nknote.data.repository.NoteRepositoryImpl.setNoteTags])
     * use this instead of clear+insert separately so a failure between the two cannot leave a
     * note with zero tags or duplicate partial state.
     */
    @Transaction
    open suspend fun setNoteTags(noteId: Int, tagIds: List<String>) {
        clearForNote(noteId)
        if (tagIds.isNotEmpty()) insertAll(tagIds.map { NoteTag(noteId, it) })
    }
}
