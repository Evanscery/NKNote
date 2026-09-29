package io.github.nknote.ui.import_

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import io.github.nknote.ui.navigation.LocalDrawerState
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkIconSize
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
    val status: Pair<String, Boolean>? = when (val r = result) {  // (text, isError)
        is ImportViewModel.Result.Success ->
            stringResource(R.string.import_success_count, r.imported) to false
        is ImportViewModel.Result.Failed ->
            stringResource(R.string.import_failed, r.message) to true
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
                    drawerState = LocalDrawerState.current
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(NkSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NkSpacing.lg)
            ) {
                Icon(Icons.Filled.FileOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = NkSpacing.xl).size(NkIconSize.xl))
                Text(stringResource(R.string.import_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(
                    onClick = {
                        viewModel.reset()
                        // octet-stream: many providers report zips that way; the VM routes by
                        // file name, so a wrong pick fails safely with a message.
                        launcher.launch(arrayOf("text/plain", "application/zip", "application/json", "application/octet-stream"))
                    },
                    enabled = !busy,
                    shape = NkShapes.smallMedium
                ) { Text(stringResource(R.string.import_pick_file)) }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NkSpacing.sm)) {
                        CircularProgressIndicator(modifier = Modifier.size(NkIconSize.md), strokeWidth = 2.dp)
                        Text(stringResource(R.string.import_progress), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                status?.let { (text, isError) ->
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = NkShapes.mediumSmall, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(NkSpacing.lg)) {
                            Icon(
                                if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(NkIconSize.md)
                            )
                            Spacer(Modifier.width(NkSpacing.sm))
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}