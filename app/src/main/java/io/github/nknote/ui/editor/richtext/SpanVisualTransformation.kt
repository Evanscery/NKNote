package io.github.nknote.ui.editor.richtext

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Renders a paragraph's [RichSpan]s as styled text inside an editable [BasicTextField].
 * This is the supported Compose path for editable rich text: the field stores plain text in its
 * TextFieldValue, and this transformation produces the styled visual [AnnotatedString].
 * Offsets are identity because the visible text equals the raw text (only styling differs).
 */
class SpanVisualTransformation(private val paragraph: EditorParagraph) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = if (paragraph.text == text.text) paragraph.toAnnotatedString()
        else buildStyledFromText(text.text, paragraph.spans)
        return TransformedText(styled, OffsetMapping.Identity)
    }

    private fun buildStyledFromText(text: String, spans: List<io.github.nknote.model.RichSpan>): AnnotatedString {
        if (text.isEmpty() || spans.isEmpty()) return AnnotatedString(text)
        // Fallback: if spans don't cover the current text exactly, render plain.
        val covered = spans.sumOf { it.text.length }
        return if (covered == text.length) paragraph.toAnnotatedString() else AnnotatedString(text)
    }
}