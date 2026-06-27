package io.github.nknote.ui.editor.richtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Renders a paragraph's [io.github.nknote.model.RichSpan]s as styled text inside an editable [BasicTextField].
 * This is the supported Compose path for editable rich text: the field stores plain text in its
 * TextFieldValue, and this transformation produces the styled visual [AnnotatedString].
 *
 * The visual text is `markerPrefix + rawText` (where `markerPrefix` is the indent whitespace +
 * the `• ` / `1. ` / `" ` marker wired in by [EditorParagraph.toAnnotatedString]). The
 * [OffsetMapping] shifts by [EditorParagraph.markerPrefixLength] so the cursor stays aligned
 * with the raw text — i.e. the user clicks/edits inside the raw text, and the marker prefix is
 * purely visual.
 */
class SpanVisualTransformation(
    private val paragraph: EditorParagraph,
    private val numberedCounter: Int = 1,
    private val codeBackground: Color = DefaultCodeBackground
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = if (paragraph.text == text.text) {
            paragraph.toAnnotatedString(numberedCounter, codeBackground)
        } else {
            buildStyledFromText(text.text, paragraph.spans)
        }
        val markerLen = paragraph.markerPrefixLength(numberedCounter)
        val offsetMapping = if (markerLen == 0) {
            OffsetMapping.Identity
        } else {
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset + markerLen
                override fun transformedToOriginal(offset: Int): Int =
                    (offset - markerLen).coerceAtLeast(0)
            }
        }
        return TransformedText(styled, offsetMapping)
    }

    private fun buildStyledFromText(text: String, spans: List<io.github.nknote.model.RichSpan>): AnnotatedString {
        if (text.isEmpty()) {
            // Empty paragraph still shows the marker (e.g. an empty bullet line shows "•  ").
            return paragraph.toAnnotatedString(numberedCounter, codeBackground)
        }
        // Fallback when the live field text doesn't exactly match the paragraph's stored text
        // (e.g. mid-keystroke): render plain text but still prepend the marker prefix so the
        // visual stays consistent with sibling paragraphs.
        val covered = spans.sumOf { it.text.length }
        return if (covered == text.length) {
            paragraph.toAnnotatedString(numberedCounter, codeBackground)
        } else {
            buildAnnotatedString {
                val marker = paragraph.marker(numberedCounter)
                val indent = paragraph.indentPrefix()
                if (indent.isNotEmpty() || marker.isNotEmpty()) {
                    append(indent)
                    append(marker)
                }
                append(text)
            }
        }
    }
}
