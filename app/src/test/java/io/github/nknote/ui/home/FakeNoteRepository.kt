package io.github.nknote.ui.home

import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.NoteTag
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * In-memory [NoteRepository] fake for pure-JVM ViewModel tests. Backed by StateFlows so
 * observers are live; records the queries [searchNotes] receives; counts active per-date
 * collectors so `flatMapLatest` cancellation is assertable.
 */
class FakeNoteRepository : NoteRepository {

    val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    val tagsFlow = MutableStateFlow<List<Tag>>(emptyList())
    val noteTagsFlow = MutableStateFlow<List<NoteTag>>(emptyList())
    val datesFlow = MutableStateFlow<List<String>>(emptyList())

    val searchQueries = mutableListOf<String>()
    val restoredIds = mutableListOf<Int>()
    val trashedIds = mutableListOf<Int>()
    var activeDateCollectors = 0
        private set
    var maxConcurrentDateCollectors = 0
        private set

    override fun observeAllNotes(): Flow<List<Note>> = notesFlow
    override fun observeDeletedNotes(): Flow<List<Note>> = notesFlow.map { l -> l.filter { it.isDeleted } }
    override fun observeNote(id: Int): Flow<Note?> = notesFlow.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getNote(id: Int): Note? = notesFlow.value.firstOrNull { it.id == id }

    override fun searchNotes(query: String): Flow<List<Note>> {
        searchQueries.add(query)
        // Naive contains-match over searchText, ignoring FTS syntax — enough for pipeline tests.
        val needle = query.substringAfter('"').substringBefore('*').lowercase()
        return notesFlow.map { l -> l.filter { it.searchText.lowercase().contains(needle) } }
    }

    override fun observeNotesOnDate(date: String): Flow<List<Note>> =
        notesFlow.map { l -> l.filter { it.date == date } }
            .onStart {
                activeDateCollectors++
                maxConcurrentDateCollectors = maxOf(maxConcurrentDateCollectors, activeDateCollectors)
            }
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    try {
                        flow.collect { emit(it) }
                    } finally {
                        activeDateCollectors--
                    }
                }
            }

    override fun observeNotesOnMonthDay(monthDay: String): Flow<List<Note>> =
        notesFlow.map { l -> l.filter { it.monthDay == monthDay } }

    override suspend fun allDates(): List<String> = datesFlow.value
    override fun observeAllDates(): Flow<List<String>> = datesFlow

    override suspend fun insertNote(note: Note): Long {
        val id = (notesFlow.value.maxOfOrNull { it.id } ?: 0) + 1
        notesFlow.value = notesFlow.value + note.copy(id = id)
        return id.toLong()
    }

    override suspend fun updateNote(note: Note) {
        notesFlow.value = notesFlow.value.map { if (it.id == note.id) note else it }
    }

    override suspend fun moveToTrash(id: Int) {
        trashedIds.add(id)
        notesFlow.value = notesFlow.value.map { if (it.id == id) it.copy(isDeleted = true) else it }
    }

    override suspend fun restoreNote(id: Int) {
        restoredIds.add(id)
        notesFlow.value = notesFlow.value.map { if (it.id == id) it.copy(isDeleted = false) else it }
    }

    override suspend fun permanentlyDelete(id: Int) {
        notesFlow.value = notesFlow.value.filterNot { it.id == id }
    }

    override suspend fun emptyTrash() {
        notesFlow.value = notesFlow.value.filterNot { it.isDeleted }
    }

    override fun observeTags(): Flow<List<Tag>> = tagsFlow
    override suspend fun getTag(id: String): Tag? = tagsFlow.value.firstOrNull { it.id == id }
    override suspend fun upsertTag(tag: Tag) {
        tagsFlow.value = tagsFlow.value.filterNot { it.id == tag.id } + tag
    }
    override suspend fun deleteTag(id: String) {
        tagsFlow.value = tagsFlow.value.filterNot { it.id == id }
    }

    override fun observeTagsForNote(noteId: Int): Flow<List<Tag>> =
        noteTagsFlow.map { joins ->
            val ids = joins.filter { it.noteId == noteId }.map { it.tagId }.toSet()
            tagsFlow.value.filter { it.id in ids }
        }

    override fun observeNotesForTag(tagId: String): Flow<List<Note>> =
        noteTagsFlow.map { joins ->
            val ids = joins.filter { it.tagId == tagId }.map { it.noteId }.toSet()
            notesFlow.value.filter { it.id in ids && !it.isDeleted }
        }

    override fun observeAllNoteTags(): Flow<List<NoteTag>> = noteTagsFlow
    override suspend fun getAllNoteTags(): List<NoteTag> = noteTagsFlow.value

    override suspend fun setNoteTags(noteId: Int, tagIds: List<String>) {
        noteTagsFlow.value =
            noteTagsFlow.value.filterNot { it.noteId == noteId } + tagIds.map { NoteTag(noteId, it) }
    }

    override suspend fun addTagToNote(noteId: Int, tagId: String) {
        noteTagsFlow.value = noteTagsFlow.value + NoteTag(noteId, tagId)
    }

    override suspend fun deleteOrphanTags() {
        val used = noteTagsFlow.value.map { it.tagId }.toSet()
        tagsFlow.value = tagsFlow.value.filter { it.id in used }
    }
}
