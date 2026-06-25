package io.github.nknote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.NoteTag
import io.github.nknote.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteTagDao {
    @Query("SELECT t.* FROM tags t INNER JOIN note_tags nt ON t.id = nt.tagId WHERE nt.noteId = :noteId ORDER BY t.name ASC")
    fun observeTagsForNote(noteId: Int): Flow<List<Tag>>

    @Query("SELECT n.* FROM notes n INNER JOIN note_tags nt ON n.id = nt.noteId WHERE nt.tagId = :tagId AND n.isDeleted = 0 ORDER BY n.date DESC")
    fun observeNotesForTag(tagId: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(noteTag: NoteTag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(noteTags: List<NoteTag>)

    @Delete
    suspend fun delete(noteTag: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearForNote(noteId: Int)
}