package io.github.nknote.ui.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepositoryImpl
import io.github.nknote.model.RichDocument
import io.github.nknote.model.RichParagraph
import io.github.nknote.model.RichSpan
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the MVVM split (todo 6): observable/document/meta state flows through
 * [EditorViewModel.uiState] (a [kotlinx.coroutines.flow.StateFlow] of [EditorUiState]), while the
 * real-time editing buffer ([EditorViewModel.fields] / focusedIndex / pendingFocusIndex) stays as
 * `mutableStateOf` / `mutableStateListOf` for keystroke latency. Also verifies the SavedStateHandle
 * draft restores the document + per-paragraph selection (text, start, end) tuples.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorViewModelStateTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var db: NkNoteDatabase
    private lateinit var repo: NoteRepositoryImpl
    private val imageStore = NoopImageStore()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NkNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepositoryImpl(db.noteDao(), db.tagDao(), db.noteTagDao(), imageStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun uiState_reflectsMetaEdits() = runBlocking {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        // A brand-new editor has loaded=true synchronously and a blank observable state.
        assertTrue(vm.loaded)
        assertEquals("", vm.uiState.value.title)

        vm.updateTitle("Hello")
        vm.updateExcerpt("an excerpt")
        vm.updateWeather("sunny")
        vm.updateMood("happy")
        vm.updateDate("2024-06-26")

        val st = vm.uiState.value
        assertEquals("Hello", st.title)
        assertEquals("an excerpt", st.excerpt)
        assertEquals("sunny", st.weatherKey)
        assertEquals("happy", st.moodKey)
        assertEquals("2024-06-26", st.date)
    }

    @Test
    fun uiState_wordCount_reflectsEdits() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())

        assertEquals(0, vm.uiState.value.wordCount)

        vm.onTextChange(0, TextFieldValue("one two three"))

        assertEquals(3, vm.uiState.value.wordCount)
    }

    @Test
    fun editingBuffer_remainsSnapshotStateListOf() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        // The editing buffer must stay a mutableStateListOf<TextFieldValue> (keystroke latency).
        // After construction there is exactly one blank paragraph field.
        assertEquals(1, vm.fields.size)
        assertEquals("", vm.fields[0].rawText)
        assertEquals(0, vm.focusedIndex)
        assertEquals(-1, vm.pendingFocusIndex)
    }

    @Test
    fun tags_flowThroughUiState() {
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        vm.addTag("gratitude")
        vm.addTag("work")
        vm.addTag("gratitude") // duplicate ignored

        assertEquals(listOf("gratitude", "work"), vm.uiState.value.tagNames)

        vm.removeTag("gratitude")
        assertEquals(listOf("work"), vm.uiState.value.tagNames)
    }

    @Test
    fun savedStateHandle_restore_reconstructsDocumentAndSelection() {
        val handle = SavedStateHandle()
        val doc = RichDocument(listOf(RichParagraph(spans = listOf(RichSpan("hello")))))
        handle["draftDoc"] = json.encodeToString(RichDocument.serializer(), doc)
        handle["draftTitle"] = "Draft title"
        handle["draftExcerpt"] = "draft excerpt"
        handle["draftWeather"] = "rainy"
        handle["draftSelections"] = json.encodeToString(ListSerializer(DraftSelection.serializer()), listOf(DraftSelection("hello", 5, 5)))

        val vm = EditorViewModel(null, imageStore, repo, handle)

        // Observable / meta state restored from the draft.
        val st = vm.uiState.value
        assertEquals("Draft title", st.title)
        assertEquals("draft excerpt", st.excerpt)
        assertEquals("rainy", st.weatherKey)

        // Editing buffer reconstructed: one paragraph with text "hello" and selection (5, 5).
        assertEquals(1, vm.paragraphs.size)
        assertEquals("hello", vm.paragraphs.first().text)
        assertEquals(1, vm.fields.size)
        val field = vm.fields.first()
        assertEquals("hello", field.rawText)
        assertEquals(5, field.rawSelection.start)
        assertEquals(5, field.rawSelection.end)

        // wordCount was recomputed from the restored document.
        assertEquals(1, st.wordCount)
    }

    @Test
    fun savedStateHandle_persistsAfterEdits() {
        val handle = SavedStateHandle()
        val vm = EditorViewModel(null, imageStore, repo, handle)

        vm.updateTitle("Persisted")
        vm.onTextChange(0, TextFieldValue("alpha beta"))

        // The handle should now carry the title and the serialized document + selection tuples.
        assertEquals("Persisted", handle.get<String>("draftTitle"))
        val draftDoc = handle.get<String>("draftDoc")
        assertTrue("draftDoc must be populated", draftDoc!!.isNotEmpty())
        val sels = json.decodeFromString<List<DraftSelection>>(handle.get<String>("draftSelections")!!)
        assertEquals(1, sels.size)
        assertEquals("alpha beta", sels[0].text)
    }

    @Test
    fun buildShareIntentIsNotApplicableHere() {
        // Sanity: editor VM exposes StateFlow<EditorUiState> (compile-time guarantee).
        val vm = EditorViewModel(null, imageStore, repo, SavedStateHandle())
        assertNull(vm.uiState.value.styleAtCursor)
        assertEquals(false, vm.uiState.value.canUndo)
        assertEquals(false, vm.uiState.value.canRedo)
    }

    /** No-op ImageStore stand-in — the editor state tests never touch disk images. */
    private class NoopImageStore : ImageStore {
        override fun saveForNote(noteId: Int, source: Uri): String? = "/tmp/test-$noteId.webp"
        override fun delete(path: String) {}
        override fun deleteAllForNote(noteId: Int) {}
        override fun exists(path: String): Boolean = false
        override fun relocateToNote(path: String, noteId: Int): String = path
    }
}