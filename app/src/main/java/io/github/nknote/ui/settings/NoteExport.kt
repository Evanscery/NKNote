package io.github.nknote.ui.settings

import io.github.nknote.data.entity.Note
import kotlinx.serialization.Serializable

/**
 * Serializable view of [Note] for the JSON export. Room entities are not `@Serializable`, so we
 * map into this DTO before encoding. Only the user-facing fields are exported; internal bookkeeping
 * (`isDeleted`, `deletedAt`, `version`, `syncStatus`) is omitted.
 */
@Serializable
data class NoteExport(
    val id: Int,
    val title: String,
    val excerpt: String,
    val content: String,
    val date: String,
    val weather: String,
    val mood: String,
    val coverImagePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun from(note: Note) = NoteExport(
            id = note.id,
            title = note.title,
            excerpt = note.excerpt,
            content = note.content,
            date = note.date,
            weather = note.weather,
            mood = note.mood,
            coverImagePath = note.coverImagePath,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt
        )
    }
}
