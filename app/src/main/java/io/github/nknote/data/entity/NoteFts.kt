package io.github.nknote.data.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search index over [Note].
 *
 * Uses `@Fts4(contentEntity = Note::class)` so Room auto-generates sync triggers on the
 * `notes` table 鈥?no manual FTS insert/update/delete is needed. The indexed columns
 * (`searchText`, `title`, `excerpt`) MUST be columns of the content entity [Note];
 * Room maps them by name. [Note.searchText] carries the plain-text rendition of the
 * rich content so FTS does not have to scan JSON markup.
 *
 * See: https://developer.android.com/training/data-storage/room/fts
 */
@Entity(tableName = "note_fts")
@Fts4(contentEntity = Note::class)
data class NoteFts(
    val searchText: String,
    val title: String,
    val excerpt: String
)
