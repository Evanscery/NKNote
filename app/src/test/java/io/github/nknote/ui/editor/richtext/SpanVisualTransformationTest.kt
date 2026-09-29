package io.github.nknote.ui.editor.richtext

import androidx.compose.ui.text.AnnotatedString
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.RichSpan
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The transformation's two load-bearing behaviors:
 *  1. An equal-length replacement (autocorrect / IME candidate swap) renders the LIVE field
 *     text with the stored span styles applied positionally — never the stale stored text.
 *  2. The OffsetMapping round-trips under the double prefix (sentinel guard + list marker),
 *     clamping so the cursor can never land before the guard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpanVisualTransformationTest {

    private val guard = GUARD_CHAR.toString()

    @Test
    fun equalLengthReplacement_rendersLiveText_notStaleParagraph() {
        val para = EditorParagraph(
            text = "teh cat",
            spans = listOf(RichSpan("teh", bold = true), RichSpan(" cat"))
        )
        val t = SpanVisualTransformation(paragraph = para, guardPrefixLength = 1)
        // Autocorrect swapped "teh" → "the" (same length); the paragraph model is still stale.
        val out = t.filter(AnnotatedString(guard + "the cat"))
        assertEquals("the cat", out.text.text)
    }

    @Test
    fun matchingText_rendersCanonicalStyledString() {
        val para = EditorParagraph(
            text = "item",
            spans = listOf(RichSpan("item")),
            style = ParagraphStyle.BULLET
        )
        val t = SpanVisualTransformation(paragraph = para, guardPrefixLength = 1)
        val out = t.filter(AnnotatedString(guard + "item"))
        assertEquals("•  item", out.text.text)
    }

    @Test
    fun offsetMapping_roundTrips_withGuardAndMarker() {
        val para = EditorParagraph(
            text = "abc",
            spans = listOf(RichSpan("abc")),
            style = ParagraphStyle.BULLET   // marker "•  " = 3 chars
        )
        val t = SpanVisualTransformation(paragraph = para, numberedCounter = 1, guardPrefixLength = 1)
        val out = t.filter(AnnotatedString(guard + "abc"))
        val m = out.offsetMapping

        // original: [0]=guard, [1..4]=abc cursor slots; transformed: "•  abc"
        assertEquals(3, m.originalToTransformed(0))   // before guard → start of raw text visually
        assertEquals(3, m.originalToTransformed(1))   // raw pos 0
        assertEquals(6, m.originalToTransformed(4))   // raw end
        assertEquals(1, m.transformedToOriginal(0))   // tap on the marker → raw 0 (behind guard)
        assertEquals(1, m.transformedToOriginal(3))
        assertEquals(4, m.transformedToOriginal(6))
        // Clamp: a visual offset beyond the text maps to the raw end.
        assertEquals(4, m.transformedToOriginal(99))
    }

    @Test
    fun offsetMapping_identityWhenNoGuardNoMarker() {
        val para = EditorParagraph(text = "abc", spans = listOf(RichSpan("abc")))
        val t = SpanVisualTransformation(paragraph = para, guardPrefixLength = 0)
        val out = t.filter(AnnotatedString("abc"))
        assertEquals(2, out.offsetMapping.originalToTransformed(2))
        assertEquals(2, out.offsetMapping.transformedToOriginal(2))
    }

    @Test
    fun guardlessTransientText_isNotStripped() {
        val para = EditorParagraph(text = "abc", spans = listOf(RichSpan("abc")))
        val t = SpanVisualTransformation(paragraph = para, guardPrefixLength = 1)
        // Transient frame: the IME already deleted the guard; nothing must be eaten.
        val out = t.filter(AnnotatedString("abc"))
        assertEquals("abc", out.text.text)
    }

    @Test
    fun numberedMarker_variableWidth_mapsCorrectly() {
        val para = EditorParagraph(
            text = "x",
            spans = listOf(RichSpan("x")),
            style = ParagraphStyle.NUMBERED
        )
        val t = SpanVisualTransformation(paragraph = para, numberedCounter = 12, guardPrefixLength = 1)
        val out = t.filter(AnnotatedString(guard + "x"))
        assertEquals("12. x", out.text.text)
        assertEquals(4, out.offsetMapping.originalToTransformed(1))  // raw 0 → after "12. "
        assertEquals(1, out.offsetMapping.transformedToOriginal(4))
    }
}
