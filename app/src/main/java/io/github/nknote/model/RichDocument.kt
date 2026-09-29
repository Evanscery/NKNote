package io.github.nknote.model

import kotlinx.serialization.Serializable

/**
 * Portable rich-text document model. Serialized to JSON and stored in [io.github.nknote.data.entity.Note.content].
 *
 * The same schema is intended to be reused by the future web / desktop targets (see docs/multiplatform.md)
 * so that a note's content is byte-for-byte identical across platforms.
 *
 * Schema is backward-compatible: every field added after v1 carries a default so legacy JSON
 * (encoded by older app versions) decodes without throwing. The serializer is configured with
 * `ignoreUnknownKeys = true` + `encodeDefaults = true` (see [io.github.nknote.ui.editor.EditorViewModel]
 * and [io.github.nknote.ui.home.HomeViewModel]), so future-added fields also round-trip cleanly.
 */
@Serializable
data class RichDocument(
    val paragraphs: List<RichParagraph> = emptyList()
) {
    val isEmpty: Boolean get() = paragraphs.all { it.spans.none { s -> s.text.isNotEmpty() } && it.image == null }
    fun plainText(): String = paragraphs.joinToString("\n") { p ->
        p.spans.joinToString("") { it.text }
    }
}

@Serializable
data class RichParagraph(
    val spans: List<RichSpan> = emptyList(),
    val style: ParagraphStyle = ParagraphStyle.BODY,
    val image: InlineImage? = null,
    val alignment: ParagraphAlignment = ParagraphAlignment.START,
    val indentLevel: Int = 0,  // 0..3; clamped at the editor/toolbar layer
    val checked: Boolean = false   // v3: meaningful only for ParagraphStyle.CHECKBOX
)

/**
 * v3 adds CHECKBOX (task-list item). Enum values are appended, never reordered. Note the
 * one-way compatibility: `ignoreUnknownKeys` does NOT cover unknown enum VALUES, so an old
 * APK decoding a CHECKBOX note falls back to an empty editor document via its existing
 * `runCatching` guards — no crash, and the JSON in the DB is untouched.
 */
@Serializable
enum class ParagraphStyle { TITLE, HEADING, SUBHEADING, BODY, QUOTE, BULLET, NUMBERED, CODE, CHECKBOX }

@Serializable
enum class ParagraphAlignment { START, CENTER, END }

@Serializable
data class RichSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: String? = null,   // ARGB hex, null = theme default
    val fontSizeScale: Float = 1f,
    val url: String? = null      // when non-null, this span is a hyperlink (rendered underlined + LinkAnnotation)
)

@Serializable
data class InlineImage(
    val path: String,              // app-internal file path (ImageStore)
    val width: Int,
    val height: Int
)
