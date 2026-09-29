package io.github.nknote.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM tests for the FTS4 MATCH sanitizer. */
class FtsQueryTest {

    @Test
    fun singleToken_becomesQuotedPrefixPhrase() {
        assertEquals("\"diary*\"", FtsQuery.sanitize("diary"))
    }

    @Test
    fun multipleTokens_areEachQuoted_andSpaceJoined() {
        assertEquals("\"team*\" \"standup*\"", FtsQuery.sanitize("team standup"))
    }

    @Test
    fun blankInput_mapsToEmptyString() {
        assertEquals("", FtsQuery.sanitize(""))
        assertEquals("", FtsQuery.sanitize("   "))
        assertEquals("", FtsQuery.sanitize("\t\n"))
    }

    @Test
    fun ftsOperators_areNeutralizedInsideQuotes() {
        assertEquals("\"-secret*\"", FtsQuery.sanitize("-secret"))
        assertEquals("\"a*\" \"OR*\" \"b*\"", FtsQuery.sanitize("a OR b"))
        assertEquals("\"NEAR*\"", FtsQuery.sanitize("NEAR"))
        assertEquals("\"title:x*\"", FtsQuery.sanitize("title:x"))
        assertEquals("\"(group)*\"", FtsQuery.sanitize("(group)"))
    }

    @Test
    fun embeddedQuotes_areDoubled() {
        assertEquals("\"say\"\"hi\"\"*\"", FtsQuery.sanitize("say\"hi\""))
    }

    @Test
    fun cjkInput_isKeptAsOnePrefixToken() {
        assertEquals("\"今天天气*\"", FtsQuery.sanitize("今天天气"))
    }

    @Test
    fun extraWhitespace_isCollapsed() {
        assertEquals("\"a*\" \"b*\"", FtsQuery.sanitize("  a   b  "))
    }
}
