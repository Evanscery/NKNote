package io.github.nknote.data.db

import androidx.room.TypeConverter
import io.github.nknote.model.RichDocument
import kotlinx.serialization.json.Json

/**
 * Room [TypeConverter] for [RichDocument] ↔ its serialized JSON form (stored in `notes.content`).
 *
 * Uses a lenient [Json] instance (`ignoreUnknownKeys = true` so older notes decode as the model
 * evolves; `encodeDefaults = true` so new fields persist). This mirrors the [Json] config used
 * in the editor/importer paths, keeping one serialization policy across the app.
 */
class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromRichDocument(document: RichDocument?): String? =
        document?.let { json.encodeToString(RichDocument.serializer(), it) }

    @TypeConverter
    fun toRichDocument(content: String?): RichDocument? =
        content?.let { json.decodeFromString(RichDocument.serializer(), it) }
}
