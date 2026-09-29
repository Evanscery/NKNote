package io.github.nknote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.db.FtsQuery
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home list state: DB-side FTS search (replacing the old per-keystroke in-memory JSON scan)
 * composed with an optional tag filter.
 *
 * - The query is debounced 300 ms — but a BLANK query bypasses the debounce so clearing the
 *   search restores the full list instantly.
 * - Search goes through [FtsQuery.sanitize] → [NoteRepository.searchNotes] (FTS4 MATCH).
 * - The tag filter intersects with the search result via [NoteRepository.observeNotesForTag].
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(private val repo: NoteRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _selectedTagId = MutableStateFlow<String?>(null)
    val selectedTagId: StateFlow<String?> = _selectedTagId.asStateFlow()

    /** All tags, for the filter chip row (empty → the row is hidden). */
    val tags: StateFlow<List<Tag>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** noteId → its tags, for the small chips on note cards. */
    val noteTags: StateFlow<Map<Int, List<Tag>>> =
        combine(repo.observeTags(), repo.observeAllNoteTags()) { allTags, joins ->
            val byId = allTags.associateBy { it.id }
            joins.groupBy({ it.noteId }, { byId[it.tagId] })
                .mapValues { (_, v) -> v.filterNotNull() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val notes: StateFlow<List<Note>> =
        combine(
            query.debounce { if (it.isBlank()) 0L else 300L },
            _selectedTagId
        ) { q, tagId -> q to tagId }
            .flatMapLatest { (q, tagId) ->
                val base =
                    if (q.isBlank()) repo.observeAllNotes()
                    else repo.searchNotes(FtsQuery.sanitize(q))
                if (tagId == null) base
                else combine(base, repo.observeNotesForTag(tagId)) { ns, forTag ->
                    val ids = forTag.mapTo(HashSet()) { it.id }
                    ns.filter { it.id in ids }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { query.value = q }

    /** Select a tag filter; selecting the active tag again clears it. */
    fun selectTag(tagId: String?) {
        _selectedTagId.value = if (_selectedTagId.value == tagId) null else tagId
    }

    fun moveToTrash(id: Int) = viewModelScope.launch { repo.moveToTrash(id) }

    /** Undo for the trash snackbar. */
    fun restore(id: Int) = viewModelScope.launch { repo.restoreNote(id) }
}
