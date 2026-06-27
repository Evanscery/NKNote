package io.github.nknote.data.repository

import io.github.nknote.data.db.NoteDao
import io.github.nknote.data.db.NoteTagDao
import io.github.nknote.data.db.TagDao
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.image.ImageStore
import io.github.nknote.model.RichDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val noteTagDao: NoteTagDao,
    private val imageStore: ImageStore
) : NoteRepository {

    /**
     * Lenient JSON used to deserialize [Note.content] into a [RichDocument] for [searchText]
     * derivation. Matches the editor/importer [Json] config.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAll()
    override fun observeDeletedNotes(): Flow<List<Note>> = noteDao.observeDeleted()
    override fun observeNote(id: Int): Flow<Note?> = noteDao.observeById(id)
    override suspend fun getNote(id: Int): Note? = noteDao.getById(id)
    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.search(query)
    override fun observeNotesOnDate(date: String): Flow<List<Note>> = noteDao.observeByDate(date)
    override fun observeNotesOnMonthDay(monthDay: String): Flow<List<Note>> = noteDao.observeByMonthDay(monthDay)
    override suspend fun allDates(): List<String> = noteDao.allDates()

    override suspend fun insertNote(note: Note): Long = noteDao.insert(deriveSearchFields(note))

    override suspend fun updateNote(note: Note) = noteDao.update(deriveSearchFields(note))

    override suspend fun moveToTrash(id: Int) = noteDao.moveToTrash(id, System.currentTimeMillis())
    override suspend fun restoreNote(id: Int) = noteDao.restore(id)

    /**
     * Permanently remove a note AND its image files. Soft-delete ([moveToTrash]) deliberately
     * keeps images so a restore is possible; only permanent-delete and [emptyTrash] clean disk.
     */
    override suspend fun permanentlyDelete(id: Int) {
        noteDao.deleteById(id)
        imageStore.deleteAllForNote(id)
    }

    /**
     * Empty the trash then clean up image files for every deleted note. The ids are read FIRST
     * (before the rows are gone) so the per-note image folders can be removed afterwards.
     */
    override suspend fun emptyTrash() {
        val ids = noteDao.getDeletedIds()
        noteDao.emptyTrash()
        ids.forEach { imageStore.deleteAllForNote(it) }
    }

    override fun observeTags(): Flow<List<Tag>> = tagDao.observeAll()
    override suspend fun getTag(id: String): Tag? = tagDao.getById(id)
    override suspend fun upsertTag(tag: Tag) = tagDao.insert(tag)
    override suspend fun deleteTag(id: String) = tagDao.deleteById(id)

    override fun observeTagsForNote(noteId: Int): Flow<List<Tag>> = noteTagDao.observeTagsForNote(noteId)

    /** Delegates to the transactional [NoteTagDao.setNoteTags] (clear+insert in one Room transaction). */
    override suspend fun setNoteTags(noteId: Int, tagIds: List<String>) =
        noteTagDao.setNoteTags(noteId, tagIds)

    override suspend fun addTagToNote(noteId: Int, tagId: String) =
        noteTagDao.insert(io.github.nknote.data.entity.NoteTag(noteId, tagId))

    /**
     * Derives [Note.searchText] (plain-text of [Note.content] via [RichDocument.plainText]) and
     * [Note.monthDay] (`MM-dd` from [Note.date]) so callers (EditorViewModel.save, TextImporter)
     * never have to compute them and the DAO queries (FTS + observeByMonthDay) always have
     * populated columns. Falls back gracefully if [content] is not valid RichDocument JSON.
     */
    private fun deriveSearchFields(note: Note): Note {
        val plain = runCatching {
            json.decodeFromString(RichDocument.serializer(), note.content).plainText()
        }.getOrDefault("")
        val monthDay = if (note.date.length >= 10) note.date.substring(5) else note.date
        return note.copy(searchText = plain, monthDay = monthDay)
    }
}