package io.github.nknote.data.entity

import androidx.room.Entity
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
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val content: String,            // serialized RichDocument JSON
    val date: String,               // "yyyy-MM-dd"
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