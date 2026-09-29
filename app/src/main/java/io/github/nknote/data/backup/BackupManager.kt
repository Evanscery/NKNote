package io.github.nknote.data.backup

import android.content.Context
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.repository.NoteRepository
import io.github.nknote.model.RichDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Full-fidelity backup: notes + tags + image files in one zip (format spec: [BackupFile]).
 * Streaming on both sides — image bytes are `copyTo`-piped, never materialized in memory.
 *
 * Import is always-insert-as-new (approved strategy): note ids are remapped, image files are
 * staged then moved into `filesDir/images/<newId>/`, and every image path inside the note's
 * RichDocument (plus the cover path) is rewritten to the new absolute location. Atomicity is
 * per-note (Room transactions); file operations cannot join a DB transaction, so a mid-import
 * crash can leave already-imported notes plus at most one partially-imaged note — reported
 * honestly via [ImportSummary].
 */
class BackupManager(
    private val context: Context,
    private val repo: NoteRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Export ─────────────────────────────────────────────────────────────────────

    /** Streams a full backup (non-deleted notes) into [out]. */
    suspend fun exportToZip(out: OutputStream): Unit = withContext(Dispatchers.IO) {
        val notes = repo.observeAllNotes().first()
        val tags = repo.observeTags().first()
        val joins = repo.getAllNoteTags()
        val tagsByNote = joins.groupBy({ it.noteId }, { it.tagId })

        // Pass 1: rewrite image paths relative and record which files go into the zip.
        val filesToPack = ArrayList<Pair<File, String>>() // (absolute file, zip entry path)
        val backupNotes = notes.map { note ->
            val doc = runCatching { json.decodeFromString<RichDocument>(note.content) }.getOrNull()
            var content = note.content
            if (doc != null) {
                var changed = false
                val rewritten = doc.copy(paragraphs = doc.paragraphs.map { p ->
                    val img = p.image ?: return@map p
                    val f = File(img.path)
                    if (!f.exists()) return@map p
                    val rel = "images/${note.id}/${f.name}"
                    filesToPack.add(f to rel)
                    changed = true
                    p.copy(image = img.copy(path = rel))
                })
                if (changed) content = json.encodeToString(RichDocument.serializer(), rewritten)
            }
            val cover = note.coverImagePath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    val rel = "images/${note.id}/${f.name}"
                    if (filesToPack.none { it.second == rel }) filesToPack.add(f to rel)
                    rel
                } else null
            }
            BackupNote(
                id = note.id,
                title = note.title, excerpt = note.excerpt, content = content,
                date = note.date, weather = note.weather, mood = note.mood,
                coverImagePath = cover,
                createdAt = note.createdAt, updatedAt = note.updatedAt,
                tagIds = tagsByNote[note.id].orEmpty()
            )
        }

        val manifest = BackupFile(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            tags = tags.map { BackupTag(it.id, it.name, it.color, it.createdAt) },
            notes = backupNotes
        )

        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_NAME))
            zip.write(json.encodeToString(BackupFile.serializer(), manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for ((file, rel) in filesToPack.distinctBy { it.second }) {
                zip.putNextEntry(ZipEntry(rel))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    // ── Import ─────────────────────────────────────────────────────────────────────

    /**
     * Reads a backup zip via [openStream] (SAF uri → stream factory). Rejects manifests newer
     * than this app understands. Zip entries are staged into cacheDir with a zip-slip guard.
     */
    suspend fun importFromZip(openStream: () -> InputStream?): ImportSummary = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "import-${UUID.randomUUID()}")
        try {
            var manifest: BackupFile? = null
            val input = openStream() ?: return@withContext ImportSummary(0, 1)
            ZipInputStream(input.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory) {
                        if (name == MANIFEST_NAME) {
                            manifest = json.decodeFromString(
                                BackupFile.serializer(),
                                zip.readBytes().toString(Charsets.UTF_8)
                            )
                        } else if (name.startsWith("images/")) {
                            // Zip-slip guard: the resolved target must stay inside the staging dir.
                            val target = File(staging, name)
                            if (!target.canonicalPath.startsWith(staging.canonicalPath + File.separator) &&
                                target.canonicalPath != staging.canonicalPath
                            ) {
                                throw SecurityException("zip entry escapes staging dir: $name")
                            }
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val m = manifest ?: return@withContext ImportSummary(0, 1)
            if (m.schemaVersion > 1) return@withContext ImportSummary(0, m.notes.size)

            importManifest(m, staging)
        } catch (_: Exception) {
            ImportSummary(0, 1)
        } finally {
            staging.deleteRecursively()
        }
    }

    private suspend fun importManifest(m: BackupFile, staging: File): ImportSummary {
        val tagById = m.tags.associateBy { it.id }
        var imported = 0
        var failed = 0
        for (dto in m.notes) {
            val ok = runCatching {
                val now = System.currentTimeMillis()
                // 1. Insert (always as a NEW note) with the raw content; the id comes back.
                val newId = repo.insertNote(
                    Note(
                        title = dto.title, excerpt = dto.excerpt, content = dto.content,
                        date = dto.date, weather = dto.weather, mood = dto.mood,
                        coverImagePath = null,
                        createdAt = if (dto.createdAt > 0) dto.createdAt else now,
                        updatedAt = if (dto.updatedAt > 0) dto.updatedAt else now
                    )
                ).toInt()

                // 2. Move this note's staged images into filesDir/images/<newId>/.
                val stagedDir = File(staging, "images/${dto.id}")
                val targetDir = File(context.filesDir, "images/$newId")
                if (stagedDir.isDirectory) {
                    targetDir.mkdirs()
                    stagedDir.listFiles()?.forEach { f ->
                        val dst = File(targetDir, f.name)
                        if (!f.renameTo(dst)) {
                            f.copyTo(dst, overwrite = true)
                            f.delete()
                        }
                    }
                }

                // 3. Rewrite relative image paths (document + cover) to the new absolute home.
                fun absolutize(rel: String?): String? {
                    if (rel == null) return null
                    if (!rel.startsWith("images/")) return rel  // legacy absolute path — keep
                    return File(targetDir, File(rel).name).absolutePath
                }

                val doc = runCatching { json.decodeFromString<RichDocument>(dto.content) }.getOrNull()
                val fixedContent = if (doc != null) {
                    json.encodeToString(
                        RichDocument.serializer(),
                        doc.copy(paragraphs = doc.paragraphs.map { p ->
                            val img = p.image ?: return@map p
                            p.copy(image = img.copy(path = absolutize(img.path) ?: img.path))
                        })
                    )
                } else dto.content

                val inserted = repo.getNote(newId) ?: error("inserted note vanished")
                repo.updateNote(
                    inserted.copy(content = fixedContent, coverImagePath = absolutize(dto.coverImagePath))
                )

                // 4. Reattach tags (upserting any the DB doesn't know yet).
                if (dto.tagIds.isNotEmpty()) {
                    dto.tagIds.forEach { tagId ->
                        if (repo.getTag(tagId) == null) {
                            val t = tagById[tagId]
                            repo.upsertTag(
                                Tag(
                                    id = tagId,
                                    name = t?.name ?: tagId,
                                    color = t?.color ?: Tag.colorFor(tagId),
                                    createdAt = t?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    repo.setNoteTags(newId, dto.tagIds)
                }
            }.isSuccess
            if (ok) imported++ else failed++
        }
        return ImportSummary(imported, failed)
    }

    // ── Legacy flat-JSON import (the pre-zip export format) ────────────────────────

    /** Decodes the old `List<NoteExport>`-shaped flat JSON (no images, no tags). */
    suspend fun importLegacyJson(input: InputStream): ImportSummary = withContext(Dispatchers.IO) {
        runCatching {
            val text = input.bufferedReader().readText()
            val notes = json.decodeFromString(ListSerializer(LegacyNoteExport.serializer()), text)
            var imported = 0
            var failed = 0
            for (dto in notes) {
                val ok = runCatching {
                    val now = System.currentTimeMillis()
                    repo.insertNote(
                        Note(
                            title = dto.title, excerpt = dto.excerpt, content = dto.content,
                            date = dto.date, weather = dto.weather, mood = dto.mood,
                            coverImagePath = dto.coverImagePath,
                            createdAt = if (dto.createdAt > 0) dto.createdAt else now,
                            updatedAt = if (dto.updatedAt > 0) dto.updatedAt else now
                        )
                    )
                }.isSuccess
                if (ok) imported++ else failed++
            }
            ImportSummary(imported, failed)
        }.getOrDefault(ImportSummary(0, 1))
    }

    companion object {
        const val MANIFEST_NAME = "notes.json"
    }
}
