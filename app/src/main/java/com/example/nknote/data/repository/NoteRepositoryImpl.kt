package com.example.nknote.data.repository

import com.example.nknote.data.dao.*
import com.example.nknote.data.entities.*
import kotlinx.coroutines.flow.Flow

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val noteTagDao: NoteTagDao,
    private val imageDao: ImageDao,
    private val syncRecordDao: SyncRecordDao
) : NoteRepository {

    // Note 相关操作
    override fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()
    override fun getNoteById(id: Int): Flow<Note?> = noteDao.getNoteById(id)
    override fun getUnsyncedNotes(): Flow<List<Note>> = noteDao.getUnsyncedNotes()
    override fun getNotesUpdatedAfter(lastSyncTime: Long): Flow<List<Note>> = noteDao.getNotesUpdatedAfter(lastSyncTime)
    override suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    override suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    override suspend fun deleteNoteById(id: Int) = noteDao.deleteNoteById(id)
    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    // Tag 相关操作
    override fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()
    override fun getTagById(id: String): Flow<Tag?> = tagDao.getTagById(id)
    override fun searchTags(query: String): Flow<List<Tag>> = tagDao.searchTags(query)
    override suspend fun insertTag(tag: Tag) = tagDao.insertTag(tag)
    override suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)
    override suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
    override suspend fun deleteTagById(id: String) = tagDao.deleteTagById(id)

    // Note-Tag 关联操作
    override fun getTagsForNote(noteId: String): Flow<List<Tag>> = noteTagDao.getTagsWithDetailsForNote(noteId)
    override fun getNotesForTag(tagId: String): Flow<List<Note>> = noteTagDao.getNotesForTag(tagId)
    override suspend fun addTagToNote(noteId: String, tagId: String) = noteTagDao.insertNoteTag(NoteTag(noteId, tagId))
    override suspend fun removeTagFromNote(noteId: String, tagId: String) = noteTagDao.deleteNoteTag(NoteTag(noteId, tagId))
    override suspend fun updateNoteTags(noteId: String, tagIds: List<String>) {
        noteTagDao.deleteAllTagsForNote(noteId)
        noteTagDao.insertNoteTags(tagIds.map { NoteTag(noteId, it) })
    }

    // Image 相关操作
    override fun getImagesForNote(noteId: String): Flow<List<Image>> = imageDao.getImagesForNote(noteId)
    override fun getImageById(id: String): Flow<Image?> = imageDao.getImageById(id)
    override fun getUnsyncedImages(): Flow<List<Image>> = imageDao.getUnsyncedImages()
    override fun getImagesUpdatedAfter(lastSyncTime: Long): Flow<List<Image>> = imageDao.getImagesUpdatedAfter(lastSyncTime)
    override suspend fun insertImage(image: Image) = imageDao.insertImage(image)
    override suspend fun updateImage(image: Image) = imageDao.updateImage(image)
    override suspend fun deleteImage(image: Image) = imageDao.deleteImage(image)
    override suspend fun deleteImageById(id: String) = imageDao.deleteImageById(id)
    override suspend fun deleteAllImagesForNote(noteId: String) = imageDao.deleteAllImagesForNote(noteId)

    // 同步相关操作
    override fun getLatestSyncRecord(deviceId: String): Flow<SyncRecord?> = syncRecordDao.getLatestSyncRecord(deviceId)
    override fun getAllSyncRecords(): Flow<List<SyncRecord>> = syncRecordDao.getAllSyncRecords()
    override suspend fun insertSyncRecord(syncRecord: SyncRecord) = syncRecordDao.insertSyncRecord(syncRecord)
    override suspend fun updateSyncRecord(syncRecord: SyncRecord) = syncRecordDao.updateSyncRecord(syncRecord)
    override suspend fun deleteSyncRecord(syncRecord: SyncRecord) = syncRecordDao.deleteSyncRecord(syncRecord)
    override suspend fun deleteSyncRecordsForDevice(deviceId: String) = syncRecordDao.deleteSyncRecordsForDevice(deviceId)
} 