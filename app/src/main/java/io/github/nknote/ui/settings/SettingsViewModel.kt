package io.github.nknote.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nknote.data.backup.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Owns the export flow (previously inlined in the SettingsPage composable). The zip is written
 * to `getExternalFilesDir()/nknote-backup-<ts>.zip`; the page fires the share chooser on
 * [ExportState.Done].
 */
class SettingsViewModel(
    application: Application,
    private val backupManager: BackupManager
) : AndroidViewModel(application) {

    sealed interface ExportState {
        data object Idle : ExportState
        data object Running : ExportState
        data class Done(val file: File) : ExportState
        data object Failed : ExportState
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun export() {
        if (_exportState.value is ExportState.Running) return
        _exportState.value = ExportState.Running
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = getApplication<Application>().getExternalFilesDir(null)
                        ?: error("external files dir unavailable")
                    val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    val file = File(dir, "nknote-backup-$ts.zip")
                    file.outputStream().use { backupManager.exportToZip(it) }
                    file
                }
            }
            _exportState.value = outcome.fold(
                onSuccess = { ExportState.Done(it) },
                onFailure = { ExportState.Failed }
            )
        }
    }

    /** Called after the page has consumed a Done/Failed result (e.g. fired the share sheet). */
    fun consumeResult() {
        if (_exportState.value !is ExportState.Running) _exportState.value = ExportState.Idle
    }
}
