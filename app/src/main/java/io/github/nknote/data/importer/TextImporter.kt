package io.github.nknote.data.importer

import io.github.nknote.data.entity.Note
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichParagraph
import io.github.nknote.model.RichSpan
import kotlinx.serialization.json.Json

/**
 * Parses a plain-text file into a Note. Used by the Import screen.
 * Rules:
 *  - First non-empty line becomes the title.
 *  - A line starting with "# " is a heading paragraph; "> " a quote; "- " a bullet.
 *  - Everything else is a body paragraph.
 */
object TextImporter {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun parse(filename: String, text: String): Note {
        val lines = text.split("\n")
        val paragraphs = mutableListOf<RichParagraph>()
        var title = filename.substringBeforeLast('.', filename).ifBlank { "Untitled" }

        var firstNonEmptySeen = false
        for (raw in lines) {
            val line = raw.removeSuffix("\r")
            if (!firstNonEmptySeen && line.isNotBlank()) {
                val candidate = line.trim().removePrefix("#")
                if (candidate.length <= 80) title = candidate.trim().ifBlank { title }
                firstNonEmptySeen = true
                // keep the line as content too if it's not just a title marker
                if (line.startsWith("#")) continue
            }
            val para = when {
                line.startsWith("# ") -> RichParagraph(spans = listOf(RichSpan(line.removePrefix("# ").trim())), style = ParagraphStyle.HEADING)
                line.startsWith("> ") -> RichParagraph(spans = listOf(RichSpan(line.removePrefix("> ").trim())), style = ParagraphStyle.QUOTE)
                line.startsWith("- ") -> RichParagraph(spans = listOf(RichSpan(line.removePrefix("- ").trim())), style = ParagraphStyle.BULLET)
                line.isBlank() -> RichParagraph(spans = listOf(RichSpan("")))
                else -> RichParagraph(spans = listOf(RichSpan(line)))
            }
            paragraphs.add(para)
        }
        if (paragraphs.isEmpty()) paragraphs.add(RichParagraph(listOf(RichSpan(""))))

        val now = System.currentTimeMillis()
        val today = java.time.LocalDate.now().toString()
        return Note(
            title = title,
            description = paragraphs.firstNotNullOfOrNull { it.spans.firstOrNull { s -> s.text.isNotBlank() } }?.text?.take(80).orEmpty(),
            content = json.encodeToString(RichDocument.serializer(), RichDocument(paragraphs)),
            date = today,
            createdAt = now,
            updatedAt = now
        )
    }
}