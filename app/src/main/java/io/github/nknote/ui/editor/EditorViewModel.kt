package io.github.nknote.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepository
import io.github.nknote.model.InlineImage
import io.github.nknote.model.NkPalette
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichDocument
import io.github.nknote.ui.editor.richtext.EditorDocument
import io.github.nknote.ui.editor.richtext.EditorParagraph
import io.github.nknote.ui.editor.richtext.afterTextChange
import io.github.nknote.ui.editor.richtext.merge
import io.github.nknote.ui.editor.richtext.setColor
import io.github.nknote.ui.editor.richtext.setFontSizeScale
import io.github.nknote.ui.editor.richtext.splitAt
import io.github.nknote.ui.editor.richtext.toEditor
import io.github.nknote.ui.editor.richtext.toModel
import io.github.nknote.ui.editor.richtext.toggleBold
import io.github.nknote.ui.editor.richtext.toggleItalic
import io.github.nknote.ui.editor.richtext.toggleStrikethrough
import io.github.nknote.ui.editor.richtext.toggleUnderline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Observable editor state exposed to the Composable via [StateFlow] + `collectAsStateWithLifecycle`.
 *
 * The real-time editing buffer ([EditorViewModel.fields], [EditorViewModel.focusedIndex],
 * [EditorViewModel.pendingFocusIndex]) is intentionally NOT part of this state — it stays in
 * `mutableStateOf` / `mutableStateListOf` for keystroke-latency (StateFlow conflation under rapid
 * typing risks losing intermediate `TextFieldValue` edits, in particular the IME composition +
 * selection). This split is the load-bearing MVVM decision for the editor.
 *
 * `canUndo` / `canRedo` / `styleAtCursor` are stubs here; the undo/redo engine lands in todo 7.
 */
data class EditorUiState(
    val title: String = "",
    val excerpt: String = "",
    val date: String = LocalDate.now().toString(),
    val weatherKey: String = "",
    val moodKey: String = "",
    val coverImagePath: String? = null,
    val tagNames: List<String> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val styleAtCursor: SpanStyle? = null,
    val wordCount: Int = 0
)

/**
 * Per-paragraph selection snapshot persisted to [SavedStateHandle]. `TextFieldValue` itself is not
 * SavedStateHandle-safe across process death (it carries Compose composition state), so the draft
 * stores the (text, selectionStart, selectionEnd) tuple and [EditorViewModel] reconstructs
 * `TextFieldValue(text, TextRange(start, end))` on restore.
 */
@Serializable
internal data class DraftSelection(val text: String, val start: Int, val end: Int)

class EditorViewModel(
    private val noteId: Int?,
    private val imageStore: ImageStore,
    private val repo: NoteRepository,
    private val handle: SavedStateHandle
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Editing buffer — STAYS as mutableState* for keystroke latency (load-bearing) ──
    private var document by mutableStateOf(EditorDocument())
    val paragraphs: List<EditorParagraph> get() = document.paragraphs
    val fields = mutableStateListOf<TextFieldValue>()
    var focusedIndex by mutableStateOf(0)
        private set
    /** When set, the EditorPage requests focus on the paragraph at this index. */
    var pendingFocusIndex by mutableStateOf(-1)
        private set

    // ── Observable / document / meta state — StateFlow ──
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    var loaded by mutableStateOf(noteId == null || noteId <= 0)
        private set

    init {
        fields.add(TextFieldValue(""))
        val restored = restoreFromHandle()
        if (restored) {
            loaded = true
            if (fields.isEmpty()) fields.add(TextFieldValue(""))
        } else if (noteId != null && noteId > 0) {
            loadNote()
        } else {
            loaded = true
        }
    }

    // ── SavedStateHandle draft persistence ─────────────────────────────────────────

    /**
     * Restore the editing buffer + meta from a [SavedStateHandle] draft (process-death recovery).
     * Returns true if a draft (title or document) was present and restored.
     *
     * `TextFieldValue`s are reconstructed from the persisted (text, selectionStart, selectionEnd)
     * tuples — never stored directly (not parcelable across process death).
     */
    private fun restoreFromHandle(): Boolean {
        val draftDoc = handle.get<String>(KEY_DOC).orEmpty()
        val hasDraft = handle.get<String>(KEY_TITLE) != null || draftDoc.isNotEmpty()
        if (!hasDraft) return false

        if (draftDoc.isNotEmpty()) {
            val model = runCatching { json.decodeFromString<RichDocument>(draftDoc).toEditor() }.getOrNull()
            if (model != null) {
                document = if (model.size == 0) EditorDocument() else model
                fields.clear()
                val sels = runCatching {
                    json.decodeFromString<List<DraftSelection>>(handle.get<String>(KEY_SELS).orEmpty())
                }.getOrDefault(emptyList())
                document.paragraphs.forEachIndexed { i, p ->
                    val s = sels.getOrNull(i)
                    fields.add(
                        if (s != null) TextFieldValue(s.text, TextRange(s.start.coerceAtLeast(0), s.end.coerceAtLeast(0)))
                        else TextFieldValue(p.text)
                    )
                }
                if (fields.isEmpty()) fields.add(TextFieldValue(""))
            }
        }

        val title = handle.get<String>(KEY_TITLE).orEmpty()
        val excerpt = handle.get<String>(KEY_EXCERPT).orEmpty()
        val date = handle.get<String>(KEY_DATE).orEmpty().ifBlank { LocalDate.now().toString() }
        val weather = handle.get<String>(KEY_WEATHER).orEmpty()
        val mood = handle.get<String>(KEY_MOOD).orEmpty()
        val cover = handle.get<String>(KEY_COVER)
        val tags = runCatching {
            json.decodeFromString<List<String>>(handle.get<String>(KEY_TAGS).orEmpty())
        }.getOrDefault(emptyList())

        _uiState.value = EditorUiState(
            title = title, excerpt = excerpt, date = date,
            weatherKey = weather, moodKey = mood, coverImagePath = cover,
            tagNames = tags, wordCount = computeWordCount()
        )
        return true
    }

    /** Serialize the editing buffer (document JSON + per-paragraph selection tuples) to the handle. */
    private fun persistDocumentDraft() {
        handle[KEY_DOC] = json.encodeToString(RichDocument.serializer(), document.toModel())
        val sels = fields.map { DraftSelection(it.text, it.selection.start, it.selection.end) }
        handle[KEY_SELS] = json.encodeToString(ListSerializer(DraftSelection.serializer()), sels)
        _uiState.update { it.copy(wordCount = computeWordCount()) }
    }

    private fun loadNote() = viewModelScope.launch {
        val id = noteId ?: return@launch
        val note = repo.getNote(id) ?: run { loaded = true; return@launch }
        document = runCatching { json.decodeFromString<RichDocument>(note.content).toEditor() }
            .getOrDefault(EditorDocument())
        if (document.size == 0) document = EditorDocument()
        fields.clear()
        document.paragraphs.forEach { fields.add(TextFieldValue(it.text)) }
        if (fields.isEmpty()) fields.add(TextFieldValue(""))
        _uiState.value = EditorUiState(
            title = note.title, excerpt = note.excerpt, date = note.date,
            weatherKey = note.weather, moodKey = note.mood, coverImagePath = note.coverImagePath,
            wordCount = computeWordCount()
        )
        loaded = true
        persistDocumentDraft()
        launch {
            repo.observeTagsForNote(id).collect { ts ->
                _uiState.update { it.copy(tagNames = ts.map { t -> t.name }) }
handle[KEY_TAGS] = json.encodeToString(ListSerializer(String.serializer()), _uiState.value.tagNames)
            }
        }
    }

    // ── Meta mutators (observable state → StateFlow + handle) ──────────────────────
    fun updateTitle(v: String) { _uiState.update { it.copy(title = v) }; handle[KEY_TITLE] = v }
    fun updateExcerpt(v: String) { _uiState.update { it.copy(excerpt = v) }; handle[KEY_EXCERPT] = v }
    fun updateDate(v: String) { _uiState.update { it.copy(date = v) }; handle[KEY_DATE] = v }
    fun updateWeather(key: String) { _uiState.update { it.copy(weatherKey = key) }; handle[KEY_WEATHER] = key }
    fun updateMood(key: String) { _uiState.update { it.copy(moodKey = key) }; handle[KEY_MOOD] = key }

    fun setCoverImage(uri: android.net.Uri) {
        val targetNoteId = noteId ?: -1
        val path = imageStore.saveForNote(if (targetNoteId <= 0) 0 else targetNoteId, uri) ?: return
        _uiState.value.coverImagePath?.let { imageStore.delete(it) }
        _uiState.update { it.copy(coverImagePath = path) }
        handle[KEY_COVER] = path
    }
    fun removeCoverImage() {
        _uiState.value.coverImagePath?.let { imageStore.delete(it) }
        _uiState.update { it.copy(coverImagePath = null) }
        handle[KEY_COVER] = null
    }

    fun addTag(name: String) {
        val n = name.trim()
        if (n.isBlank()) return
        _uiState.update { st ->
            if (n in st.tagNames) st else st.copy(tagNames = st.tagNames + n)
        }
        handle[KEY_TAGS] = json.encodeToString(ListSerializer(String.serializer()), _uiState.value.tagNames)
    }
    fun removeTag(name: String) {
        _uiState.update { it.copy(tagNames = it.tagNames - name) }
        handle[KEY_TAGS] = json.encodeToString(ListSerializer(String.serializer()), _uiState.value.tagNames)
    }

    // ── Editing-buffer mutators (fields/focusedIndex/pendingFocusIndex stay mutableState) ──
    fun onFocus(index: Int) { focusedIndex = index }

    fun onTextChange(index: Int, newValue: TextFieldValue) {
        if (index !in document.paragraphs.indices) ensureCapacity(index + 1)
        val para = document.paragraphs[index]
        val updated = para.afterTextChange(newValue.text)
        updateParagraph(index, updated)
        fields[index] = newValue
        persistDocumentDraft()
    }

    /**
     * Split paragraph [index] at the newline. [beforeText] is the text before \n,
     * [afterText] is the text after. Requests focus on the new paragraph.
     */
    fun splitParagraph(index: Int, beforeText: String, afterText: String) {
        val para = document.paragraphs.getOrNull(index) ?: return
        val splitPoint = beforeText.length.coerceIn(0, para.text.length)
        val (before, after) = para.splitAt(splitPoint)
        updateParagraph(index, before)
        insertParagraphAfter(index, after)
        fields[index] = TextFieldValue(before.text, TextRange(before.text.length))
        fields.add(index + 1, TextFieldValue(after.text, TextRange(after.text.length.coerceAtLeast(0))))
        focusedIndex = index + 1
        pendingFocusIndex = index + 1
        persistDocumentDraft()
    }

    /**
     * Merge paragraph [index] into the previous paragraph (backspace at start).
     * Requests focus on the merged paragraph with cursor at the join point.
     */
    fun mergeWithPrevious(index: Int) {
        if (index <= 0) return
        val prev = document.paragraphs[index - 1]
        val cur = document.paragraphs[index]
        if (cur.image != null) {
            // image paragraph: just remove it, keep focus on previous
            removeImageParagraph(index)
            focusedIndex = index - 1
            pendingFocusIndex = index - 1
            persistDocumentDraft()
            return
        }
        if (prev.image != null) {
            // previous is image: merge skips it, focus on the text paragraph before it
            // simplest: just remove the image paragraph, stay on current
            removeImageParagraph(index - 1)
            focusedIndex = index - 1
            pendingFocusIndex = index - 1
            persistDocumentDraft()
            return
        }
        val joinCursor = prev.text.length
        val merged = prev.merge(cur)
        updateParagraph(index - 1, merged)
        removeParagraphInternal(index)
        fields[index - 1] = TextFieldValue(merged.text, TextRange(joinCursor))
        focusedIndex = index - 1
        pendingFocusIndex = index - 1
        persistDocumentDraft()
    }

    fun consumePendingFocus() { pendingFocusIndex = -1 }

    /** Focus the last text paragraph (or first paragraph if all are images). Used when tapping blank space. */
    fun focusLastTextParagraph() {
        val lastTextIndex = document.paragraphs.indices.reversed().firstOrNull {
            document.paragraphs[it].image == null
        } ?: 0
        // Place cursor at end of the text
        val text = fields.getOrNull(lastTextIndex)?.text ?: ""
        fields[lastTextIndex] = TextFieldValue(text, TextRange(text.length))
        focusedIndex = lastTextIndex
        pendingFocusIndex = lastTextIndex
        persistDocumentDraft()
    }

    fun setParagraphStyle(index: Int, style: ParagraphStyle) {
        if (index in document.paragraphs.indices) {
            updateParagraph(index, document.paragraphs[index].copy(style = style))
            persistDocumentDraft()
        }
    }

    private fun currentSelection(): Pair<Int, TextRange>? {
        val i = focusedIndex
        if (i !in document.paragraphs.indices) return null
        val sel = fields.getOrNull(i)?.selection ?: return null
        return i to sel
    }

    fun toggleBold() = applyToSelection { p, s, e -> p.toggleBold(s, e) }
    fun toggleItalic() = applyToSelection { p, s, e -> p.toggleItalic(s, e) }
    fun toggleUnderline() = applyToSelection { p, s, e -> p.toggleUnderline(s, e) }
    fun toggleStrikethrough() = applyToSelection { p, s, e -> p.toggleStrikethrough(s, e) }
    fun setColor(color: String) = applyToSelection { p, s, e -> p.setColor(s, e, color) }
    fun setFontSizeScale(scale: Float) = applyToSelection { p, s, e -> p.setFontSizeScale(s, e, scale) }

    private inline fun applyToSelection(transform: (EditorParagraph, Int, Int) -> EditorParagraph) {
        val (i, sel) = currentSelection() ?: return
        val para = document.paragraphs[i]
        val s = minOf(sel.start, sel.end).coerceAtLeast(0)
        val e = maxOf(sel.start, sel.end).coerceAtMost(para.text.length)
        if (s == e) return
        val updated = transform(para, s, e)
        updateParagraph(i, updated)
        persistDocumentDraft()
    }

    fun insertImageAfter(index: Int, sourceUri: android.net.Uri) {
        val targetNoteId = noteId ?: -1
        val path = imageStore.saveForNote(if (targetNoteId <= 0) 0 else targetNoteId, sourceUri) ?: return
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, opts)
        val img = InlineImage(path, opts.outWidth.coerceAtLeast(1), opts.outHeight.coerceAtLeast(1))
        val insertAt = index.coerceAtMost(document.size - 1)
        // Insert image paragraph
        insertParagraphAfter(insertAt, EditorParagraph(image = img))
        fields.add((insertAt + 1).coerceAtMost(fields.size), TextFieldValue(""))
        // Auto-insert an empty text paragraph after the image so the user can continue typing
        insertParagraphAfter(insertAt + 1, EditorParagraph())
        fields.add((insertAt + 2).coerceAtMost(fields.size), TextFieldValue(""))
        // Focus the text paragraph after the image
        focusedIndex = insertAt + 2
        pendingFocusIndex = insertAt + 2
        persistDocumentDraft()
    }

    fun removeImageParagraph(index: Int) {
        document.paragraphs.getOrNull(index)?.image?.let { imageStore.delete(it.path) }
        removeParagraphInternal(index)
        persistDocumentDraft()
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val model = document.toModel()
        val content = json.encodeToString(RichDocument.serializer(), model)
        val existing = noteId?.let { repo.getNote(it) }
        val st = _uiState.value
        // Prefer user-set cover; fall back to first inline image
        val resolvedCover = st.coverImagePath ?: model.paragraphs.firstNotNullOfOrNull { it.image }?.path
        val note = (existing ?: Note(
            title = st.title, excerpt = st.excerpt, content = content,
            date = st.date, weather = st.weatherKey, mood = st.moodKey,
            coverImagePath = resolvedCover,
            createdAt = now, updatedAt = now
        )).copy(
            title = st.title, excerpt = st.excerpt, content = content,
            date = st.date, weather = st.weatherKey, mood = st.moodKey,
            coverImagePath = resolvedCover,
            updatedAt = now, version = (existing?.version ?: 1) + 1
        )
        if (existing == null) {
            val newId = repo.insertNote(note)
            persistTags(newId.toInt())
        } else {
            repo.updateNote(note)
            persistTags(note.id)
        }
        onDone()
    }

    private suspend fun persistTags(noteId: Int) {
        val ids = _uiState.value.tagNames.map { name ->
            val id = "tag:" + name.hashCode().toUInt().toString()
            repo.upsertTag(Tag(id, name, NkPalette.defaultTagColor, System.currentTimeMillis()))
            id
        }
        repo.setNoteTags(noteId, ids)
    }

    private fun updateParagraph(index: Int, paragraph: EditorParagraph) {
        val list = document.paragraphs.toMutableList()
        list[index] = paragraph
        document = document.copy(paragraphs = list)
    }
    private fun insertParagraphAfter(index: Int, paragraph: EditorParagraph) {
        val list = document.paragraphs.toMutableList()
        list.add((index + 1).coerceAtMost(list.size), paragraph)
        document = document.copy(paragraphs = list)
    }
    private fun removeParagraphInternal(index: Int) {
        if (document.size <= 1) {
            updateParagraph(0, EditorParagraph()); fields[0] = TextFieldValue(""); return
        }
        val list = document.paragraphs.toMutableList()
        list.removeAt(index)
        document = document.copy(paragraphs = list)
        if (fields.size > index) fields.removeAt(index)
    }
    private fun ensureCapacity(size: Int) {
        while (document.paragraphs.size < size) {
            document = document.copy(paragraphs = document.paragraphs + EditorParagraph())
            fields.add(TextFieldValue(""))
        }
    }

    /** Word count from the document plain text. Recomputed after every document mutation. */
    private fun computeWordCount(): Int {
        val text = document.toModel().plainText()
        return if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    }

    private companion object {
        const val KEY_DOC = "draftDoc"
        const val KEY_SELS = "draftSelections"
        const val KEY_TITLE = "draftTitle"
        const val KEY_EXCERPT = "draftExcerpt"
        const val KEY_DATE = "draftDate"
        const val KEY_WEATHER = "draftWeather"
        const val KEY_MOOD = "draftMood"
        const val KEY_COVER = "draftCover"
        const val KEY_TAGS = "draftTags"
    }
}
