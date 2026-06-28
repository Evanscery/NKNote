package io.github.nknote.data.templates

import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichParagraph
import io.github.nknote.model.RichSpan

/**
 * A built-in starter document the user can pre-fill a new note with from the editor overflow menu
 * (todo 12). Kept tiny: only [key], [title], and the [document] body. Picking a template replaces
 * the editor's current document via [io.github.nknote.ui.editor.EditorViewModel.applyTemplate].
 */
data class NoteTemplate(
    val key: String,
    val title: String,
    val document: RichDocument
)

/**
 * Built-in note templates. No external / 3rd-party template engine — these are plain
 * [RichDocument]s hand-authored here, consumed only by the editor's "new note" flow.
 */
object NoteTemplates {

    /** "Gratitude" — a heading + 3 empty bullet points. */
    val Gratitude = NoteTemplate(
        key = "gratitude",
        title = "Gratitude",
        document = RichDocument(
            paragraphs = listOf(
                RichParagraph(
                    style = ParagraphStyle.HEADING,
                    spans = listOf(RichSpan("Three things I'm grateful for today"))
                ),
                RichParagraph(style = ParagraphStyle.BULLET, spans = listOf(RichSpan(""))),
                RichParagraph(style = ParagraphStyle.BULLET, spans = listOf(RichSpan(""))),
                RichParagraph(style = ParagraphStyle.BULLET, spans = listOf(RichSpan("")))
            )
        )
    )

    /** "Daily Log" — a heading + one empty body paragraph. */
    val DailyLog = NoteTemplate(
        key = "daily_log",
        title = "Daily Log",
        document = RichDocument(
            paragraphs = listOf(
                RichParagraph(style = ParagraphStyle.HEADING, spans = listOf(RichSpan("Today"))),
                RichParagraph(spans = listOf(RichSpan("")))
            )
        )
    )

    /** "Free Write" — a single empty body paragraph. */
    val FreeWrite = NoteTemplate(
        key = "free_write",
        title = "Free Write",
        document = RichDocument(
            paragraphs = listOf(RichParagraph(spans = listOf(RichSpan(""))))
        )
    )

    val all: List<NoteTemplate> = listOf(Gratitude, DailyLog, FreeWrite)

    fun byKey(key: String): NoteTemplate? = all.firstOrNull { it.key == key }
}
