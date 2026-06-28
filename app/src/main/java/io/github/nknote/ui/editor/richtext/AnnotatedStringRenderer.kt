package io.github.nknote.ui.editor.richtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichSpan

/** Parse "#RRGGBB" / "#AARRGGBB" into a Compose [Color]. Returns null on bad input. */
fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val h = hex.removePrefix("#")
    return runCatching {
        when (h.length) {
            6 -> Color(
                red = h.substring(0, 2).toInt(16),
                green = h.substring(2, 4).toInt(16),
                blue = h.substring(4, 6).toInt(16)
            )
            8 -> Color(
                alpha = h.substring(0, 2).toInt(16),
                red = h.substring(2, 4).toInt(16),
                green = h.substring(4, 6).toInt(16),
                blue = h.substring(6, 8).toInt(16)
            )
            else -> null
        }
    }.getOrNull()
}

/** Fallback code-block background when no theme color is supplied (e.g. in non-Composable tests). */
val DefaultCodeBackground: Color = Color(0xFFEEEEEE)

/** Fallback find-in-page highlight color (translucent yellow) when no theme color is supplied. */
val DefaultHighlightColor: Color = Color(0x66FFEB3B)

/**
 * A find-in-page highlight range, in *raw paragraph text* coordinates (end-exclusive). The
 * renderer shifts each range by `markerPrefixLength` when laying the highlight over the
 * transformed (marker + indent + text) string. Kept in the richtext package so the renderer
 * does not depend on the editor ViewModel's `MatchLocation`.
 */
data class HighlightRange(val start: Int, val end: Int)

/**
 * Convert this [RichSpan] to a Compose [SpanStyle]. Public so [io.github.nknote.ui.editor.EditorViewModel]
 * can reuse it for the active-style-at-cursor query (`EditorUiState.styleAtCursor`) — keeping the
 * style mapping in one place so the editor indicator and the renderer never drift apart.
 */
fun RichSpan.toSpanStyle(): SpanStyle {
    val decoration = when {
        underline && strikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        underline -> TextDecoration.Underline
        strikethrough -> TextDecoration.LineThrough
        url != null -> TextDecoration.Underline   // links render underlined in the editor
        else -> null
    }
    return SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        textDecoration = decoration,
        color = parseHexColor(color) ?: Color.Unspecified,
        fontSize = if (fontSizeScale != 1f) fontSizeScale.em else TextUnit.Unspecified,
    )
}

private fun baseStyleFor(style: ParagraphStyle, codeBackground: Color): SpanStyle = when (style) {
    ParagraphStyle.TITLE -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp)
    ParagraphStyle.HEADING -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
    ParagraphStyle.SUBHEADING -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    ParagraphStyle.QUOTE -> SpanStyle(fontStyle = FontStyle.Italic)
    ParagraphStyle.BULLET, ParagraphStyle.NUMBERED -> SpanStyle()
    ParagraphStyle.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
    ParagraphStyle.BODY -> SpanStyle()
}

/** Indent leading-whitespace prefix; consumed by [SpanVisualTransformation] for offset math. */
fun EditorParagraph.indentPrefix(): String =
    if (indentLevel > 0) " ".repeat(indentLevel.coerceIn(0, 3) * 2) else ""

/**
 * Prefix shown for list/quote paragraphs. For `NUMBERED`, [numberedCounter] is the running
 * sequence number (computed by [numberedCounters]); a non-`NUMBERED` paragraph resets the
 * caller's counter to 1 (handled by [numberedCounters]).
 */
fun EditorParagraph.marker(numberedCounter: Int = 1): String = when (style) {
    ParagraphStyle.BULLET -> "•  "
    ParagraphStyle.NUMBERED -> "$numberedCounter. "
    ParagraphStyle.QUOTE -> "“  "
    else -> ""
}

/** Total visual-prefix length (indent + marker) for [paragraph]; used to build the [OffsetMapping]. */
fun EditorParagraph.markerPrefixLength(numberedCounter: Int = 1): Int =
    indentPrefix().length + marker(numberedCounter).length

/**
 * Render the paragraph (spans + marker + indent prefix) as a single [AnnotatedString].
 *
 * The marker (`• `, `1. `, `" `) and indent whitespace are prepended INSIDE this string
 * (not as a separate TextField), so the visual text = prefix + raw text. Callers wrapping this
 * in a [VisualTransformation] must shift offsets by [markerPrefixLength].
 *
 * `RichSpan.url` is emitted as a [UrlAnnotation] (clickable when rendered via `ClickableText`
 * or `BasicText` with a `linkInteractionHandler`; underlined here via [RichSpan.toSpanStyle]).
 * `ParagraphStyle.CODE` is rendered as a monospace [SpanStyle] with a code-block background.
 *
 * [highlights] lays a translucent yellow background over find-in-page match ranges (raw-text
 * coordinates, end-exclusive). They are added as a post-pass `addStyle` over the already-built
 * string, shifted by the marker-prefix length so they align with the raw text in the visual.
 */
@OptIn(ExperimentalTextApi::class)
fun EditorParagraph.toAnnotatedString(
    numberedCounter: Int = 1,
    codeBackground: Color = DefaultCodeBackground,
    highlights: List<HighlightRange> = emptyList(),
    highlightColor: Color = DefaultHighlightColor
): AnnotatedString {
    val base = baseStyleFor(style, codeBackground)
    val marker = marker(numberedCounter)
    val indent = indentPrefix()
    return buildAnnotatedString {
        pushStyle(base)
        if (indent.isNotEmpty()) append(indent)
        if (marker.isNotEmpty()) append(marker)
        for (span in spans) {
            val url = span.url
            if (url != null) {
                // UrlAnnotation is honored by ClickableText / BasicText(linkInteractionHandler);
                // in the editor BasicTextField the underline (above) is the visible cue.
                pushUrlAnnotation(UrlAnnotation(url))
                pushStyle(span.toSpanStyle())
                append(span.text)
                pop()
                pop()
            } else {
                pushStyle(span.toSpanStyle())
                append(span.text)
                pop()
            }
        }
        pop()
        // Find-in-page highlight overlay: raw-text [start, end) → transformed [start+prefixLen, end+prefixLen).
        if (highlights.isNotEmpty()) {
            val prefixLen = indent.length + marker.length
            val textLen = this@toAnnotatedString.text.length
            for (h in highlights) {
                val s = (h.start + prefixLen).coerceIn(prefixLen, prefixLen + textLen)
                val e = (h.end + prefixLen).coerceIn(s, prefixLen + textLen)
                if (e > s) addStyle(SpanStyle(background = highlightColor), s, e)
            }
        }
    }
}
