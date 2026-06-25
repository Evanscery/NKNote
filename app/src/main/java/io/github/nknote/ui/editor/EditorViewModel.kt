package io.github.nknote.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepository
import io.github.nknote.model.InlineImage
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate

class EditorViewModel(
    private val noteId: Int?,
    private val imageStore: ImageStore,
    private val repo: NoteRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var title by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var date by mutableStateOf(LocalDate.now().toString())
        private set
    var weatherKey by mutableStateOf("")
        private set
    var moodKey by mutableStateOf("")
        private set
    val tagNames = mutableStateListOf<String>()

    private var document by mutableStateOf(EditorDocument())
    val paragraphs: List<EditorParagraph> get() = document.paragraphs
    val fields = mutableStateListOf<TextFieldValue>()
    var focusedIndex by mutableStateOf(0)
        private set

    /** When set, the EditorPage requests focus on the paragraph at this index. */
    var pendingFocusIndex by mutableStateOf(-1)
        private set

    var loaded by mutableStateOf(noteId == null || noteId <= 0)
        private set

    init {
        fields.add(TextFieldValue(""))
        if (noteId != null && noteId > 0) loadNote() else loaded = true
    }

    private fun loadNote() = viewModelScope.launch {
        val id = noteId ?: return@launch
        val note = repo.getNote(id) ?: run { loaded = true; return@launch }
        title = note.title
        description = note.description
        date = note.date
        weatherKey = note.weather
        moodKey = note.mood
        document = runCatching { json.decodeFromString<RichDocument>(note.content).toEditor() }
            .getOrDefault(EditorDocument())
        if (document.size == 0) document = EditorDocument()
        fields.clear()
        document.paragraphs.forEach { fields.add(TextFieldValue(it.text)) }
        if (fields.isEmpty()) fields.add(TextFieldValue(""))
        loaded = true
        launch { repo.observeTagsForNote(id).collect { ts -> tagNames.clear(); tagNames.addAll(ts.map { it.name }) } }
    }

    fun updateTitle(v: String) { title = v }
    fun updateDescription(v: String) { description = v }
    fun updateDate(v: String) { date = v }
    fun updateWeather(key: String) { weatherKey = key }
    fun updateMood(key: String) { moodKey = key }

    fun addTag(name: String) {
        val n = name.trim()
        if (n.isNotBlank() && n !in tagNames) tagNames.add(n)
    }
    fun removeTag(name: String) { tagNames.remove(name) }

    fun onFocus(index: Int) { focusedIndex = index }

    fun onTextChange(index: Int, newValue: TextFieldValue) {
        if (index !in document.paragraphs.indices) ensureCapacity(index + 1)
        val para = document.paragraphs[index]
        val updated = para.afterTextChange(newValue.text)
        updateParagraph(index, updated)
        fields[index] = newValue
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
            return
        }
        if (prev.image != null) {
            // previous is image: merge skips it, focus on the text paragraph before it
            // simplest: just remove the image paragraph, stay on current
            removeImageParagraph(index - 1)
            focusedIndex = index - 1
            pendingFocusIndex = index - 1
            return
        }
        val joinCursor = prev.text.length
        val merged = prev.merge(cur)
        updateParagraph(index - 1, merged)
        removeParagraphInternal(index)
        fields[index - 1] = TextFieldValue(merged.text, TextRange(joinCursor))
        focusedIndex = index - 1
        pendingFocusIndex = index - 1
    }

    fun consumePendingFocus() { pendingFocusIndex = -1 }

    fun setParagraphStyle(index: Int, style: ParagraphStyle) {
        if (index in document.paragraphs.indices) {
            updateParagraph(index, document.paragraphs[index].copy(style = style))
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
    }

    fun insertImageAfter(index: Int, sourceUri: android.net.Uri) {
        val targetNoteId = noteId ?: -1
        val path = imageStore.saveForNote(if (targetNoteId <= 0) 0 else targetNoteId, sourceUri) ?: return
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, opts)
        val img = InlineImage(path, opts.outWidth.coerceAtLeast(1), opts.outHeight.coerceAtLeast(1))
        insertParagraphAfter(index.coerceAtMost(document.size - 1), EditorParagraph(image = img))
        fields.add((index + 1).coerceAtMost(fields.size), TextFieldValue(""))
        focusedIndex = index + 1
        pendingFocusIndex = index + 1
    }

    fun removeImageParagraph(index: Int) {
        document.paragraphs.getOrNull(index)?.image?.let { imageStore.delete(it.path) }
        removeParagraphInternal(index)
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val model = document.toModel()
        val content = json.encodeToString(RichDocument.serializer(), model)
        val existing = noteId?.let { repo.getNote(it) }
        val note = (existing ?: Note(
            title = title, description = description, content = content,
            date = date, weather = weatherKey, mood = moodKey,
            coverImagePath = model.paragraphs.firstNotNullOfOrNull { it.image }?.path,
            createdAt = now, updatedAt = now
        )).copy(
            title = title, description = description, content = content,
            date = date, weather = weatherKey, mood = moodKey,
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
        val ids = tagNames.map { name ->
            val id = "tag:" + name.hashCode().toUInt().toString()
            repo.upsertTag(Tag(id, name, "#5E7A6E", System.currentTimeMillis()))
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
}