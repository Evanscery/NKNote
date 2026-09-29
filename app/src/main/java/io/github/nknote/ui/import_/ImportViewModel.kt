package io.github.nknote.ui.import_

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.backup.BackupManager
import io.github.nknote.data.backup.ImportSummary
import io.github.nknote.data.importer.TextImporter
import io.github.nknote.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs the Import screen. Routes the picked file by extension: `.zip` → the full backup
 * importer ([BackupManager.importFromZip], notes + tags + images), `.json` → the legacy flat
 * export ([BackupManager.importLegacyJson]), anything else → the plain-text [TextImporter].
 *
 * The screen resolves the localized status strings from [Result]; the VM stays
 * Android-resource-free so it is unit-testable without string resources.
 */
class ImportViewModel(
    application: Application,
    private val repo: NoteRepository,
    private val backupManager: BackupManager
) : AndroidViewModel(application) {

    sealed interface Result {
        data object Idle : Result
        data object Busy : Result
        data class Success(val imported: Int) : Result
        data class Failed(val message: String) : Result
    }

    private val _result = MutableStateFlow<Result>(Result.Idle)
    val result: StateFlow<Result> = _result.asStateFlow()

    /** Import the file at [fileUri]. Emits [Result.Busy] then [Result.Success] / [Result.Failed]. */
    fun import(fileUri: Uri) {
        _result.value = Result.Busy
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val app = getApplication<Application>()
                    val name = runCatching {
                        app.contentResolver.query(fileUri, null, null, null, null)?.use { c ->
                            if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
                        } ?: "imported.txt"
                    }.getOrDefault("imported.txt")

                    when {
                        name.endsWith(".zip", ignoreCase = true) -> {
                            val summary = backupManager.importFromZip { app.contentResolver.openInputStream(fileUri) }
                            summary.requireAnySuccess()
                        }
                        name.endsWith(".json", ignoreCase = true) -> {
                            val stream = app.contentResolver.openInputStream(fileUri) ?: error("Cannot read file")
                            val summary = stream.use { backupManager.importLegacyJson(it) }
                            summary.requireAnySuccess()
                        }
                        else -> {
                            val text = app.contentResolver.openInputStream(fileUri)
                                ?.use { it.readBytes().decodeToString() }
                                ?: error("Cannot read file")
                            val note = TextImporter.parse(name, text)
                            repo.insertNote(note)
                            1
                        }
                    }
                }
            }
            _result.value = outcome.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failed(it.message ?: "error") }
            )
        }
    }

    private fun ImportSummary.requireAnySuccess(): Int {
        if (imported == 0) error("no notes imported (failed: $failed)")
        return imported
    }

    fun reset() { _result.value = Result.Idle }
}
