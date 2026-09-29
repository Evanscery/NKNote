package io.github.nknote.ui.editor.richtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Zero-width-space (U+200B) sentinel prepended to every editor field (see EditorViewModel's guard docs). */
val GUARD_CHAR: Char = 8203.toChar()

/**
 * Renders a paragraph's [io.github.nknote.model.RichSpan]s as styled text inside an editable [BasicTextField].
 * This is the supported Compose path for editable rich text: the field stores plain text in its
 * TextFieldValue, and this transformation produces the styled visual [AnnotatedString].
 *
 * The field's original text is `guard + rawText` where `guard` is [guardPrefixLength] sentinel
 * characters (the editor's soft-keyboard backspace detector, see EditorViewModel; the read-only
 * renderer passes 0). The visual text is `markerPrefix + rawText` (where `markerPrefix` is the
 * indent whitespace + the `• ` / `1. ` / `" ` marker wired in by [EditorParagraph.toAnnotatedString]).
 * The [OffsetMapping] therefore maps original offsets down by the guard and up by the marker —
 * and clamps `transformedToOriginal` to at least the guard length, which also keeps the cursor
 * from ever landing before the sentinel.
 */
class SpanVisualTransformation(
    private val paragraph: EditorParagraph,
    private val numberedCounter: Int = 1,
    private val codeBackground: Color = DefaultCodeBackground,
    private val highlights: List<HighlightRange> = emptyList(),
    private val highlightColor: Color = DefaultHighlightColor,
    private val guardPrefixLength: Int = 0
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Defensive: strip the guard only if it is actually present (a transient IME state may
        // hand us sentinel-less text for one frame before the ViewModel restores the invariant).
        val guard = if (guardPrefixLength > 0 &&
            text.text.length >= guardPrefixLength &&
            text.text.take(guardPrefixLength).all { it == GUARD_CHAR }
        ) guardPrefixLength else 0
        val visible = if (guard == 0) text.text else text.text.substring(guard)

        val styled = if (paragraph.text == visible) {
            paragraph.toAnnotatedString(numberedCounter, codeBackground, highlights, highlightColor)
        } else {
            buildStyledFromText(visible)
        }
        val markerLen = paragraph.markerPrefixLength(numberedCounter)
        val visibleLen = visible.length
        val offsetMapping = if (markerLen == 0 && guard == 0) {
            OffsetMapping.Identity
        } else {
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    (offset - guard).coerceIn(0, visibleLen) + markerLen

                override fun transformedToOriginal(offset: Int): Int =
                    (offset - markerLen).coerceIn(0, visibleLen) + guard
            }
        }
        return TransformedText(styled, offsetMapping)
    }

    private fun buildStyledFromText(text: String): AnnotatedString {
        if (text.isEmpty()) {
            // Empty paragraph still shows the marker (e.g. an empty bullet line shows "•  ").
            return paragraph.toAnnotatedString(numberedCounter, codeBackground, highlights, highlightColor)
        }
        val spans = paragraph.spans
        val covered = spans.sumOf { it.text.length }
        return if (covered == text.length) {
            // Equal length ⇒ the live field text differs from the stored paragraph text only by
            // a same-length replacement (autocorrect / IME candidate swap). Apply each stored
            // span's STYLE positionally over the LIVE text — never render the stale stored text.
            buildAnnotatedString {
                val marker = paragraph.marker(numberedCounter)
                val indent = paragraph.indentPrefix()
                append(indent)
                append(marker)
                var pos = 0
                for (span in spans) {
                    pushStyle(span.toSpanStyle())
                    append(text.substring(pos, pos + span.text.length))
                    pop()
                    pos += span.text.length
                }
                overlayHighlights(indent.length + marker.length, text.length)
            }
        } else {
            // Mid-keystroke length mismatch: render plain live text with the marker prefix so the
            // visual stays consistent with sibling paragraphs. Find highlights are overlaid too.
            buildAnnotatedString {
                val marker = paragraph.marker(numberedCounter)
                val indent = paragraph.indentPrefix()
                append(indent)
                append(marker)
                append(text)
                overlayHighlights(indent.length + marker.length, text.length)
            }
        }
    }

    private fun androidx.compose.ui.text.AnnotatedString.Builder.overlayHighlights(
        prefixLen: Int,
        textLen: Int
    ) {
        if (highlights.isEmpty()) return
        for (h in highlights) {
            val s = (h.start + prefixLen).coerceIn(prefixLen, prefixLen + textLen)
            val e = (h.end + prefixLen).coerceIn(s, prefixLen + textLen)
            if (e > s) addStyle(SpanStyle(background = highlightColor), s, e)
        }
    }
}
