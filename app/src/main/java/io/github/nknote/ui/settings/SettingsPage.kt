package io.github.nknote.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nknote.R
import io.github.nknote.ui.components.NkSettingsRow
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(nav: NkNoteNavigation) {
    val context = LocalContext.current

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NkTopAppBar(
                    title = stringResource(R.string.nav_settings),
                    onBack = nav.back
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
                            Toast.makeText(context, context.getString(R.string.import_progress), Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // ── Appearance section ──
                var darkMode by remember { mutableStateOf(false) }
                SettingsSection(stringResource(R.string.settings_section_appearance)) {
                    NkSettingsRow(
                        icon = Icons.Filled.Brightness6,
                        title = stringResource(R.string.settings_theme_dark),
                        subtitle = stringResource(R.string.settings_theme_system),
                        onClick = { darkMode = !darkMode },
                        trailing = { Switch(checked = darkMode, onCheckedChange = { darkMode = it }) }
                    )
                    NkSettingsRow(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = stringResource(R.string.settings_language_system),
                        onClick = {}
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