package io.github.nknote.ui.editor.richtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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

private fun RichSpan.toSpanStyle(): SpanStyle = SpanStyle(
    fontWeight = if (bold) FontWeight.Bold else null,
    fontStyle = if (italic) FontStyle.Italic else null,
    textDecoration = when {
        underline && strikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        underline -> TextDecoration.Underline
        strikethrough -> TextDecoration.LineThrough
        else -> null
    },
    color = parseHexColor(color) ?: Color.Unspecified,
    fontSize = if (fontSizeScale != 1f) fontSizeScale.em else TextUnit.Unspecified
)

private fun baseStyleFor(style: ParagraphStyle): SpanStyle = when (style) {
    ParagraphStyle.TITLE -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp)
    ParagraphStyle.HEADING -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
    ParagraphStyle.SUBHEADING -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    ParagraphStyle.QUOTE -> SpanStyle(fontStyle = FontStyle.Italic)
    ParagraphStyle.BULLET, ParagraphStyle.NUMBERED -> SpanStyle()
    ParagraphStyle.BODY -> SpanStyle()
}

fun EditorParagraph.toAnnotatedString(): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    val base = baseStyleFor(style)
    return buildAnnotatedString {
        pushStyle(base)
        for (span in spans) {
            pushStyle(span.toSpanStyle())
            append(span.text)
            pop()
        }
        pop()
    }
}

/** Prefix shown for list/quote paragraphs. */
fun EditorParagraph.marker(): String = when (style) {
    ParagraphStyle.BULLET -> "•  "
    ParagraphStyle.NUMBERED -> "1. "
    ParagraphStyle.QUOTE -> "“  "
    else -> ""
}