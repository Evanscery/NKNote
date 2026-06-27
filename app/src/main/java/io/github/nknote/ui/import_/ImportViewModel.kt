package io.github.nknote.ui.import_

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.importer.TextImporter
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs the Import screen. Reads the picked `.txt` via the Application's [android.content.ContentResolver]
 * (no raw [android.content.Context] is passed in — [AndroidViewModel] holds the Application), parses it
 * with [TextImporter], and inserts the resulting [io.github.nknote.data.entity.Note] through [NoteRepository].
 *
 * The screen resolves the localized status strings from [Result]; the VM stays Android-resource-free
 * so it is unit-testable without string resources.
 */
class ImportViewModel(
    application: Application,
    private val repo: NoteRepository
) : AndroidViewModel(application) {

    sealed interface Result {
        data object Idle : Result
        data object Busy : Result
        data object Success : Result
        data class Failed(val message: String) : Result
    }

    private val _result = MutableStateFlow<Result>(Result.Idle)
    val result: StateFlow<Result> = _result.asStateFlow()

    /** Parse + insert the file at [fileUri]. Emits [Result.Busy] then [Result.Success] / [Result.Failed]. */
    fun import(fileUri: Uri) {
        _result.value = Result.Busy
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val app = getApplication<Application>()
                    val text = app.contentResolver.openInputStream(fileUri)?.use { it.readBytes().decodeToString() }
                        ?: error("Cannot read file")
                    val name = runCatching {
                        app.contentResolver.query(fileUri, null, null, null, null)?.use { c ->
                            if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
                        } ?: "imported.txt"
                    }.getOrDefault("imported.txt")
                    val note = TextImporter.parse(name, text)
                    repo.insertNote(note)
                }
            }
            _result.value = outcome.fold(
                onSuccess = { Result.Success },
                onFailure = { Result.Failed(it.message ?: "error") }
            )
        }
    }

    fun reset() { _result.value = Result.Idle }
}