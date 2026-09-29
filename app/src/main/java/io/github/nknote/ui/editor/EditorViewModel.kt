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
import io.github.nknote.model.ParagraphAlignment
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichSpan
import io.github.nknote.ui.editor.richtext.EditorDocument
import io.github.nknote.ui.editor.richtext.EditorParagraph
import io.github.nknote.ui.editor.richtext.GUARD_CHAR
import io.github.nknote.ui.editor.richtext.afterTextChange
import io.github.nknote.ui.editor.richtext.merge
import io.github.nknote.ui.editor.richtext.setColor
import io.github.nknote.ui.editor.richtext.setFontSizeScale
import io.github.nknote.ui.editor.richtext.spanAt
import io.github.nknote.ui.editor.richtext.splitIntoLines
import io.github.nknote.ui.editor.richtext.toEditor
import io.github.nknote.ui.editor.richtext.toModel
import io.github.nknote.ui.editor.richtext.toSpanStyle
import io.github.nknote.ui.editor.richtext.toggleBold
import io.github.nknote.ui.editor.richtext.toggleItalic
import io.github.nknote.ui.editor.richtext.toggleStrikethrough
import io.github.nknote.ui.editor.richtext.toggleUnderline
import io.github.nknote.ui.editor.richtext.withSpanToggled
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val paragraphStyleAtCursor: ParagraphStyle = ParagraphStyle.BODY,
    val paragraphAlignmentAtCursor: ParagraphAlignment = ParagraphAlignment.START,
    val indentLevelAtCursor: Int = 0,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val linkAtCursor: Boolean = false,
    val codeAtCursor: Boolean = false,
    val findMatches: List<MatchLocation> = emptyList(),
    val findIndex: Int = -1
)

/**
 * A single find-in-page match: the paragraph index plus the half-open `[start, end)` range of the
 * matched substring within that paragraph's *raw* text (the marker / indent prefix is NOT counted
 * — the renderer shifts by `markerPrefixLength` when laying the highlight over the transformed
 * string). Computed by [EditorViewModel.searchInDocument] via plain [String.indexOf].
 */
data class MatchLocation(val paragraphIndex: Int, val start: Int, val end: Int)

/**
 * Per-paragraph selection snapshot persisted to [SavedStateHandle]. Stores the RAW text and RAW
 * selection offsets (never the sentinel guard), so drafts written by any app version restore
 * cleanly. `TextFieldValue` itself is not SavedStateHandle-safe across process death.
 */
@Serializable
internal data class DraftSelection(val text: String, val start: Int, val end: Int)

// ── Zero-width sentinel guard ──────────────────────────────────────────────────
//
// Soft keyboards delete via InputConnection.deleteSurroundingText and never dispatch KEYCODE_DEL,
// so a key-event backspace handler cannot detect "backspace at paragraph start" (the merge
// trigger). Instead, every text field's TextFieldValue holds `GUARD + rawText` with the selection
// kept >= 1: a backspace at raw position 0 deletes the guard, which DOES fire onValueChange on
// every IME, and the ViewModel recognizes the missing guard as the merge signal. The guard never
// reaches the document model, drafts, word counts, or find coordinates — the [rawText] /
// [rawSelection] accessors are the only legal way to read a field for document math.

private val GUARD: String = GUARD_CHAR.toString()

/** Build a guarded field for [raw] with a RAW-coordinate cursor/selection. */
internal fun fieldOf(raw: String, cursor: Int = 0, selEnd: Int = cursor): TextFieldValue {
    val s = (cursor + 1).coerceIn(1, raw.length + 1)
    val e = (selEnd + 1).coerceIn(s, raw.length + 1)
    return TextFieldValue(GUARD + raw, TextRange(s, e))
}

/** The field text without the sentinel guard. Tolerates guard-less values (tests, transients). */
internal val TextFieldValue.rawText: String
    get() = if (text.startsWith(GUARD)) text.substring(GUARD.length) else text

/** The selection in RAW text coordinates. */
internal val TextFieldValue.rawSelection: TextRange
    get() {
        val g = if (text.startsWith(GUARD)) GUARD.length else 0
        return TextRange(
            (selection.start - g).coerceAtLeast(0),
            (selection.end - g).coerceAtLeast(0)
        )
    }

/**
 * Detect a markdown shortcut prefix at the start of [text]. Returns (target style, prefix length
 * to strip) or null. Manual parse, no regex. Mapping: `- ` / `* ` → BULLET, `<digits>. ` →
 * NUMBERED, `> ` → QUOTE, `# ` → TITLE, `## ` → HEADING, `### ` → SUBHEADING (largest-to-smallest
 * across the three available levels), ``` or `` ` `` + space → CODE, `[] ` → CHECKBOX.
 */
internal fun detectMarkdownShortcut(text: String): Pair<ParagraphStyle, Int>? {
    if (text.startsWith("- ") || text.startsWith("* ")) return ParagraphStyle.BULLET to 2
    if (text.startsWith("> ")) return ParagraphStyle.QUOTE to 2
    if (text.startsWith("[] ")) return ParagraphStyle.CHECKBOX to 3
    if (text.startsWith("### ")) return ParagraphStyle.SUBHEADING to 4
    if (text.startsWith("## ")) return ParagraphStyle.HEADING to 3
    if (text.startsWith("# ")) return ParagraphStyle.TITLE to 2
    if (text.startsWith("```")) return ParagraphStyle.CODE to 3
    if (text.startsWith("` ")) return ParagraphStyle.CODE to 2
    var i = 0
    while (i < text.length && text[i].isDigit()) i++
    if (i in 1..3 && text.startsWith(". ", i)) return ParagraphStyle.NUMBERED to i + 2
    return null
}

/**
 * Reversible editor mutation. The undo/redo engine stores *commands* (targeted per-paragraph
 * diffs), NOT raw document snapshots. Image *removal* is intentionally NOT reversible (the
 * image file is deleted on removal — a permanent action).
 *
 * Invariant maintained by every command: `fields.size == document.paragraphs.size` at all
 * times. Paragraph removal inside commands goes through [EditorViewModel.cmdRemoveParagraphAt]
 * (paragraph ONLY) paired with an explicit [EditorViewModel.cmdRemoveField] — never a helper
 * that removes both (that double-removal was the historical desync crash).
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

/**
 * Split one paragraph into N (Enter = the N=2 case; multi-paragraph paste = N>2). Replaces the
 * former single-split command so a paste with any number of newlines is one undo step.
 */
private class MultiSplitCmd(
    val index: Int,
    val originalPara: EditorParagraph,
    val originalField: TextFieldValue,
    val newParas: List<EditorParagraph>,
    val newFields: List<TextFieldValue>,
    val prevFocus: Int,
    val newFocus: Int
) : EditorCommand {
    override fun undo(vm: EditorViewModel) {
        for (k in newParas.size - 1 downTo 1) {
            vm.cmdRemoveParagraphAt(index + k)
            vm.cmdRemoveField(index + k)
        }
        vm.cmdUpdateParagraph(index, originalPara)
        vm.cmdSetField(index, originalField)
        vm.cmdSetFocusedAndPending(prevFocus)
        vm.cmdAfterMutation()
    }
    override fun redo(vm: EditorViewModel) {
        vm.cmdUpdateParagraph(index, newParas[0])
        vm.cmdSetField(index, newFields[0])
        for (k in 1 until newParas.size) {
            vm.cmdInsertParagraphAfter(index + k - 1, newParas[k])
            vm.cmdAddField(index + k, newFields[k])
        }
        vm.cmdSetFocusedAndPending(newFocus)
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
        vm.cmdRemoveField(insertAt + 2)
        vm.cmdRemoveParagraphAt(insertAt + 1)
        vm.cmdRemoveField(insertAt + 1)
        vm.cmdSetFocusedAndPending(prevFocus)
        vm.cmdAfterMutation()
    }
    override fun redo(vm: EditorViewModel) {
        vm.cmdInsertParagraphAfter(insertAt, imagePara)
        vm.cmdAddField(insertAt + 1, fieldOf(""))
        vm.cmdInsertParagraphAfter(insertAt + 1, textPara)
        vm.cmdAddField(insertAt + 2, fieldOf(""))
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

    /** Injectable clock so undo-coalescing windows are testable with a fake time source. */
    internal var nowProvider: () -> Long = System::currentTimeMillis

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

    /**
     * Monotonic counter bumped on every document / meta edit. The [EditorPage] watches this via a
     * `LaunchedEffect` to fire a *debounced* autosave (3 s after the last edit). It starts at 0 and
     * is never bumped on [restoreFromHandle] / [loadNote] — nor on pure navigation (cursor moves,
     * find-jumps) — so neither a fresh load nor mere caret movement schedules an autosave.
     */
    var editVersion by mutableStateOf(0)
        private set

    // ── Autosave race guard (load-bearing) ──
    /**
     * Serializes [save] so a manual save and a concurrent ON_STOP / debounced autosave can't both
     * observe the "new note" branch and create duplicate rows. The first save of a brand-new note
     * (`noteId <= 0`) calls [NoteRepository.insertNote] and records the returned id in
     * [savedNoteId]; every subsequent save calls [NoteRepository.updateNote] with that id — never
     * `insertNote` again.
     */
    private val saveMutex = Mutex()
    /** The DB row id after the first save of a new note; non-null from the moment a row exists. */
    private var savedNoteId: Int? = noteId?.takeIf { it > 0 }

    // ── Undo / redo command stacks ──
    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()

    /** Coalescing state: the last committed pure-text edit (same-index bursts merge into one command). */
    private var lastTextEdit: TextEditInfo? = null
    private data class TextEditInfo(val index: Int, val at: Long, val insert: Boolean)

    // ── Sticky style: per-attribute target overrides applied to the next typed run ──
    //
    // `stickyOverrides[BOLD] = false` means "the next typed run will NOT be bold" — armed by
    // pressing Bold while the cursor sits inside a bold run. This is what makes un-bolding
    // forward-typed text possible (a force-on set could never turn a style off).
    private var stickyOverrides: Map<StickyStyle, Boolean> = emptyMap()
    private var stickyColor: String? = null
    private var stickySizeScale: Float = 1f

    init {
        fields.add(fieldOf(""))
        val restored = restoreFromHandle()
        if (restored) {
            loaded = true
            if (fields.isEmpty()) fields.add(fieldOf(""))
        } else if (noteId != null && noteId > 0) {
            loadNote()
        } else {
            loaded = true
        }
    }

    /** Test hook: the fields↔paragraphs parity invariant every mutation must preserve. */
    internal fun fieldParagraphParity(): Boolean = fields.size == document.paragraphs.size

    // ── SavedStateHandle draft persistence ─────────────────────────────────────────

    /**
     * Restore the editing buffer + meta from a [SavedStateHandle] draft (process-death recovery).
     * Returns true if a draft (title or document) was present and restored.
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
                        if (s != null) fieldOf(s.text, s.start.coerceAtLeast(0), s.end.coerceAtLeast(0))
                        else fieldOf(p.text)
                    )
                }
                if (fields.isEmpty()) fields.add(fieldOf(""))
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

    /** Serialize the editing buffer (document JSON + per-paragraph RAW selection tuples) to the handle. */
    private fun persistDocumentDraft() {
        handle[KEY_DOC] = json.encodeToString(RichDocument.serializer(), document.toModel())
        val sels = fields.map {
            val sel = it.rawSelection
            DraftSelection(it.rawText, sel.start, sel.end)
        }
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
        document.paragraphs.forEach { fields.add(fieldOf(it.text)) }
        if (fields.isEmpty()) fields.add(fieldOf(""))
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
    fun updateTitle(v: String) { _uiState.update { it.copy(title = v) }; handle[KEY_TITLE] = v; markEdited() }
    fun updateExcerpt(v: String) { _uiState.update { it.copy(excerpt = v) }; handle[KEY_EXCERPT] = v; markEdited() }
    fun updateDate(v: String) { _uiState.update { it.copy(date = v) }; handle[KEY_DATE] = v; markEdited() }
    fun updateWeather(key: String) { _uiState.update { it.copy(weatherKey = key) }; handle[KEY_WEATHER] = key; markEdited() }
    fun updateMood(key: String) { _uiState.update { it.copy(moodKey = key) }; handle[KEY_MOOD] = key; markEdited() }

    fun setCoverImage(uri: android.net.Uri) {
        val targetNoteId = savedNoteId ?: noteId ?: -1
        val path = imageStore.saveForNote(if (targetNoteId <= 0) 0 else targetNoteId, uri) ?: return
        _uiState.value.coverImagePath?.let { imageStore.delete(it) }
        _uiState.update { it.copy(coverImagePath = path) }
        handle[KEY_COVER] = path
        markEdited()
    }
    fun removeCoverImage() {
        _uiState.value.coverImagePath?.let { imageStore.delete(it) }
        _uiState.update { it.copy(coverImagePath = null) }
        handle[KEY_COVER] = null
        markEdited()
    }

    fun addTag(name: String) {
        val n = name.trim()
        if (n.isBlank()) return
        _uiState.update { st ->
            // Dedupe on the normalized identity, not the display string ("Work" == " work ").
            if (st.tagNames.any { Tag.idFor(it) == Tag.idFor(n) }) st
            else st.copy(tagNames = st.tagNames + n)
        }
        handle[KEY_TAGS] = json.encodeToString(ListSerializer(String.serializer()), _uiState.value.tagNames)
        markEdited()
    }
    fun removeTag(name: String) {
        _uiState.update { it.copy(tagNames = it.tagNames - name) }
        handle[KEY_TAGS] = json.encodeToString(ListSerializer(String.serializer()), _uiState.value.tagNames)
        markEdited()
    }

    /** Bump [editVersion] so the [EditorPage] debounced-autosave effect restarts its 3 s timer. */
    private fun markEdited() { editVersion++ }

    // ── Editing-buffer mutators ────────────────────────────────────────────────────

    fun onFocus(index: Int) {
        if (focusedIndex != index) {
            clearStickyOverrides()
            breakUndoCoalescing()
        }
        focusedIndex = index
        refreshStyleAtCursor()
    }

    /**
     * Front door for every field change. Routes:
     *  1. guard intact → normal text/selection processing (raw coordinates).
     *  2. guard deleted alone with the caret at raw 0 → soft-keyboard backspace at paragraph
     *     start → merge with the previous paragraph (or restore the guard at index 0).
     *  3. guard deleted as part of a larger edit (select-all delete/replace, or a caller passing
     *     raw text — e.g. tests) → treat the incoming text as raw content and re-arm the guard.
     */
    fun onTextChange(index: Int, newValue: TextFieldValue) {
        if (index !in document.paragraphs.indices) ensureCapacity(index + 1)
        if (!newValue.text.startsWith(GUARD)) {
            val before = fields.getOrNull(index)
            if (before != null && before.text.startsWith(GUARD) &&
                newValue.text == before.rawText &&
                before.rawSelection.collapsed && before.rawSelection.start == 0
            ) {
                if (index > 0) {
                    // Restore the guard first so a re-merge attempt can't observe a bare field.
                    fields[index] = fieldOf(before.rawText, 0)
                    mergeWithPrevious(index)
                } else {
                    fields[index] = fieldOf(before.rawText, 0)
                }
                return
            }
            processTextChange(index, fieldOf(newValue.text, newValue.selection.start, newValue.selection.end))
            return
        }
        processTextChange(index, newValue)
    }

    private fun processTextChange(index: Int, newValue: TextFieldValue) {
        val raw = newValue.rawText
        if ('\n' in raw) { handleNewlineChange(index, newValue); return }
        val beforePara = document.paragraphs[index]
        val beforeField = fields[index]

        if (raw == beforePara.text) {
            // Selection-only change: move the caret, but push NO command and schedule NO autosave.
            val moved = newValue.selection != beforeField.selection
            fields[index] = newValue
            if (moved) {
                clearStickyOverrides()
                breakUndoCoalescing()
                persistDocumentDraft()
                refreshStyleAtCursor()
            }
            return
        }

        val (insStart, insEnd) = computeInsertedRange(beforePara.text, raw)
        val inserted = if (insEnd > insStart) raw.substring(insStart, insEnd) else ""

        var finalPara = beforePara.afterTextChange(raw)
        if (stickyOverrides.isNotEmpty() && inserted.isNotEmpty()) {
            var styled = finalPara
            for ((st, target) in stickyOverrides) styled = applySticky(st, target, styled, insStart, insEnd)
            finalPara = styled
            clearStickyOverrides()
        }

        updateParagraph(index, finalPara)
        fields[index] = newValue
        commitTextEdit(ReplaceCmd(index, beforePara, finalPara, beforeField, newValue), inserted)

        // Markdown shortcut: fires only on a single-char keystroke completing a prefix on a BODY
        // paragraph with the caret exactly at the prefix end (never on paste).
        if (inserted.length == 1) maybeApplyMarkdownShortcut(index)
    }

    /**
     * A '\n' arrived in the field text — plain Enter or a multi-line paste. The span algebra
     * first absorbs the FULL new text (so pasted characters are never dropped), then the
     * combined paragraph is split on every newline into one paragraph per line, committed as a
     * single undoable [MultiSplitCmd]. The caret lands at its raw position mapped into
     * (line, offset).
     *
     * Special case: a pure Enter on an EMPTY bullet/numbered/checkbox item exits the list
     * (converts the paragraph to BODY) instead of continuing it — standard editor behavior.
     */
    private fun handleNewlineChange(index: Int, newValue: TextFieldValue) {
        val raw = newValue.rawText
        val beforePara = document.paragraphs[index]
        val beforeField = fields[index]

        if (raw == "\n" && beforePara.text.isEmpty() &&
            (beforePara.style == ParagraphStyle.BULLET ||
                beforePara.style == ParagraphStyle.NUMBERED ||
                beforePara.style == ParagraphStyle.CHECKBOX)
        ) {
            val exited = beforePara.copy(style = ParagraphStyle.BODY, checked = false)
            val exitedField = fieldOf("", 0)
            updateParagraph(index, exited)
            fields[index] = exitedField
            commit(ReplaceCmd(index, beforePara, exited, beforeField, exitedField))
            return
        }

        val combined = beforePara.afterTextChange(raw)
        val lines = combined.splitIntoLines()
        if (lines.size < 2) {
            updateParagraph(index, combined)
            fields[index] = newValue
            commit(ReplaceCmd(index, beforePara, combined, beforeField, newValue))
            return
        }

        // Map the raw caret position (in the '\n'-bearing combined text) to (line, offset).
        val cursor = newValue.rawSelection.start.coerceIn(0, combined.text.length)
        var acc = 0
        var lineIdx = lines.lastIndex
        var offset = lines.last().text.length
        for ((k, line) in lines.withIndex()) {
            val end = acc + line.text.length
            if (cursor <= end) { lineIdx = k; offset = cursor - acc; break }
            acc = end + 1  // skip the consumed '\n'
        }

        val prevFocus = focusedIndex
        val newFields = lines.mapIndexed { k, line ->
            if (k == lineIdx) fieldOf(line.text, offset.coerceIn(0, line.text.length))
            else fieldOf(line.text, line.text.length)
        }

        updateParagraph(index, lines[0])
        fields[index] = newFields[0]
        for (k in 1 until lines.size) {
            insertParagraphAfter(index + k - 1, lines[k])
            fields.add(index + k, newFields[k])
        }
        focusedIndex = index + lineIdx
        pendingFocusIndex = index + lineIdx
        commit(MultiSplitCmd(index, beforePara, beforeField, lines, newFields, prevFocus, index + lineIdx))
    }

    /**
     * Compatibility wrapper for the old Enter entry point: reconstructs the combined text and
     * routes through [handleNewlineChange] (single Enter = the N=2 [MultiSplitCmd] case).
     */
    fun splitParagraph(index: Int, beforeText: String, afterText: String) {
        if (index !in document.paragraphs.indices) return
        handleNewlineChange(index, fieldOf(beforeText + "\n" + afterText, beforeText.length + 1))
    }

    private fun maybeApplyMarkdownShortcut(index: Int) {
        val para = document.paragraphs.getOrNull(index) ?: return
        if (para.style != ParagraphStyle.BODY) return
        val field = fields.getOrNull(index) ?: return
        val sel = field.rawSelection
        if (!sel.collapsed) return
        val (target, prefixLen) = detectMarkdownShortcut(para.text) ?: return
        if (sel.start != prefixLen) return  // caret must sit exactly at the end of the trigger
        val stripped = para.text.substring(prefixLen)
        val converted = para.afterTextChange(stripped).copy(style = target, checked = false)
        val newField = fieldOf(stripped, 0)
        updateParagraph(index, converted)
        fields[index] = newField
        // Second command on top of the keystroke's own command: one undo restores the literal
        // prefix (e.g. "- "), a second undo restores the state before typing it.
        commit(ReplaceCmd(index, para, converted, field, newField))
    }

    /**
     * Merge paragraph [index] into the previous paragraph (backspace at start).
     * Requests focus on the merged paragraph with cursor at the join point.
     *
     * Image-paragraph merges are NOT recorded (file deletion is permanent — re-inserting the
     * paragraph would point at a missing file). Only text↔text merges push an undo command.
     */
    fun mergeWithPrevious(index: Int) {
        if (index <= 0) return
        if (index !in document.paragraphs.indices) return
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
        val mergedField = fieldOf(merged.text, joinCursor)
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
        if (lastTextIndex < fields.size) {
            val raw = fields[lastTextIndex].rawText
            fields[lastTextIndex] = fieldOf(raw, raw.length)
        }
        focusedIndex = lastTextIndex
        pendingFocusIndex = lastTextIndex
        afterNavigation()
    }

    fun setParagraphStyle(index: Int, style: ParagraphStyle) {
        if (index !in document.paragraphs.indices) return
        val beforePara = document.paragraphs[index]
        if (beforePara.style == style) return
        val afterPara = beforePara.copy(style = style, checked = false)
        updateParagraph(index, afterPara)
        commit(ReplaceCmd(index, beforePara, afterPara))
    }

    /** Set the focused paragraph's text alignment (Left/Center/Right). Reversible via the undo stack. */
    fun setParagraphAlignment(index: Int, alignment: ParagraphAlignment) {
        if (index !in document.paragraphs.indices) return
        val beforePara = document.paragraphs[index]
        if (beforePara.alignment == alignment) return
        val afterPara = beforePara.copy(alignment = alignment)
        updateParagraph(index, afterPara)
        commit(ReplaceCmd(index, beforePara, afterPara))
    }

    /**
     * Adjust the focused paragraph's indent level by [delta], clamped to `0..3`. Indent (+1) /
     * outdent (-1) at the boundary is a no-op (never negative, never above 3) rather than a crash.
     * Reversible via the undo stack.
     */
    fun setIndentLevel(index: Int, delta: Int) {
        if (index !in document.paragraphs.indices) return
        val beforePara = document.paragraphs[index]
        val newLevel = (beforePara.indentLevel + delta).coerceIn(0, 3)
        if (newLevel == beforePara.indentLevel) return
        val afterPara = beforePara.copy(indentLevel = newLevel)
        updateParagraph(index, afterPara)
        commit(ReplaceCmd(index, beforePara, afterPara))
    }

    /** Toggle a CHECKBOX paragraph's checked state. Reversible via the undo stack. */
    fun toggleChecked(index: Int) {
        if (index !in document.paragraphs.indices) return
        val beforePara = document.paragraphs[index]
        if (beforePara.style != ParagraphStyle.CHECKBOX) return
        val afterPara = beforePara.copy(checked = !beforePara.checked)
        updateParagraph(index, afterPara)
        commit(ReplaceCmd(index, beforePara, afterPara))
    }

    private fun currentSelection(): Pair<Int, TextRange>? {
        val i = focusedIndex
        if (i !in document.paragraphs.indices) return null
        val sel = fields.getOrNull(i)?.rawSelection ?: return null
        return i to sel
    }

    /**
     * Set a hyperlink [url] on the span covering the current selection. Requires a NON-empty
     * selection (linking zero characters is a no-op — the user must first select the text to
     * turn into a link). Reversible via the undo stack.
     */
    fun setLinkOnSelection(url: String) {
        val u = url.trim()
        if (u.isEmpty()) return
        val (i, sel) = currentSelection() ?: return
        val para = document.paragraphs[i]
        val s = minOf(sel.start, sel.end).coerceAtLeast(0)
        val e = maxOf(sel.start, sel.end).coerceAtMost(para.text.length)
        if (s >= e) return  // empty selection → no-op (link needs a non-empty anchor)
        val beforePara = para
        val updated = para.withSpanToggled(s, e) { it.copy(url = u) }
        updateParagraph(i, updated)
        commit(ReplaceCmd(i, beforePara, updated))
    }

    /**
     * Remove the hyperlink under the current selection. With a collapsed cursor, the full extent
     * of the link span under the caret is un-linked. Reversible via the undo stack.
     */
    fun clearLinkOnSelection() {
        val (i, sel) = currentSelection() ?: return
        val para = document.paragraphs[i]
        var s = minOf(sel.start, sel.end).coerceAtLeast(0)
        var e = maxOf(sel.start, sel.end).coerceAtMost(para.text.length)
        if (s == e) {
            // Collapsed: expand to the link run covering the char before the caret.
            val charIdx = if (s == 0) 0 else s - 1
            var pos = 0
            var found = false
            for (span in para.spans) {
                val end = pos + span.text.length
                if (span.url != null && charIdx in pos until end) {
                    s = pos; e = end; found = true; break
                }
                pos = end
            }
            if (!found) return
        }
        val beforePara = para
        val updated = para.withSpanToggled(s, e) { it.copy(url = null) }
        if (updated == beforePara) return
        updateParagraph(i, updated)
        commit(ReplaceCmd(i, beforePara, updated))
    }

    /** True when the cursor's span already carries a link (drives the Link button active-state). */
    fun linkAtCursor(): Boolean = uiState.value.linkAtCursor

    /**
     * Toggle `ParagraphStyle.CODE` on the focused paragraph. Code paragraphs render with a
     * monospace `SpanStyle` + a code-block background. Toggling off returns to `BODY`.
     * Reversible via the undo stack.
     */
    fun toggleCode() {
        val i = focusedIndex
        if (i !in document.paragraphs.indices) return
        val beforePara = document.paragraphs[i]
        val newStyle = if (beforePara.style == ParagraphStyle.CODE) ParagraphStyle.BODY else ParagraphStyle.CODE
        if (beforePara.style == newStyle) return
        val afterPara = beforePara.copy(style = newStyle)
        updateParagraph(i, afterPara)
        commit(ReplaceCmd(i, beforePara, afterPara))
    }

    /**
     * Find every occurrence of [query] across all paragraphs via plain [String.indexOf] (no regex
     * engine). Returns one [MatchLocation] per occurrence, in document order. Non-overlapping
     * matches: each search resumes at `index + query.length`.
     */
    fun searchInDocument(query: String): List<MatchLocation> {
        if (query.isEmpty()) return emptyList()
        val out = ArrayList<MatchLocation>()
        document.paragraphs.forEachIndexed { i, p ->
            val text = p.text
            var from = 0
            while (from <= text.length) {
                val idx = text.indexOf(query, from)
                if (idx < 0) break
                out.add(MatchLocation(i, idx, idx + query.length))
                from = idx + query.length
            }
        }
        return out
    }

    /**
     * Run [searchInDocument] for [query], publish the matches to [EditorUiState.findMatches], and
     * jump to the first match (focus + scroll + selection). Clears state when the query is empty.
     */
    fun updateFind(query: String) {
        val matches = searchInDocument(query)
        _uiState.update { it.copy(findMatches = matches, findIndex = if (matches.isEmpty()) -1 else 0) }
        if (matches.isNotEmpty()) navigateToMatch(matches[0])
    }

    /** Advance to the next find match (wraps around). No-op when there are no matches. */
    fun findNext() {
        val st = uiState.value
        if (st.findMatches.isEmpty()) return
        val next = (st.findIndex + 1) % st.findMatches.size
        _uiState.update { it.copy(findIndex = next) }
        navigateToMatch(st.findMatches[next])
    }

    /** Advance to the previous find match (wraps around). No-op when there are no matches. */
    fun findPrev() {
        val st = uiState.value
        if (st.findMatches.isEmpty()) return
        val prev = (st.findIndex - 1 + st.findMatches.size) % st.findMatches.size
        _uiState.update { it.copy(findIndex = prev) }
        navigateToMatch(st.findMatches[prev])
    }

    /** Clear find-in-page state (closes the bar). */
    fun clearFind() {
        _uiState.update { it.copy(findMatches = emptyList(), findIndex = -1) }
    }

    /** Highlights for a single paragraph — consumed by the per-paragraph [SpanVisualTransformation]. */
    fun findHighlightsForParagraph(index: Int): List<MatchLocation> {
        val st = uiState.value
        return if (st.findMatches.isEmpty()) emptyList()
        else st.findMatches.filter { it.paragraphIndex == index }
    }

    /**
     * Focus + scroll to a match and select its range so the user sees it. Pure navigation:
     * persists the caret but does NOT schedule an autosave (see [afterNavigation]).
     */
    private fun navigateToMatch(m: MatchLocation) {
        if (m.paragraphIndex !in document.paragraphs.indices) return
        val cur = fields.getOrNull(m.paragraphIndex)
        if (cur != null) {
            val raw = cur.rawText
            val e = m.end.coerceAtMost(raw.length)
            val s = m.start.coerceAtLeast(0).coerceAtMost(e)
            fields[m.paragraphIndex] = fieldOf(raw, s, e)
        }
        focusedIndex = m.paragraphIndex
        pendingFocusIndex = m.paragraphIndex
        afterNavigation()
    }

    fun toggleBold() = applyToSelection(StickyStyle.BOLD) { p, s, e -> p.toggleBold(s, e) }
    fun toggleItalic() = applyToSelection(StickyStyle.ITALIC) { p, s, e -> p.toggleItalic(s, e) }
    fun toggleUnderline() = applyToSelection(StickyStyle.UNDERLINE) { p, s, e -> p.toggleUnderline(s, e) }
    fun toggleStrikethrough() = applyToSelection(StickyStyle.STRIKETHROUGH) { p, s, e -> p.toggleStrikethrough(s, e) }
    fun setColor(color: String) = applyToSelection(StickyStyle.COLOR, colorArg = color) { p, s, e -> p.setColor(s, e, color) }
    fun setFontSizeScale(scale: Float) = applyToSelection(StickyStyle.SIZE, scaleArg = scale) { p, s, e -> p.setFontSizeScale(s, e, scale) }

    /**
     * Apply a span [transform] to the current selection. When the selection is EMPTY, arm a
     * sticky override for the next typed run: the override's target is the NEGATION of the
     * attribute's state at the cursor (so pressing Bold inside a bold run arms "not bold"),
     * and the armed state is surfaced through [EditorUiState.styleAtCursor] so the toolbar
     * highlights immediately. Re-tapping the same style disarms it.
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
            if (stickyStyle in stickyOverrides) {
                stickyOverrides = stickyOverrides - stickyStyle
                if (stickyStyle == StickyStyle.COLOR) stickyColor = null
                if (stickyStyle == StickyStyle.SIZE) stickySizeScale = 1f
            } else {
                val span = para.spanAt(s)
                val target = when (stickyStyle) {
                    StickyStyle.BOLD -> !(span?.bold ?: false)
                    StickyStyle.ITALIC -> !(span?.italic ?: false)
                    StickyStyle.UNDERLINE -> !(span?.underline ?: false)
                    StickyStyle.STRIKETHROUGH -> !(span?.strikethrough ?: false)
                    StickyStyle.COLOR, StickyStyle.SIZE -> true
                }
                stickyOverrides = stickyOverrides + (stickyStyle to target)
                if (stickyStyle == StickyStyle.COLOR) stickyColor = colorArg
                if (stickyStyle == StickyStyle.SIZE) stickySizeScale = scaleArg
            }
            refreshStyleAtCursor()
            return
        }
        val beforePara = para
        val updated = transform(para, s, e)
        updateParagraph(i, updated)
        commit(ReplaceCmd(i, beforePara, updated))
    }

    fun insertImageAfter(index: Int, sourceUri: android.net.Uri) {
        val targetNoteId = savedNoteId ?: noteId ?: -1
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
        fields.add((insertAt + 1).coerceAtMost(fields.size), fieldOf(""))
        // Auto-insert an empty text paragraph after the image so the user can continue typing
        insertParagraphAfter(insertAt + 1, textPara)
        fields.add((insertAt + 2).coerceAtMost(fields.size), fieldOf(""))
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

    // ── Undo / redo engine ─────────────────────────────────────────────────────────

    /** Undo the last mutation. A no-op (no crash) when the undo stack is empty. */
    fun undo() {
        val cmd = undoStack.removeLastOrNull() ?: return
        breakUndoCoalescing()
        cmd.undo(this)
        redoStack.addLast(cmd)
        refreshUndoRedoState()
    }

    /** Redo the last undone mutation. A no-op (no crash) when the redo stack is empty. */
    fun redo() {
        val cmd = redoStack.removeLastOrNull() ?: return
        breakUndoCoalescing()
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

    /** Apply a command: push → persist + refresh style. Non-text commands break coalescing. */
    private fun commit(cmd: EditorCommand) {
        breakUndoCoalescing()
        pushCommand(cmd)
        afterMutation()
    }

    /**
     * Commit a pure text edit with burst coalescing: consecutive single-run insertions (or
     * deletions) on the same paragraph within [COALESCE_WINDOW_MS] merge into the stack top, so
     * undo steps are word/burst-sized instead of per-keystroke. Whitespace starts a new step;
     * cursor moves, focus changes, undo/redo, and non-text commands all break the chain.
     */
    private fun commitTextEdit(cmd: ReplaceCmd, inserted: String) {
        val now = nowProvider()
        val insert = inserted.isNotEmpty()
        val last = lastTextEdit
        val top = undoStack.lastOrNull()
        val canCoalesce = last != null && top is ReplaceCmd &&
            top.beforeField != null && cmd.beforeField != null &&
            last.index == cmd.index && last.insert == insert &&
            now - last.at < COALESCE_WINDOW_MS &&
            (!insert || inserted.none { it.isWhitespace() })
        if (canCoalesce) {
            undoStack.removeLast()
            undoStack.addLast(
                ReplaceCmd(cmd.index, (top as ReplaceCmd).beforePara, cmd.afterPara, top.beforeField, cmd.afterField)
            )
            redoStack.clear()
            refreshUndoRedoState()
            afterMutation()
        } else {
            pushCommand(cmd)
            afterMutation()
        }
        lastTextEdit = TextEditInfo(cmd.index, now, insert)
    }

    private fun breakUndoCoalescing() { lastTextEdit = null }

    /** Persist the draft + refresh styleAtCursor + schedule autosave. Used after real edits. */
    private fun afterMutation() {
        persistDocumentDraft()
        refreshStyleAtCursor()
        markEdited()
    }

    /** Persist + refresh WITHOUT scheduling an autosave — for pure navigation (caret moves, find-jumps). */
    private fun afterNavigation() {
        persistDocumentDraft()
        refreshStyleAtCursor()
    }

    private fun clearStickyOverrides() {
        if (stickyOverrides.isEmpty() && stickyColor == null && stickySizeScale == 1f) return
        stickyOverrides = emptyMap()
        stickyColor = null
        stickySizeScale = 1f
    }

    private fun refreshUndoRedoState() {
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
    }

    private fun refreshStyleAtCursor() {
        _uiState.update {
            val (spanStyle, pStyle, pAlign, pIndent, linkActive) = computeStyleAtCursor()
            val codeActive = pStyle == ParagraphStyle.CODE
            it.copy(
                styleAtCursor = spanStyle,
                paragraphStyleAtCursor = pStyle,
                paragraphAlignmentAtCursor = pAlign,
                indentLevelAtCursor = pIndent,
                linkAtCursor = linkActive,
                codeAtCursor = codeActive
            )
        }
    }

    /**
     * The merged [SpanStyle] at the cursor plus the focused paragraph's paragraph-level state.
     * Armed sticky overrides are merged in so the toolbar highlights an armed style BEFORE the
     * user types (e.g. tapping Bold on an empty selection immediately lights the Bold button).
     */
    private fun computeStyleAtCursor(): SpanStyleAtCursor {
        val i = focusedIndex
        val para = document.paragraphs.getOrNull(i)
        if (para == null) {
            return SpanStyleAtCursor(null, ParagraphStyle.BODY, ParagraphAlignment.START, 0, false)
        }
        val pos = fields.getOrNull(i)?.rawSelection?.start
        val span = pos?.let { para.spanAt(it) }
        val effective: RichSpan? = if (stickyOverrides.isEmpty()) span else {
            var sp = span ?: RichSpan("")
            for ((st, target) in stickyOverrides) {
                sp = when (st) {
                    StickyStyle.BOLD -> sp.copy(bold = target)
                    StickyStyle.ITALIC -> sp.copy(italic = target)
                    StickyStyle.UNDERLINE -> sp.copy(underline = target)
                    StickyStyle.STRIKETHROUGH -> sp.copy(strikethrough = target)
                    StickyStyle.COLOR -> sp.copy(color = if (target) stickyColor else null)
                    StickyStyle.SIZE -> sp.copy(fontSizeScale = if (target) stickySizeScale else 1f)
                }
            }
            sp
        }
        return SpanStyleAtCursor(
            effective?.toSpanStyle(), para.style, para.alignment, para.indentLevel, span?.url != null
        )
    }

    /** Bundle returned by [computeStyleAtCursor] — span style + paragraph-level state at the cursor. */
    private data class SpanStyleAtCursor(
        val span: SpanStyle?,
        val paragraphStyle: ParagraphStyle,
        val paragraphAlignment: ParagraphAlignment,
        val indentLevel: Int,
        val linkActive: Boolean
    )

    /** Apply a sticky override to a range: SETS the attribute to [target] (on OR off). */
    private fun applySticky(st: StickyStyle, target: Boolean, p: EditorParagraph, s: Int, e: Int): EditorParagraph {
        val transform: (RichSpan) -> RichSpan = when (st) {
            StickyStyle.BOLD -> { it -> it.copy(bold = target) }
            StickyStyle.ITALIC -> { it -> it.copy(italic = target) }
            StickyStyle.UNDERLINE -> { it -> it.copy(underline = target) }
            StickyStyle.STRIKETHROUGH -> { it -> it.copy(strikethrough = target) }
            StickyStyle.COLOR -> { it -> it.copy(color = if (target) stickyColor else null) }
            StickyStyle.SIZE -> { it -> it.copy(fontSizeScale = if (target) stickySizeScale else 1f) }
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

    // ── Internal helpers used by [EditorCommand] undo/redo ─────────────────────────

    internal fun cmdUpdateParagraph(index: Int, p: EditorParagraph) {
        if (index in document.paragraphs.indices) updateParagraph(index, p)
    }
    internal fun cmdInsertParagraphAfter(index: Int, p: EditorParagraph) {
        insertParagraphAfter(index, p)
    }
    /** Removes ONLY the paragraph — commands pair this with an explicit [cmdRemoveField]. */
    internal fun cmdRemoveParagraphAt(index: Int) {
        removeParagraphOnly(index)
    }
    internal fun cmdSetField(index: Int, f: TextFieldValue) {
        if (index in fields.indices) fields[index] = f
    }
    internal fun cmdAddField(index: Int, f: TextFieldValue) {
        fields.add(index.coerceIn(0, fields.size), f)
    }
    internal fun cmdRemoveField(index: Int) {
        if (index in fields.indices) fields.removeAt(index)
    }
    internal fun cmdSetFocusedAndPending(focus: Int) {
        focusedIndex = focus
        pendingFocusIndex = focus
    }
    internal fun cmdAfterMutation() {
        afterMutation()
    }

    /**
     * Persist the current document + meta to the DB. Silent when [onDone] is empty (used by the
     * autosave paths in [EditorPage]: ON_STOP + the debounced LaunchedEffect). The manual save
     * button passes an [onDone] that navigates.
     *
     * Race guard (load-bearing): the whole body runs under [saveMutex] — see [savedNoteId].
     *
     * Empty-note guard: a pristine brand-new note (empty document, blank title, no cover) never
     * inserts a row, so backing out of an untouched editor leaves no junk entry. This replaces a
     * back-confirm dialog — the ON_STOP autosave already covers every leave-screen path.
     */
    fun save(onDone: () -> Unit = {}): Job = viewModelScope.launch {
        saveMutex.withLock {
            val now = System.currentTimeMillis()
            var model = document.toModel()
            val st = _uiState.value
            if (savedNoteId == null && model.isEmpty && st.title.isBlank() &&
                st.excerpt.isBlank() && st.coverImagePath == null
            ) {
                return@withLock
            }
            val content = json.encodeToString(RichDocument.serializer(), model)
            // Prefer user-set cover; fall back to first inline image
            val resolvedCover = st.coverImagePath ?: model.paragraphs.firstNotNullOfOrNull { it.image }?.path
            if (savedNoteId == null) {
                // First save of a new note: insert + record the id.
                val note = Note(
                    title = st.title, excerpt = st.excerpt, content = content,
                    date = st.date, weather = st.weatherKey, mood = st.moodKey,
                    coverImagePath = resolvedCover,
                    createdAt = now, updatedAt = now, version = 1
                )
                savedNoteId = repo.insertNote(note).toInt()
                // Images picked before the first save live under the provisional folder
                // images/0/ — re-home them into images/<id>/ now that a row id exists, and
                // rewrite the stored paths (document + cover) to match.
                if (rehomeImages(savedNoteId!!)) {
                    model = document.toModel()
                    val fixedContent = json.encodeToString(RichDocument.serializer(), model)
                    val fixedCover = _uiState.value.coverImagePath
                        ?: model.paragraphs.firstNotNullOfOrNull { it.image }?.path
                    repo.getNote(savedNoteId!!)?.let {
                        repo.updateNote(it.copy(content = fixedContent, coverImagePath = fixedCover))
                    }
                    persistDocumentDraft()
                }
                persistTags(savedNoteId!!)
            } else {
                // Subsequent save (manual or autosave): update the existing row. NEVER insert again.
                val existing = repo.getNote(savedNoteId!!)
                val note = Note(
                    id = savedNoteId!!,
                    title = st.title, excerpt = st.excerpt, content = content,
                    date = st.date, weather = st.weatherKey, mood = st.moodKey,
                    coverImagePath = resolvedCover,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    version = (existing?.version ?: 1) + 1
                )
                repo.updateNote(note)
                persistTags(savedNoteId!!)
            }
        }
        onDone()
    }

    /**
     * Move any image files stored under a different note folder (the provisional `images/0/`)
     * into `images/<noteId>/`, updating the in-memory document + cover path. Returns true when
     * anything moved. Idempotent — a second save finds every path already in place.
     */
    private fun rehomeImages(noteId: Int): Boolean {
        var changed = false
        val updated = document.paragraphs.map { p ->
            val img = p.image ?: return@map p
            val newPath = imageStore.relocateToNote(img.path, noteId)
            if (newPath != img.path) {
                changed = true
                p.copy(image = img.copy(path = newPath))
            } else p
        }
        if (changed) document = document.copy(paragraphs = updated)
        val cover = _uiState.value.coverImagePath
        if (cover != null) {
            val newCover = imageStore.relocateToNote(cover, noteId)
            if (newCover != cover) {
                changed = true
                _uiState.update { it.copy(coverImagePath = newCover) }
                handle[KEY_COVER] = newCover
            }
        }
        return changed
    }

    /**
     * Replace the document with the given template. Clears the undo/redo stacks: a template
     * application is a wholesale reset. Focuses the first paragraph.
     */
    fun applyTemplate(doc: RichDocument) {
        undoStack.clear()
        redoStack.clear()
        breakUndoCoalescing()
        clearStickyOverrides()
        refreshUndoRedoState()
        val editor = if (doc.paragraphs.isEmpty()) EditorDocument() else doc.toEditor()
        document = editor
        fields.clear()
        document.paragraphs.forEach { fields.add(fieldOf(it.text)) }
        if (fields.isEmpty()) fields.add(fieldOf(""))
        focusedIndex = 0
        pendingFocusIndex = 0
        afterMutation()
    }

    /**
     * Upsert each tag by its normalized-name identity ([Tag.idFor]) and atomically replace the
     * note's tag set. Existing tags keep their `createdAt`/`color`; a new tag gets a
     * deterministic palette color ([Tag.colorFor]). Orphan cleanup happens inside the repo.
     */
    private suspend fun persistTags(noteId: Int) {
        val now = System.currentTimeMillis()
        val ids = _uiState.value.tagNames.map { name ->
            val id = Tag.idFor(name)
            if (repo.getTag(id) == null) {
                repo.upsertTag(Tag(id, name.trim(), Tag.colorFor(name), now))
            }
            id
        }
        repo.setNoteTags(noteId, ids.distinct())
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
    /** Removes paragraph AND its field. Used by direct mutators (merge, image removal). */
    private fun removeParagraphInternal(index: Int) {
        if (document.size <= 1) {
            updateParagraph(0, EditorParagraph())
            if (fields.isEmpty()) fields.add(fieldOf("")) else fields[0] = fieldOf("")
            return
        }
        val list = document.paragraphs.toMutableList()
        list.removeAt(index)
        document = document.copy(paragraphs = list)
        if (index in fields.indices) fields.removeAt(index)
    }
    /** Removes ONLY the paragraph — the command engine removes fields explicitly. */
    private fun removeParagraphOnly(index: Int) {
        if (index !in document.paragraphs.indices) return
        if (document.size <= 1) {
            updateParagraph(0, EditorParagraph())
            return
        }
        val list = document.paragraphs.toMutableList()
        list.removeAt(index)
        document = document.copy(paragraphs = list)
    }
    private fun ensureCapacity(size: Int) {
        while (document.paragraphs.size < size) {
            document = document.copy(paragraphs = document.paragraphs + EditorParagraph())
            fields.add(fieldOf(""))
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
        const val COALESCE_WINDOW_MS = 800L
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
