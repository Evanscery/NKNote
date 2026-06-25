package io.github.nknote.ui.editor.richtext

import io.github.nknote.model.InlineImage
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichParagraph
import io.github.nknote.model.RichSpan

/**
 * Editor-side mutable model. Spans are a *partition* of the paragraph text: non-overlapping,
 * contiguous, and each span carries the COMPLETE style for its run. This makes style toggling
 * a simple per-character transform (see [toggleSpan] / [setSpan]).
 */
data class EditorDocument(val paragraphs: List<EditorParagraph> = listOf(EditorParagraph())) {
    val size: Int get() = paragraphs.size
    fun isEmpty(): Boolean = paragraphs.all { it.text.isEmpty() && it.image == null }
}

data class EditorParagraph(
    val style: ParagraphStyle = ParagraphStyle.BODY,
    val text: String = "",
    val spans: List<RichSpan> = emptyList(),   // partition; spans[i].text concatenates to [text]
    val image: InlineImage? = null
)

/** Build the display AnnotatedString's character coverage from spans. Returns one style per char. */
private fun EditorParagraph.perCharStyles(): List<RichSpan> {
    if (text.isEmpty()) return emptyList()
    val out = ArrayList<RichSpan>(text.length)
    var idx = 0
    val fallback = RichSpan("")
    for (span in spans) {
        val s = span.text
        for (i in s.indices) {
            out.add(if (idx >= text.length) fallback else span.copy(text = s[i].toString()))
            idx++
        }
    }
    while (idx < text.length) { out.add(fallback.copy(text = text[idx].toString())); idx++ }
    return out
}

/** Re-partition a per-char style list back into contiguous merged spans. */
private fun repartition(chars: List<RichSpan>): List<RichSpan> {
    if (chars.isEmpty()) return emptyList()
    val out = ArrayList<RichSpan>()
    var cur = chars[0].copy(text = chars[0].text)
    for (i in 1 until chars.size) {
        val c = chars[i]
        if (sameStyle(cur, c)) {
            cur = cur.copy(text = cur.text + c.text)
        } else {
            out.add(cur); cur = c.copy(text = c.text)
        }
    }
    out.add(cur)
    return out
}

private fun sameStyle(a: RichSpan, b: RichSpan): Boolean =
    a.bold == b.bold && a.italic == b.italic && a.underline == b.underline &&
        a.strikethrough == b.strikethrough && a.color == b.color && a.fontSizeScale == b.fontSizeScale

/** Apply [transform] to every char in [start, end), then re-merge. */
fun EditorParagraph.withSpanToggled(start: Int, end: Int, transform: (RichSpan) -> RichSpan): EditorParagraph {
    if (text.isEmpty() || start >= end) return this
    val s = start.coerceIn(0, text.length)
    val e = end.coerceIn(0, text.length)
    if (s >= e) return this
    val chars = perCharStyles().toMutableList()
    for (i in s until e) chars[i] = transform(chars[i])
    return copy(spans = repartition(chars))
}

fun EditorParagraph.toggleBold(s: Int, e: Int) = withSpanToggled(s, e) { it.copy(bold = !it.bold) }
fun EditorParagraph.toggleItalic(s: Int, e: Int) = withSpanToggled(s, e) { it.copy(italic = !it.italic) }
fun EditorParagraph.toggleUnderline(s: Int, e: Int) = withSpanToggled(s, e) { it.copy(underline = !it.underline) }
fun EditorParagraph.toggleStrikethrough(s: Int, e: Int) = withSpanToggled(s, e) { it.copy(strikethrough = !it.strikethrough) }
fun EditorParagraph.setColor(s: Int, e: Int, color: String) = withSpanToggled(s, e) { it.copy(color = color) }
fun EditorParagraph.setFontSizeScale(s: Int, e: Int, scale: Float) = withSpanToggled(s, e) { it.copy(fontSizeScale = scale) }

/**
 * Re-derive spans after the paragraph text was edited (single insert/delete from keyboard).
 * Uses common-prefix / common-suffix to find the edited region, shifts surviving chars,
 * and lets newly-inserted chars inherit the style of the char immediately before the edit.
 * This is the canonical rich-text range-adjustment and keeps inline formatting stable while typing.
 */
fun EditorParagraph.afterTextChange(newText: String): EditorParagraph {
    val oldText = this.text
    if (oldText == newText) return this
    val oldLen = oldText.length
    val newLen = newText.length
    var p = 0
    while (p < oldLen && p < newLen && oldText[p] == newText[p]) p++
    var s = 0
    while (s < (oldLen - p) && s < (newLen - p) && oldText[oldLen - 1 - s] == newText[newLen - 1 - s]) s++
    val oldEditStart = p
    val oldEditEnd = oldLen - s
    val insertedLen = (newLen - s) - p
    val oldChars = perCharStyles() // length oldLen
    val newChars = ArrayList<RichSpan>(newLen)
    for (i in 0 until p) newChars.add(oldChars[i])
    val inherit = if (p > 0) oldChars[p - 1].copy(text = "") else RichSpan("")
    for (i in 0 until insertedLen) newChars.add(inherit.copy(text = newText[p + i].toString()))
    for (i in oldEditEnd until oldLen) newChars.add(oldChars[i])
    return copy(text = newText, spans = repartition(newChars))
}

/** Split a paragraph at [cursor] into (before, after). Used on Enter. */
fun EditorParagraph.splitAt(cursor: Int): Pair<EditorParagraph, EditorParagraph> {
    val c = cursor.coerceIn(0, text.length)
    val beforeText = text.substring(0, c)
    val afterText = text.substring(c)
    val chars = perCharStyles()
    val before = repartition(chars.subList(0, c))
    val after = repartition(chars.subList(c, text.length))
    val afterStyle = when (style) { ParagraphStyle.TITLE, ParagraphStyle.HEADING, ParagraphStyle.SUBHEADING -> ParagraphStyle.BODY; else -> style }
    return copy(text = beforeText, spans = before) to
        copy(text = afterText, spans = after, style = afterStyle)
}

/** Merge [other] (next paragraph) into the end of this paragraph. Used on backspace at start. */
fun EditorParagraph.merge(other: EditorParagraph): EditorParagraph {
    val a = this.perCharStyles()
    val b = other.perCharStyles()
    val all = ArrayList<RichSpan>(a.size + b.size).apply { addAll(a); addAll(b) }
    return copy(text = text + other.text, spans = repartition(all))
}

// ── Document <-> model conversion ──────────────────────────────────────────────

fun EditorDocument.toModel(): RichDocument =
    RichDocument(paragraphs.map { p ->
        RichParagraph(
            spans = if (p.text.isEmpty()) emptyList() else p.spans.map { it.copy(text = it.text) }.let { ensureCovers(it, p.text) },
            style = p.style,
            image = p.image
        )
    })

private fun ensureCovers(spans: List<RichSpan>, text: String): List<RichSpan> {
    val total = spans.sumOf { it.text.length }
    if (total == text.length) return spans
    // If spans don't cover text (e.g., freshly typed), rebuild a single plain span.
    return listOf(RichSpan(text))
}

fun RichDocument.toEditor(): EditorDocument =
    EditorDocument(paragraphs.map { p ->
        EditorParagraph(style = p.style, text = p.spans.joinToString("") { it.text }, spans = p.spans, image = p.image)
    })