package io.github.nknote.data.backup

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepositoryImpl
import io.github.nknote.model.InlineImage
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichParagraph
import io.github.nknote.model.RichSpan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Full round-trip: seed notes (with a real image file + inline path + cover + tags) → export
 * zip → wipe → import → everything back with new ids, image files restored under the new note
 * folder, RichDocument paths rewritten to existing files, tags reattached.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRoundTripTest {

    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl
    private lateinit var context: android.content.Context
    private lateinit var manager: BackupManager
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = null
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NkNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepositoryImpl(db.noteDao(), db.tagDao(), db.noteTagDao(), NoopImageStore)
        manager = BackupManager(context, repo)
    }

    @After
    fun tearDown() {
        db.close()
        File(context.filesDir, "images").deleteRecursively()
    }

    private fun docWithImage(text: String, imagePath: String?): String {
        val paragraphs = buildList {
            add(RichParagraph(spans = listOf(RichSpan(text))))
            if (imagePath != null) add(RichParagraph(image = InlineImage(imagePath, 100, 80)))
        }
        return json.encodeToString(RichDocument.serializer(), RichDocument(paragraphs))
    }

    @Test
    fun exportImport_roundTrips_notesImagesAndTags() = runBlocking {
        // Seed: note 1 with a real image file (inline + cover) and a tag; note 2 plain.
        val imgDir = File(context.filesDir, "images/1").apply { mkdirs() }
        val imgFile = File(imgDir, "photo.webp").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

        val id1 = repo.insertNote(
            Note(
                title = "with image", excerpt = "e1", content = docWithImage("hello", imgFile.absolutePath),
                date = "2026-07-27", monthDay = "07-27", weather = "sunny", mood = "happy",
                coverImagePath = imgFile.absolutePath, createdAt = 111, updatedAt = 222
            )
        ).toInt()
        repo.insertNote(
            Note(
                title = "plain", excerpt = "e2", content = docWithImage("world", null),
                date = "2026-07-26", monthDay = "07-26", createdAt = 333, updatedAt = 444
            )
        )
        repo.upsertTag(Tag(Tag.idFor("travel"), "travel", Tag.colorFor("travel"), 0))
        repo.setNoteTags(id1, listOf(Tag.idFor("travel")))

        // Export.
        val zipBytes = ByteArrayOutputStream().also { manager.exportToZip(it) }.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        // Wipe DB + files.
        repo.observeAllNotes().first().forEach { repo.permanentlyDelete(it.id) }
        File(context.filesDir, "images").deleteRecursively()
        assertTrue(repo.observeAllNotes().first().isEmpty())

        // Import.
        val summary = manager.importFromZip { ByteArrayInputStream(zipBytes) }
        assertEquals(ImportSummary(2, 0), summary)

        val notes = repo.observeAllNotes().first()
        assertEquals(2, notes.size)
        val withImage = notes.first { it.title == "with image" }
        val plain = notes.first { it.title == "plain" }
        assertEquals("e1", withImage.excerpt)
        assertEquals("sunny", withImage.weather)
        assertEquals(111, withImage.createdAt)
        assertEquals("world", json.decodeFromString(RichDocument.serializer(), plain.content).plainText())

        // The image file was restored under the NEW note folder and both paths were rewritten.
        val newCover = withImage.coverImagePath
        assertTrue("cover must point at the new folder", newCover!!.contains("images/${withImage.id}") || newCover.contains("images\\${withImage.id}"))
        assertTrue("cover file must exist on disk", File(newCover).exists())
        val doc = json.decodeFromString(RichDocument.serializer(), withImage.content)
        val inlinePath = doc.paragraphs.firstNotNullOf { it.image }.path
        assertTrue("inline image path must be absolute + existing", File(inlinePath).exists())

        // Tags reattached.
        val tags = repo.observeTagsForNote(withImage.id).first()
        assertEquals(listOf("travel"), tags.map { it.name })
    }

    @Test
    fun import_rejectsZipSlipEntries() = runBlocking {
        val evil = ByteArrayOutputStream().use { bos ->
            ZipOutputStream(bos).use { zip ->
                zip.putNextEntry(ZipEntry(BackupManager.MANIFEST_NAME))
                zip.write(
                    json.encodeToString(BackupFile.serializer(), BackupFile(notes = emptyList()))
                        .toByteArray()
                )
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("images/../../evil.txt"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
            bos.toByteArray()
        }
        // The traversal entry must not escape; the import fails safely instead.
        val summary = manager.importFromZip { ByteArrayInputStream(evil) }
        assertEquals(0, summary.imported)
        assertTrue("no file may escape the staging dir", !File(context.cacheDir.parentFile, "evil.txt").exists())
    }

    @Test
    fun import_rejectsNewerSchemaVersion() = runBlocking {
        val future = ByteArrayOutputStream().use { bos ->
            ZipOutputStream(bos).use { zip ->
                zip.putNextEntry(ZipEntry(BackupManager.MANIFEST_NAME))
                zip.write(
                    json.encodeToString(
                        BackupFile.serializer(),
                        BackupFile(schemaVersion = 99, notes = listOf(BackupNote(id = 1, title = "x")))
                    ).toByteArray()
                )
                zip.closeEntry()
            }
            bos.toByteArray()
        }
        val summary = manager.importFromZip { ByteArrayInputStream(future) }
        assertEquals(0, summary.imported)
        assertTrue(repo.observeAllNotes().first().isEmpty())
    }

    @Test
    fun legacyJson_importsNotes() = runBlocking {
        val legacy = """
            [
              {"id":1,"title":"old note","excerpt":"","content":"{\"paragraphs\":[]}",
               "date":"2024-01-01","weather":"","mood":"","coverImagePath":null,
               "createdAt":5,"updatedAt":6}
            ]
        """.trimIndent()
        val summary = manager.importLegacyJson(ByteArrayInputStream(legacy.toByteArray()))
        assertEquals(ImportSummary(1, 0), summary)
        assertEquals("old note", repo.observeAllNotes().first().single().title)
    }

    @Test
    fun garbageInput_failsSafely() = runBlocking {
        val summary = manager.importFromZip { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        assertEquals(0, summary.imported)
    }
}
