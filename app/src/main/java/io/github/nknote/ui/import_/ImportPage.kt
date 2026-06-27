package io.github.nknote.ui.import_

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPage(
    nav: NkNoteNavigation,
    viewModel: ImportViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val successMsg = stringResource(R.string.import_success)
    val status: String? = when (val r = result) {
        ImportViewModel.Result.Success -> successMsg
        is ImportViewModel.Result.Failed -> stringResource(R.string.import_failed, r.message)
        else -> null
    }
    val busy = result is ImportViewModel.Result.Busy

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.import(uri)
    }

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NkTopAppBar(
                    title = stringResource(R.string.import_title),
                    onBack = nav.back
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(NkSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NkSpacing.lg)
            ) {
                Icon(Icons.Filled.FileOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = NkSpacing.xl))
                Text(stringResource(R.string.import_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(
                    onClick = { launcher.launch(arrayOf("text/plain")) },
                    enabled = !busy,
                    shape = NkShapes.smallMedium
                ) { Text(stringResource(R.string.import_pick_file)) }
                status?.let {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = NkShapes.mediumSmall, modifier = Modifier.fillMaxWidth()) {
                        Text(it, modifier = Modifier.padding(NkSpacing.lg), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}