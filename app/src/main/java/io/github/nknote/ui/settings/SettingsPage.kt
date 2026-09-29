package io.github.nknote.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nknote.AppViewModelFactory
import io.github.nknote.NkNoteApplication
import io.github.nknote.R
import io.github.nknote.core.ThemeMode
import io.github.nknote.ui.components.NkSettingsRow
import io.github.nknote.ui.components.NkSingleChoiceDialog
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.navigation.LocalDrawerState
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkIconSize
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    nav: NkNoteNavigation,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as NkNoteApplication).container
    val themeMode by appContainer.themeMode.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // (text, isError)

    // Version from the package manager — BuildConfig generation is off under AGP 8.5 defaults.
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    val exportDoneTemplate = stringResource(R.string.settings_export_done)
    val exportFailedText = stringResource(R.string.settings_export_failed)
    LaunchedEffect(exportState) {
        when (val st = exportState) {
            is SettingsViewModel.ExportState.Done -> {
                exportStatus = String.format(exportDoneTemplate, st.file.absolutePath) to false
                runCatching {
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", st.file
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(share, context.getString(R.string.settings_export)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                viewModel.consumeResult()
            }
            is SettingsViewModel.ExportState.Failed -> {
                exportStatus = exportFailedText to true
                viewModel.consumeResult()
            }
            else -> Unit
        }
    }

    val systemLabel = stringResource(R.string.settings_theme_system)
    val lightLabel = stringResource(R.string.settings_theme_light)
    val darkLabel = stringResource(R.string.settings_theme_dark)
    val themeLabel: (ThemeMode) -> String = {
        when (it) {
            ThemeMode.SYSTEM -> systemLabel
            ThemeMode.LIGHT -> lightLabel
            ThemeMode.DARK -> darkLabel
        }
    }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NkSpacing.lg, vertical = NkSpacing.sm)
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
                    icon = Icons.Filled.Archive,
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    onClick = { viewModel.export() },
                    trailing = {
                        if (exportState is SettingsViewModel.ExportState.Running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(NkIconSize.md),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }
            if (exportState is SettingsViewModel.ExportState.Running) {
                StatusLine(stringResource(R.string.settings_export_progress), isError = false)
            }
            exportStatus?.let { (text, isError) -> StatusLine(text, isError) }

            // ── Appearance section ──
            // Language row removed: i18n follows the system locale by design (no in-app picker).
            SettingsSection(stringResource(R.string.settings_section_appearance)) {
                NkSettingsRow(
                    icon = Icons.Filled.Brightness6,
                    title = stringResource(R.string.settings_theme),
                    subtitle = themeLabel(themeMode),
                    onClick = { showThemeDialog = true }
                )
            }

            // ── About section ──
            SettingsSection(stringResource(R.string.settings_section_about)) {
                NkSettingsRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.app_name),
                    subtitle = if (versionName.isBlank()) stringResource(R.string.settings_about_description)
                    else stringResource(R.string.settings_about_version) + " $versionName",
                    onClick = {},
                    trailing = {}
                )
                NkSettingsRow(
                    icon = Icons.Filled.Code,
                    title = stringResource(R.string.settings_about_github),
                    subtitle = GITHUB_URL,
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
                        }
                    }
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

    if (showThemeDialog) {
        NkSingleChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = themeMode,
            onSelect = { appContainer.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun StatusLine(text: String, isError: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = NkSpacing.sm, vertical = NkSpacing.xs)
    )
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

private const val GITHUB_URL = "https://github.com/EvansCery/NKNote"
