package io.github.nknote.data.backup

import kotlinx.serialization.Serializable

/**
 * The `notes.json` manifest inside an NKNote backup zip.
 *
 * Zip layout (format v1):
 * ```
 * nknote-backup-<yyyyMMdd-HHmmss>.zip
 * ├── notes.json                      (this manifest, UTF-8)
 * └── images/<exportNoteId>/<file>.webp
 * ```
 *
 * Image paths inside [BackupNote.content] (the note's RichDocument JSON) and
 * [BackupNote.coverImagePath] are rewritten RELATIVE (`images/<id>/<file>.webp`) on export and
 * rewritten back to absolute app paths on import — this is what makes a backup restorable on a
 * fresh install, unlike the legacy flat-JSON export whose absolute paths broke on reinstall.
 *
 * [BackupNote.id] is only a folder key inside the zip (export-scope, not a DB promise): the
 * importer always inserts as NEW notes and remaps.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int = 1,
    val exportedAt: Long = 0L,
    val tags: List<BackupTag> = emptyList(),
    val notes: List<BackupNote> = emptyList()
)

@Serializable
data class BackupTag(
    val id: String,
    val name: String,
    val color: String,
    val createdAt: Long = 0L
)

@Serializable
data class BackupNote(
    val id: Int,
    val title: String = "",
    val excerpt: String = "",
    val content: String = "",
    val date: String = "",
    val weather: String = "",
    val mood: String = "",
    val coverImagePath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val tagIds: List<String> = emptyList()
)

/** Import outcome, reported honestly (per-note failures don't abort the whole import). */
data class ImportSummary(val imported: Int, val failed: Int)
