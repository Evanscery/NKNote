package io.github.nknote.data.backup

import kotlinx.serialization.Serializable

/**
 * READ-ONLY legacy format: the flat `List<NoteExport>` JSON produced by the pre-zip export.
 * Kept only so old export files remain importable ([BackupManager.importLegacyJson]); the
 * writer side was replaced by the zip backup. Image paths in this format are absolute and
 * break on reinstall — imported as-is (known historical limitation).
 */
@Serializable
data class LegacyNoteExport(
    val id: Int = 0,
    val title: String = "",
    val excerpt: String = "",
    val content: String = "",
    val date: String = "",
    val weather: String = "",
    val mood: String = "",
    val coverImagePath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
