package io.github.nknote.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A diary / note entry.
 *
 * Images are NOT stored as a separate Room table: they live as compressed files on disk
 * (see [io.github.nknote.data.image.ImageStore]) and are referenced inline by path inside
 * [content]. This keeps the DB small and satisfies the "compressed storage" constraint.
 *
 * [version] and [syncStatus] are retained for the serverless sync engine (netdisk / LAN);
 * they are inert until a real [io.github.nknote.data.sync.SyncEngine] is wired.
 *
 * [searchText] is a plain-text rendition of [content] (via [io.github.nknote.model.RichDocument.plainText])
 * denormalized so FTS4 ([io.github.nknote.data.entity.NoteFts]) can index it without deserializing JSON.
 * [monthDay] is `MM-dd` derived from [date] so [io.github.nknote.data.db.NoteDao.observeByMonthDay]
 * no longer uses `substr()`.
 */
@Entity(
    tableName = "notes",
    indices = [Index("isDeleted"), Index("date")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val excerpt: String,
    val content: String,            // serialized RichDocument JSON
    val searchText: String = "",    // plain-text of content, denormalized for FTS
    val date: String,               // "yyyy-MM-dd"
    val monthDay: String = "",      // "MM-dd" derived from date, avoids substr() in queries
    val weather: String = "",      // Weather.key, "" = none
    val mood: String = "",          // Mood.key, "" = none
    val coverImagePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val version: Int = 1,
    val syncStatus: Int = 0         // 0 local-only, 1 pending, 2 synced
)