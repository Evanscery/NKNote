package io.github.nknote.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.repository.NoteRepository
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichDocument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Read-only note view. Observes the note reactively ([NoteRepository.observeNote]) so edits made
 * in the editor are live when the user navigates back to the reader. The only mutation the
 * reader performs is checking/unchecking a CHECKBOX paragraph — persisted immediately.
 */
class NoteReadViewModel(
    private val noteId: Int,
    private val repo: NoteRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    data class ReadUiState(
        val note: Note? = null,
        val document: RichDocument = RichDocument(),
        val tags: List<String> = emptyList(),
        val loaded: Boolean = false
    )

    val uiState: StateFlow<ReadUiState> =
        combine(repo.observeNote(noteId), repo.observeTagsForNote(noteId)) { note, tags ->
            val doc = note?.let {
                runCatching { json.decodeFromString<RichDocument>(it.content) }.getOrNull()
            } ?: RichDocument()
            ReadUiState(note = note, document = doc, tags = tags.map { t -> t.name }, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadUiState())

    /** Flip a CHECKBOX paragraph's checked state and persist (bumps updatedAt + version). */
    fun toggleChecked(paragraphIndex: Int) = viewModelScope.launch {
        val note = repo.getNote(noteId) ?: return@launch
        val doc = runCatching { json.decodeFromString<RichDocument>(note.content) }.getOrNull()
            ?: return@launch
        val p = doc.paragraphs.getOrNull(paragraphIndex) ?: return@launch
        if (p.style != ParagraphStyle.CHECKBOX) return@launch
        val updated = doc.copy(
            paragraphs = doc.paragraphs.toMutableList().also {
                it[paragraphIndex] = p.copy(checked = !p.checked)
            }
        )
        repo.updateNote(
            note.copy(
                content = json.encodeToString(RichDocument.serializer(), updated),
                updatedAt = System.currentTimeMillis(),
                version = note.version + 1
            )
        )
    }
}
