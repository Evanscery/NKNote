package io.github.nknote.model

import kotlinx.serialization.Serializable

/**
 * Portable rich-text document model. Serialized to JSON and stored in [io.github.nknote.data.entity.Note.content].
 *
 * The same schema is intended to be reused by the future web / desktop targets (see docs/multiplatform.md)
 * so that a note's content is byte-for-byte identical across platforms.
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
    val image: InlineImage? = null
)

@Serializable
enum class ParagraphStyle { TITLE, HEADING, SUBHEADING, BODY, QUOTE, BULLET, NUMBERED }

@Serializable
data class RichSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: String? = null,   // ARGB hex, null = theme default
    val fontSizeScale: Float = 1f
)

@Serializable
data class InlineImage(
    val path: String,              // app-internal file path (ImageStore)
    val width: Int,
    val height: Int
)