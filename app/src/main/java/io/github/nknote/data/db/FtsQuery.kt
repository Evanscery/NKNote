package io.github.nknote.data.db

/**
 * Sanitizes raw user input into an FTS4 `MATCH` expression.
 *
 * Each whitespace-separated token becomes a quoted prefix phrase — `"tok*"` with any
 * embedded quotes doubled — so FTS operators typed by the user (`-`, `OR`, `NEAR`, `:`,
 * parentheses) are neutralized instead of being interpreted. Tokens are implicitly ANDed
 * by FTS4. Blank input maps to `""`, which [NoteDao.search] already guards against
 * (a bare `MATCH ''` would throw).
 *
 * Known limitation: FTS4's default `simple` tokenizer does not segment CJK text, so a
 * run of Chinese characters matches only as a prefix from the start of an indexed token.
 * This is no worse than the previous in-memory substring scan for latin text and is the
 * accepted trade-off of DB-side search; switching to the `icu` tokenizer would require
 * rebuilding the FTS table.
 */
object FtsQuery {
    fun sanitize(raw: String): String =
        raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token -> "\"" + token.replace("\"", "\"\"") + "*\"" }
}
