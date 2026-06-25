package io.github.nknote.data.repository

import io.github.nknote.data.db.NoteDao
import io.github.nknote.data.db.NoteTagDao
import io.github.nknote.data.db.TagDao
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.NoteTag
import io.github.nknote.data.entity.Tag
import kotlinx.coroutines.flow.Flow

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val noteTagDao: NoteTagDao
) : NoteRepository {

    override fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAll()
    override fun observeDeletedNotes(): Flow<List<Note>> = noteDao.observeDeleted()
    override fun observeNote(id: Int): Flow<Note?> = noteDao.observeById(id)
    override suspend fun getNote(id: Int): Note? = noteDao.getById(id)
    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.search(query)
    override fun observeNotesOnDate(date: String): Flow<List<Note>> = noteDao.observeByDate(date)
    override fun observeNotesOnMonthDay(monthDay: String): Flow<List<Note>> = noteDao.observeByMonthDay(monthDay)
    override suspend fun allDates(): List<String> = noteDao.allDates()

    override suspend fun insertNote(note: Note): Long = noteDao.insert(note)
    override suspend fun updateNote(note: Note) = noteDao.update(note)
    override suspend fun moveToTrash(id: Int) = noteDao.moveToTrash(id, System.currentTimeMillis())
    override suspend fun restoreNote(id: Int) = noteDao.restore(id)
    override suspend fun permanentlyDelete(id: Int) = noteDao.deleteById(id)
    override suspend fun emptyTrash() = noteDao.emptyTrash()

    override fun observeTags(): Flow<List<Tag>> = tagDao.observeAll()
    override suspend fun getTag(id: String): Tag? = tagDao.getById(id)
    override suspend fun upsertTag(tag: Tag) = tagDao.insert(tag)
    override suspend fun deleteTag(id: String) = tagDao.deleteById(id)

    override fun observeTagsForNote(noteId: Int): Flow<List<Tag>> = noteTagDao.observeTagsForNote(noteId)

    override suspend fun setNoteTags(noteId: Int, tagIds: List<String>) {
        noteTagDao.clearForNote(noteId)
        if (tagIds.isNotEmpty()) noteTagDao.insertAll(tagIds.map { NoteTag(noteId, it) })
    }

    override suspend fun addTagToNote(noteId: Int, tagId: String) = noteTagDao.insert(NoteTag(noteId, tagId))
}