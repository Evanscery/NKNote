package com.example.nknote.data.dao

import androidx.room.*
import com.example.nknote.data.entities.Note
import com.example.nknote.data.entities.NoteTag
import com.example.nknote.data.entities.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteTagDao {
    @Query("SELECT * FROM note_tags WHERE noteId = :noteId")
    fun getTagsForNote(noteId: String): Flow<List<NoteTag>>

    @Query("SELECT t.* FROM tags t INNER JOIN note_tags nt ON t.id = nt.tagId WHERE nt.noteId = :noteId")
    fun getTagsWithDetailsForNote(noteId: String): Flow<List<Tag>>

    @Query("SELECT n.* FROM notes n INNER JOIN note_tags nt ON n.id = nt.noteId WHERE nt.tagId = :tagId")
    fun getNotesForTag(tagId: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTag(noteTag: NoteTag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTags(noteTags: List<NoteTag>)

    @Delete
    suspend fun deleteNoteTag(noteTag: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteAllTagsForNote(noteId: String)

    @Query("DELETE FROM note_tags WHERE tagId = :tagId")
    suspend fun deleteAllNotesForTag(tagId: String)
} 