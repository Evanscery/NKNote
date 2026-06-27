package io.github.nknote.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nknote.R
import io.github.nknote.ui.components.NkSettingsRow
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.theme.NkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsPage(nav: NkNoteNavigation) {
    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NkTopAppBar(
                    title = stringResource(R.string.tools_title),
                    onBack = nav.back
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = NkSpacing.lg, vertical = NkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.tools_section_utilities),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = NkSpacing.lg, bottom = NkSpacing.xs, start = NkSpacing.sm)
                )
                NkSettingsRow(
                    icon = Icons.Filled.Casino,
                    title = stringResource(R.string.random_title),
                    subtitle = stringResource(R.string.random_subtitle),
                    onClick = { nav.toRandom() }
                )
            }
        }
    }
}