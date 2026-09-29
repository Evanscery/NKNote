package io.github.nknote.data.repository

import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.NoteTag
import io.github.nknote.data.entity.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeAllNotes(): Flow<List<Note>>
    fun observeDeletedNotes(): Flow<List<Note>>
    fun observeNote(id: Int): Flow<Note?>
    suspend fun getNote(id: Int): Note?
    fun searchNotes(query: String): Flow<List<Note>>
    fun observeNotesOnDate(date: String): Flow<List<Note>>
    fun observeNotesOnMonthDay(monthDay: String): Flow<List<Note>>
    suspend fun allDates(): List<String>
    fun observeAllDates(): Flow<List<String>>

    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun moveToTrash(id: Int)
    suspend fun restoreNote(id: Int)
    suspend fun permanentlyDelete(id: Int)
    suspend fun emptyTrash()

    fun observeTags(): Flow<List<Tag>>
    suspend fun getTag(id: String): Tag?
    suspend fun upsertTag(tag: Tag)
    suspend fun deleteTag(id: String)

    fun observeTagsForNote(noteId: Int): Flow<List<Tag>>
    fun observeNotesForTag(tagId: String): Flow<List<Note>>
    fun observeAllNoteTags(): Flow<List<NoteTag>>
    suspend fun getAllNoteTags(): List<NoteTag>
    suspend fun setNoteTags(noteId: Int, tagIds: List<String>)
    suspend fun addTagToNote(noteId: Int, tagId: String)
    suspend fun deleteOrphanTags()
}