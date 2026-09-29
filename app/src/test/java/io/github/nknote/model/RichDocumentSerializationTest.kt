package io.github.nknote.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM round-trip + legacy-decode tests for the v2 `RichDocument` schema
 * (todo 4 of `.omo/plans/nknote-polish.md`).
 *
 * The serializer mirrors the production config in `EditorViewModel` / `HomeViewModel`:
 *   `Json { ignoreUnknownKeys = true; encodeDefaults = true }`
 *
 * Coverage:
 *  - `roundTripsAllNewFields` — encodes a document exercising every new field
 *    (alignment=CENTER, indentLevel=2, ParagraphStyle.CODE, span.url="https://x") and asserts
 *    decode returns an equal document.
 *  - `legacyJsonWithoutNewFieldsDecodesToDefaults` — a hand-written JSON string WITHOUT any of
 *    the v2 fields (alignment, indentLevel, url, CODE) decodes without throwing and the v2
 *    fields fall back to their defaults (alignment=START, indentLevel=0, url=null).
 *  - `roundTripIsDeterministicAcrossRepeatedRuns` — runs the round-trip assertion in a loop to
 *    rule out flaky / stateful serializer behavior.
 */
class RichDocumentSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTripsAllNewFields() {
        val doc = RichDocument(
            paragraphs = listOf(
                RichParagraph(
                    spans = listOf(RichSpan("hello", url = "https://x")),
                    style = ParagraphStyle.CODE,
                    alignment = ParagraphAlignment.CENTER,
                    indentLevel = 2
                ),
                RichParagraph(
                    spans = listOf(RichSpan(" world")),
                    style = ParagraphStyle.BULLET,
                    alignment = ParagraphAlignment.END,
                    indentLevel = 3
                ),
                RichParagraph(
                    spans = listOf(RichSpan("title")),
                    style = ParagraphStyle.TITLE
                )
            )
        )
        val s = json.encodeToString(RichDocument.serializer(), doc)
        val back = json.decodeFromString(RichDocument.serializer(), s)
        assertEquals(doc, back)
    }

    @Test
    fun legacyJsonWithoutNewFieldsDecodesToDefaults() {
        // Hand-written fixture matching the v1 schema: no `alignment`, no `indentLevel`,
        // no `url` on spans, no `CODE` style. Must decode without throwing.
        val legacy = """
            {
              "paragraphs": [
                {
                  "spans": [{"text":"old","bold":true}],
                  "style": "BODY",
                  "image": null
                }
              ]
            }
        """.trimIndent()
        val doc = json.decodeFromString(RichDocument.serializer(), legacy)
        assertEquals(1, doc.paragraphs.size)
        val p = doc.paragraphs[0]
        assertEquals(ParagraphStyle.BODY, p.style)
        assertEquals(ParagraphAlignment.START, p.alignment)
        assertEquals(0, p.indentLevel)
        assertEquals(1, p.spans.size)
        assertEquals("old", p.spans[0].text)
        assertTrue(p.spans[0].bold)
        assertNull(p.spans[0].url)
    }

    @Test
    fun v3Checkbox_roundTrips() {
        val doc = RichDocument(
            paragraphs = listOf(
                RichParagraph(
                    spans = listOf(RichSpan("buy milk")),
                    style = ParagraphStyle.CHECKBOX,
                    checked = true
                ),
                RichParagraph(
                    spans = listOf(RichSpan("walk dog")),
                    style = ParagraphStyle.CHECKBOX
                )
            )
        )
        val s = json.encodeToString(RichDocument.serializer(), doc)
        val back = json.decodeFromString(RichDocument.serializer(), s)
        assertEquals(doc, back)
        assertTrue(back.paragraphs[0].checked)
        assertEquals(false, back.paragraphs[1].checked)
    }

    @Test
    fun legacyJsonWithoutChecked_decodesToFalse() {
        val legacy = """
            {
              "paragraphs": [
                {"spans":[{"text":"old"}],"style":"BULLET"}
              ]
            }
        """.trimIndent()
        val doc = json.decodeFromString(RichDocument.serializer(), legacy)
        assertEquals(false, doc.paragraphs[0].checked)
    }

    @Test
    fun roundTripIsDeterministicAcrossRepeatedRuns() {
        val doc = RichDocument(
            paragraphs = listOf(
                RichParagraph(
                    spans = listOf(RichSpan("hello", url = "https://x")),
                    style = ParagraphStyle.CODE,
                    alignment = ParagraphAlignment.CENTER,
                    indentLevel = 2
                )
            )
        )
        repeat(5) {
            val s = json.encodeToString(RichDocument.serializer(), doc)
            val back = json.decodeFromString(RichDocument.serializer(), s)
            assertEquals("round-trip iteration $it", doc, back)
        }
    }
}
