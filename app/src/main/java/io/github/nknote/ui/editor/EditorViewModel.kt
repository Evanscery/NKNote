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
import io.github.nknote.model.RichSpan
import io.github.nknote.ui.editor.richtext.EditorDocument
import io.github.nknote.ui.editor.richtext.EditorParagraph
import io.github.nknote.ui.editor.richtext.afterTextChange
import io.github.nknote.ui.editor.richtext.merge
import io.github.nknote.ui.editor.richtext.setColor
import io.github.nknote.ui.editor.richtext.setFontSizeScale
import io.github.nknote.ui.editor.richtext.spanAt
import io.github.nknote.ui.editor.richtext.splitAt
import io.github.nknote.ui.editor.richtext.toEditor
import io.github.nknote.ui.editor.richtext.toModel
import io.github.nknote.ui.editor.richtext.toSpanStyle
import io.github.nknote.ui.editor.richtext.toggleBold
import io.github.nknote.ui.editor.richtext.toggleItalic
import io.github.nknote.ui.editor.richtext.toggleStrikethrough
import io.github.nknote.ui.editor.richtext.toggleUnderline
import io.github.nknote.ui.editor.richtext.withSpanToggled
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
 * `canUndo` / `canRedo` reflect the [EditorViewModel] command stacks; `styleAtCursor` is the merged
 * [SpanStyle] of the span under the cursor (null when the cursor is in an empty paragraph);
 * `wordCount` / `charCount` are derived from [RichDocument.plainText].
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
    val wordCount: Int = 0,
    val charCount: Int = 0
)

/**
 * Per-paragraph selection snapshot persisted to [SavedStateHandle]. `TextFieldValue` itself is not
 * SavedStateHandle-safe across process death (it carries Compose composition state), so the draft
 * stores the (text, selectionStart, selectionEnd) tuple and [EditorViewModel] reconstructs
 * `TextFieldValue(text, TextRange(start, end))` on restore.
 */
@Serializable
internal data class DraftSelection(val text: String, val start: Int, val end: Int)

/**
 * Reversible editor mutation. The undo/redo engine stores *commands* (targeted per-paragraph
 * diffs), NOT raw document snapshots — each command carries exactly the before/after state of the
 * region it touched, so the stack stays light even for large documents. See the individual
 * subclasses for the mutations covered (text edit, span toggle / paragraph-style change, paragraph
 * split, paragraph merge, image insert). Image *removal* is intentionally NOT reversible (the
 * image file is deleted on removal — a permanent action — so re-inserting the paragraph would
 * point at a missing file).
 */
private sealed interface EditorCommand {
    fun undo(vm: EditorViewModel)
    fun redo(vm: EditorViewModel)
}

/** Replace a single paragraph (and optionally its field value). Covers text edits and span / paragraph-style changes. */
private class ReplaceCmd(
    val index: Int,
    val beforePara: EditorParagraph,
    val afterPara: EditorParagraph,
    val beforeField: TextFieldValue? = null,
    val afterField: TextFieldValue? = null
) : EditorCommand {
    override fun undo(vm: EditorViewModel) {
        vm.cmdUpdateParagraph(index, beforePara)
        if (beforeField != null) vm.cmdSetField(index, beforeField)
        vm.cmdAfterMutation()
    }
    override fun redo(vm: EditorViewModel) {
        vm.cmdUpdateParagraph(index, afterPara)
        if (afterField != null) vm.cmdSetField(index, afterField)
        vm.cmdAfterMutation()
    }
}

/** Split a paragraph on Enter (one paragraph → two). */
private class SplitCmd(
    val index: Int,
    val originalPara: EditorParagraph,    // paragraph before the split (full text)
    val beforeHalfPara: EditorParagraph, // paragraph[index] after the split (text before cursor)
    val afterPara: EditorParagraph,       // paragraph[index+1] created by the split
    val originalField: TextFieldValue,    // field[index] before the split
    val beforeHalfField: TextFieldValue,  // field[index] after the split
    val afterField: TextFieldValue,       // field[index+1] after the split
    val prevFocus: Int                    // focused index before the split
) : EditorCommand {
    override fun undo(vm: EditorViewModel) {
        vm.cmdUpdateParagraph(index, originalPara)
        vm.cmdRemoveParagraphAt(index + 1)
        vm.cmdSetField(index, originalField)
        vm.cmdRemoveField(index + 1)
        vm.cmdSetFocusedAndPending(prevFocus)
        vm.cmdAfterMutation()
    }
    override fun redo(vm: EditorViewModel) {
        vm.cmdUpdateParagraph(index, beforeHalfPara)
        vm.cmdInsertParagraphAfter(index, afterPara)
        vm.cmdSetField(index, beforeHalfField)
        vm.cmdAddField(index + 1, afterField)
        vm.cmdSetFocusedAndPending(index + 1)
        vm.cmdAfterMutation()
    }
}

/** Merge a paragraph into the previous one (backspace at start). */
private class MergeCmd(
    val index: Int,                          // the removed paragraph's index
    val prevPara: EditorParagraph,           // paragraph[index-1] before the merge
    val curPara: EditorParagraph,            // paragraph[index] before the merge (removed)
    val mergedPara: EditorParagraph,         // paragraph[index-1] after the merge
    val prevField: TextFieldValue,           // field[index-1] before the merge
    val curField: TextFieldValue,            // field[index] before the merge
    val mergedField: TextFieldValue,         // field[index-1] after the merge
    val prevFocus: Int                       // focused index before the merge
) : EditorCommand {
    override fun undo(vm: EditorViewModel) {
        vm.cmdUpdateParagraph(index - 1, prevPara)
        vm.cmdInsertParagraphAfter(index - 1, curPara)
        vm.cmdSetField(index - 1, prevField)
        vm.cmdAddField(index, curField)
        vm.cmdSetFocusedAndPending(prevFocus)
        vm.cmdAfterMutation()
    }
    override fun redo(vm: EditorViewModel) {
        vm.cmdUpdateParagraph(index - 1, mergedPara)
        vm.cmdRemoveParagraphAt(index)
        vm.cmdSetField(index - 1, mergedField)
        vm.cmdRemoveField(index)
        vm.cmdSetFocusedAndPending(index - 1)
        vm.cmdAfterMutation()
    }
}

/** Insert an image paragraph + an empty text paragraph after it (the editor auto-inserts the text paragraph). */
private class ImageInsertCmd(
    val insertAt: Int,                       // image paragraph goes at insertAt+1, text at insertAt+2
    val imagePara: EditorParagraph,
    val textPara: EditorParagraph,
    val prevFocus: Int
) : EditorCommand {
    override fun undo(vm: EditorViewModel) {
        vm.cmdRemoveParagraphAt(insertAt + 2)
        vm.cmdRemoveParagraphAt(insertAt + 1)
        vm.cmdRemoveField(insertAt + 2)
        vm.cmdRemoveField(insertAt + 1)
        vm.cmdSetFocusedAndPending(prevFocus)
        vm.cmdAfterMutation()
    }
    override fun redo(vm: EditorViewModel) {
        vm.cmdInsertParagraphAfter(insertAt, imagePara)
        vm.cmdInsertParagraphAfter(insertAt + 1, textPara)
        vm.cmdAddField((insertAt + 1).coerceAtMost(vm.fields.size), TextFieldValue(""))
        vm.cmdAddField((insertAt + 2).coerceAtMost(vm.fields.size), TextFieldValue(""))
        vm.cmdSetFocusedAndPending(insertAt + 2)
        vm.cmdAfterMutation()
    }
}

/** Sticky-style flags applied to the next typed run when a style toggle is invoked with an empty selection. */
enum class StickyStyle { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, COLOR, SIZE }

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

    // ── Undo / redo command stacks (todo 7) ──
    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()

    // ── Sticky style (todo 7): applied to the next typed run when a toggle is invoked with an empty selection ──
    private var stickyStyles: Set<StickyStyle> = emptySet()
    private var stickyColor: String? = null
    private var stickySizeScale: Float = 1f

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

        val (wc, cc) = documentStats()
        _uiState.value = EditorUiState(
            title = title, excerpt = excerpt, date = date,
            weatherKey = weather, moodKey = mood, coverImagePath = cover,
            tagNames = tags, wordCount = wc, charCount = cc
        )
        return true
    }

    /** Serialize the editing buffer (document JSON + per-paragraph selection tuples) to the handle. */
    private fun persistDocumentDraft() {
        handle[KEY_DOC] = json.encodeToString(RichDocument.serializer(), document.toModel())
        val sels = fields.map { DraftSelection(it.text, it.selection.start, it.selection.end) }
        handle[KEY_SELS] = json.encodeToString(ListSerializer(DraftSelection.serializer()), sels)
        refreshDocumentStats()
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
        val (wc, cc) = documentStats()
        _uiState.value = EditorUiState(
            title = note.title, excerpt = note.excerpt, date = note.date,
            weatherKey = note.weather, moodKey = note.mood, coverImagePath = note.coverImagePath,
            wordCount = wc, charCount = cc
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
    fun onFocus(index: Int) {
        focusedIndex = index
        refreshStyleAtCursor()
    }

    fun onTextChange(index: Int, newValue: TextFieldValue) {
        if (index !in document.paragraphs.indices) ensureCapacity(index + 1)
        val beforePara = document.paragraphs[index]
        val beforeField = fields[index]
        val afterTextChange = beforePara.afterTextChange(newValue.text)

        // Sticky style: apply to the inserted range, then clear the sticky set.
        var finalPara = afterTextChange
        if (stickyStyles.isNotEmpty() && newValue.text.length > beforePara.text.length) {
            val (insStart, insEnd) = computeInsertedRange(beforePara.text, newValue.text)
            if (insEnd > insStart) {
                var styled = afterTextChange
                for (st in stickyStyles) styled = applySticky(st, styled, insStart, insEnd)
                finalPara = styled
            }
            stickyStyles = emptySet()
            stickyColor = null
            stickySizeScale = 1f
        }

        updateParagraph(index, finalPara)
        fields[index] = newValue
        commit(ReplaceCmd(index, beforePara, finalPara, beforeField, newValue))
    }

    /**
     * Split paragraph [index] at the newline. [beforeText] is the text before \n,
     * [afterText] is the text after. Requests focus on the new paragraph.
     */
    fun splitParagraph(index: Int, beforeText: String, afterText: String) {
        val para = document.paragraphs.getOrNull(index) ?: return
        val splitPoint = beforeText.length.coerceIn(0, para.text.length)
        val originalPara = para
        val originalField = fields[index]
        val prevFocus = focusedIndex
        val (before, after) = para.splitAt(splitPoint)
        val beforeField = TextFieldValue(before.text, TextRange(before.text.length))
        val afterField = TextFieldValue(after.text, TextRange(after.text.length.coerceAtLeast(0)))
        updateParagraph(index, before)
        insertParagraphAfter(index, after)
        fields[index] = beforeField
        fields.add(index + 1, afterField)
        focusedIndex = index + 1
        pendingFocusIndex = index + 1
        commit(SplitCmd(index, originalPara, before, after, originalField, beforeField, afterField, prevFocus))
    }

    /**
     * Merge paragraph [index] into the previous paragraph (backspace at start).
     * Requests focus on the merged paragraph with cursor at the join point.
     *
     * Image-paragraph merges are NOT recorded (file deletion is permanent — re-inserting the
     * paragraph would point at a missing image). Only text↔text merges push an undo command.
     */
    fun mergeWithPrevious(index: Int) {
        if (index <= 0) return
        val prev = document.paragraphs[index - 1]
        val cur = document.paragraphs[index]
        val prevFocus = focusedIndex
        if (cur.image != null) {
            // image paragraph: just remove it, keep focus on previous
            removeImageParagraph(index)
            focusedIndex = index - 1
            pendingFocusIndex = index - 1
            afterMutation()
            return
        }
        if (prev.image != null) {
            // previous is image: merge skips it, focus on the text paragraph before it
            removeImageParagraph(index - 1)
            focusedIndex = index - 1
            pendingFocusIndex = index - 1
            afterMutation()
            return
        }
        val prevField = fields[index - 1]
        val curField = fields[index]
        val joinCursor = prev.text.length
        val merged = prev.merge(cur)
        val mergedField = TextFieldValue(merged.text, TextRange(joinCursor))
        updateParagraph(index - 1, merged)
        removeParagraphInternal(index)
        if (fields.size > index - 1) fields[index - 1] = mergedField
        focusedIndex = index - 1
        pendingFocusIndex = index - 1
        commit(MergeCmd(index, prev, cur, merged, prevField, curField, mergedField, prevFocus))
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
        afterMutation()
    }

    fun setParagraphStyle(index: Int, style: ParagraphStyle) {
        if (index !in document.paragraphs.indices) return
        val beforePara = document.paragraphs[index]
        if (beforePara.style == style) return
        val afterPara = beforePara.copy(style = style)
        updateParagraph(index, afterPara)
        commit(ReplaceCmd(index, beforePara, afterPara))
    }

    private fun currentSelection(): Pair<Int, TextRange>? {
        val i = focusedIndex
        if (i !in document.paragraphs.indices) return null
        val sel = fields.getOrNull(i)?.selection ?: return null
        return i to sel
    }

    fun toggleBold() = applyToSelection(StickyStyle.BOLD) { p, s, e -> p.toggleBold(s, e) }
    fun toggleItalic() = applyToSelection(StickyStyle.ITALIC) { p, s, e -> p.toggleItalic(s, e) }
    fun toggleUnderline() = applyToSelection(StickyStyle.UNDERLINE) { p, s, e -> p.toggleUnderline(s, e) }
    fun toggleStrikethrough() = applyToSelection(StickyStyle.STRIKETHROUGH) { p, s, e -> p.toggleStrikethrough(s, e) }
    fun setColor(color: String) = applyToSelection(StickyStyle.COLOR, colorArg = color) { p, s, e -> p.setColor(s, e, color) }
    fun setFontSizeScale(scale: Float) = applyToSelection(StickyStyle.SIZE, scaleArg = scale) { p, s, e -> p.setFontSizeScale(s, e, scale) }

    /**
     * Apply a span [transform] to the current selection. When the selection is EMPTY, instead of a
     * no-op, set the corresponding [stickyStyle] flag — the next typed run picks up the style
     * (applied in [onTextChange]) and the sticky set is cleared. Toggling the same sticky style
     * twice removes it.
     */
    private inline fun applyToSelection(
        stickyStyle: StickyStyle,
        colorArg: String? = null,
        scaleArg: Float = 1f,
        transform: (EditorParagraph, Int, Int) -> EditorParagraph
    ) {
        val (i, sel) = currentSelection() ?: return
        val para = document.paragraphs[i]
        val s = minOf(sel.start, sel.end).coerceAtLeast(0)
        val e = maxOf(sel.start, sel.end).coerceAtMost(para.text.length)
        if (s == e) {
            // Empty selection → sticky style for the next typed run
            stickyStyles = if (stickyStyle in stickyStyles) stickyStyles - stickyStyle else stickyStyles + stickyStyle
            val stickyOn = stickyStyle in stickyStyles
            if (stickyStyle == StickyStyle.COLOR) stickyColor = if (stickyOn) colorArg else null
            if (stickyStyle == StickyStyle.SIZE) stickySizeScale = if (stickyOn) scaleArg else 1f
            refreshStyleAtCursor()
            return
        }
        val beforePara = para
        val updated = transform(para, s, e)
        updateParagraph(i, updated)
        commit(ReplaceCmd(i, beforePara, updated))
    }

    fun insertImageAfter(index: Int, sourceUri: android.net.Uri) {
        val targetNoteId = noteId ?: -1
        val path = imageStore.saveForNote(if (targetNoteId <= 0) 0 else targetNoteId, sourceUri) ?: return
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, opts)
        val img = InlineImage(path, opts.outWidth.coerceAtLeast(1), opts.outHeight.coerceAtLeast(1))
        val insertAt = index.coerceAtMost(document.size - 1)
        val prevFocus = focusedIndex
        val imagePara = EditorParagraph(image = img)
        val textPara = EditorParagraph()
        // Insert image paragraph
        insertParagraphAfter(insertAt, imagePara)
        fields.add((insertAt + 1).coerceAtMost(fields.size), TextFieldValue(""))
        // Auto-insert an empty text paragraph after the image so the user can continue typing
        insertParagraphAfter(insertAt + 1, textPara)
        fields.add((insertAt + 2).coerceAtMost(fields.size), TextFieldValue(""))
        // Focus the text paragraph after the image
        focusedIndex = insertAt + 2
        pendingFocusIndex = insertAt + 2
        commit(ImageInsertCmd(insertAt, imagePara, textPara, prevFocus))
    }

    /** Remove an image paragraph. NOT reversible (the image file is permanently deleted). */
    fun removeImageParagraph(index: Int) {
        document.paragraphs.getOrNull(index)?.image?.let { imageStore.delete(it.path) }
        removeParagraphInternal(index)
        afterMutation()
    }

    // ── Undo / redo engine (todo 7) ────────────────────────────────────────────────

    /** Undo the last mutation. A no-op (no crash) when the undo stack is empty. */
    fun undo() {
        val cmd = undoStack.removeLastOrNull() ?: return
        cmd.undo(this)
        redoStack.addLast(cmd)
        refreshUndoRedoState()
    }

    /** Redo the last undone mutation. A no-op (no crash) when the redo stack is empty. */
    fun redo() {
        val cmd = redoStack.removeLastOrNull() ?: return
        cmd.redo(this)
        undoStack.addLast(cmd)
        refreshUndoRedoState()
    }

    /** Push a command onto the undo stack, clear the redo stack, cap history at [MAX_HISTORY]. */
    private fun pushCommand(cmd: EditorCommand) {
        undoStack.addLast(cmd)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        refreshUndoRedoState()
    }

    /** Apply a command: mutate → push → persist + refresh style. Used by public mutators. */
    private fun commit(cmd: EditorCommand) {
        pushCommand(cmd)
        afterMutation()
    }

    /** Persist the draft + refresh styleAtCursor (used after every mutation, public or via command). */
    private fun afterMutation() {
        persistDocumentDraft()
        refreshStyleAtCursor()
    }

    private fun refreshUndoRedoState() {
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
    }

    private fun refreshStyleAtCursor() {
        _uiState.update { it.copy(styleAtCursor = computeStyleAtCursor()) }
    }

    /** The merged [SpanStyle] of the span under the cursor (null for an empty paragraph). */
    private fun computeStyleAtCursor(): SpanStyle? {
        val i = focusedIndex
        if (i !in document.paragraphs.indices) return null
        val pos = fields.getOrNull(i)?.selection?.start ?: return null
        return document.paragraphs[i].spanAt(pos)?.toSpanStyle()
    }

    /**
     * Apply a sticky style to a range. Sticky style SETS the style on (does NOT toggle) — when the
     * inserted chars already inherited the predecessor's style via [afterTextChange], toggling would
     * wrongly remove it. Setting force-on matches the sticky semantics ("the next typed run WILL
     * have this style").
     */
    private fun applySticky(st: StickyStyle, p: EditorParagraph, s: Int, e: Int): EditorParagraph {
        val transform: (RichSpan) -> RichSpan = when (st) {
            StickyStyle.BOLD -> { it -> it.copy(bold = true) }
            StickyStyle.ITALIC -> { it -> it.copy(italic = true) }
            StickyStyle.UNDERLINE -> { it -> it.copy(underline = true) }
            StickyStyle.STRIKETHROUGH -> { it -> it.copy(strikethrough = true) }
            StickyStyle.COLOR -> { it -> it.copy(color = stickyColor) }
            StickyStyle.SIZE -> { it -> it.copy(fontSizeScale = stickySizeScale) }
        }
        return p.withSpanToggled(s, e, transform)
    }

    /** Compute the inserted char range [start, end) from an old→new text transition. */
    private fun computeInsertedRange(oldText: String, newText: String): Pair<Int, Int> {
        var p = 0
        while (p < oldText.length && p < newText.length && oldText[p] == newText[p]) p++
        var sfx = 0
        while (sfx < (oldText.length - p) && sfx < (newText.length - p) &&
            oldText[oldText.length - 1 - sfx] == newText[newText.length - 1 - sfx]) sfx++
        return p to (newText.length - sfx)
    }

    // ── Internal helpers used by [EditorCommand] undo/redo (internal so the private
    //     top-level command classes in this file can call them without `inner` classes) ──

    internal fun cmdUpdateParagraph(index: Int, p: EditorParagraph) {
        if (index in document.paragraphs.indices) updateParagraph(index, p)
    }
    internal fun cmdInsertParagraphAfter(index: Int, p: EditorParagraph) {
        insertParagraphAfter(index, p)
    }
    internal fun cmdRemoveParagraphAt(index: Int) {
        removeParagraphInternal(index)
    }
    internal fun cmdSetField(index: Int, f: TextFieldValue) {
        if (index in fields.indices) fields[index] = f
    }
    internal fun cmdAddField(index: Int, f: TextFieldValue) {
        fields.add(index.coerceIn(0, fields.size), f)
    }
    internal fun cmdRemoveField(index: Int) {
        if (fields.size > index) fields.removeAt(index)
    }
    internal fun cmdSetFocusedAndPending(focus: Int) {
        focusedIndex = focus
        pendingFocusIndex = focus
    }
    internal fun cmdAfterMutation() {
        afterMutation()
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

    /** Word + char count from the document plain text. Recomputed after every document mutation. */
    private fun documentStats(): Pair<Int, Int> {
        val text = document.toModel().plainText()
        val wc = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        return wc to text.length
    }

    private fun refreshDocumentStats() {
        val (wc, cc) = documentStats()
        _uiState.update { it.copy(wordCount = wc, charCount = cc) }
    }

    private companion object {
        const val MAX_HISTORY = 100
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
