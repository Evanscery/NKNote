package com.example.nknote.data.repository

import com.example.nknote.data.entities.*
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    // Note 相关操作
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(id: Int): Flow<Note?>
    fun getUnsyncedNotes(): Flow<List<Note>>
    fun getNotesUpdatedAfter(lastSyncTime: Long): Flow<List<Note>>
    suspend fun insertNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun deleteNoteById(id: Int)
    fun searchNotes(query: String): Flow<List<Note>>

    // Tag 相关操作
    fun getAllTags(): Flow<List<Tag>>
    fun getTagById(id: String): Flow<Tag?>
    fun searchTags(query: String): Flow<List<Tag>>
    suspend fun insertTag(tag: Tag)
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
    suspend fun deleteTagById(id: String)

    // Note-Tag 关联操作
    fun getTagsForNote(noteId: String): Flow<List<Tag>>
    fun getNotesForTag(tagId: String): Flow<List<Note>>
    suspend fun addTagToNote(noteId: String, tagId: String)
    suspend fun removeTagFromNote(noteId: String, tagId: String)
    suspend fun updateNoteTags(noteId: String, tagIds: List<String>)

    // Image 相关操作
    fun getImagesForNote(noteId: String): Flow<List<Image>>
    fun getImageById(id: String): Flow<Image?>
    fun getUnsyncedImages(): Flow<List<Image>>
    fun getImagesUpdatedAfter(lastSyncTime: Long): Flow<List<Image>>
    suspend fun insertImage(image: Image)
    suspend fun updateImage(image: Image)
    suspend fun deleteImage(image: Image)
    suspend fun deleteImageById(id: String)
    suspend fun deleteAllImagesForNote(noteId: String)

    // 同步相关操作
    fun getLatestSyncRecord(deviceId: String): Flow<SyncRecord?>
    fun getAllSyncRecords(): Flow<List<SyncRecord>>
    suspend fun insertSyncRecord(syncRecord: SyncRecord)
    suspend fun updateSyncRecord(syncRecord: SyncRecord)
    suspend fun deleteSyncRecord(syncRecord: SyncRecord)
    suspend fun deleteSyncRecordsForDevice(deviceId: String)
} 