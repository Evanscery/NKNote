package io.github.nknote.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nknote.NkNoteApplication
import io.github.nknote.R
import io.github.nknote.core.ThemeMode
import io.github.nknote.ui.components.NkSettingsRow
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.navigation.LocalDrawerState
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(nav: NkNoteNavigation) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as NkNoteApplication).container
    val themeMode by appContainer.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkOn = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scope = rememberCoroutineScope()
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NkTopAppBar(
                title = stringResource(R.string.nav_settings),
                drawerState = LocalDrawerState.current
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = NkSpacing.lg, vertical = NkSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // ── Data section ──
            SettingsSection(stringResource(R.string.settings_section_data)) {
                NkSettingsRow(
                    icon = Icons.Filled.FileUpload,
                    title = stringResource(R.string.import_title),
                    subtitle = stringResource(R.string.import_hint),
                    onClick = { nav.toImport() }
                )
                NkSettingsRow(
                    icon = Icons.Filled.Description,
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    onClick = {
                        if (exporting) return@NkSettingsRow
                        exporting = true
                        exportStatus = null
                        scope.launch {
                            val result = runCatching { exportNotes(context, appContainer) }
                            exporting = false
                            exportStatus = result.fold(
                                onSuccess = { path -> context.getString(R.string.settings_export_done, path) },
                                onFailure = { context.getString(R.string.settings_export_failed) }
                            )
                        }
                    }
                )
            }
            exportStatus?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = NkSpacing.sm, vertical = NkSpacing.xs)
                )
            }

            // ── Appearance section ──
            // Language row removed: i18n follows the system locale by design (no in-app picker).
            SettingsSection(stringResource(R.string.settings_section_appearance)) {
                NkSettingsRow(
                    icon = Icons.Filled.Brightness6,
                    title = stringResource(R.string.settings_theme_dark),
                    subtitle = stringResource(R.string.settings_theme_system),
                    onClick = { appContainer.setThemeMode(if (!darkOn) ThemeMode.DARK else ThemeMode.LIGHT) },
                    trailing = { Switch(checked = darkOn, onCheckedChange = { appContainer.setThemeMode(if (it) ThemeMode.DARK else ThemeMode.LIGHT) }) }
                )
            }

            // ── About section ──
            SettingsSection(stringResource(R.string.settings_section_about)) {
                NkSettingsRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.settings_about_description),
                    onClick = {}
                )
                NkSettingsRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_about_version),
                    subtitle = "v2.0.0",
                    onClick = {}
                )
            }
            Spacer(Modifier.height(NkSpacing.xl))
            Text(
                text = stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = NkSpacing.sm)
            )
        }
    }
}

/**
 * Write all non-deleted notes to `getExternalFilesDir()/nknote-export-<ts>.json`, then fire a share
 * `Intent.ACTION_SEND` via `FileProvider` (authority wired in the manifest + `res/xml/file_paths.xml`).
 * Returns the absolute file path on success. Runs on [Dispatchers.IO].
 */
private suspend fun exportNotes(
    context: android.content.Context,
    appContainer: io.github.nknote.core.AppContainer
): String = withContext(Dispatchers.IO) {
    val notes = appContainer.noteRepository.observeAllNotes().first()
    val payload = Json.encodeToString(
        ListSerializer(NoteExport.serializer()),
        notes.map { NoteExport.from(it) }
    )
    val dir = context.getExternalFilesDir(null)
        ?: throw IllegalStateException("external files dir unavailable")
    val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    val file = File(dir, "nknote-export-$ts.json")
    file.writeText(payload)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(share, context.getString(R.string.settings_export)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
    file.absolutePath
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = NkSpacing.lg, bottom = NkSpacing.xs, start = NkSpacing.sm)
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NkShapes.smallMedium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}
