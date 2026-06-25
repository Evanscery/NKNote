package io.github.nknote.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.data.entity.Note
import io.github.nknote.ui.components.NkNoteCard
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashPage(
    nav: NkNoteNavigation,
    viewModel: TrashViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    val notes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    var showClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.trash_title)) },
                    navigationIcon = { IconButton(onClick = nav.back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = {
                        if (notes.isNotEmpty()) {
                            IconButton(onClick = { showClear = true }) {
                                Icon(Icons.Filled.DeleteForever, stringResource(R.string.trash_clear_all))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.trash_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 24.dp,
                        start = 16.dp, end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        TrashRow(note = note, onRestore = { viewModel.restore(note.id) }, onDelete = { viewModel.deleteForever(note.id) })
                    }
                }
            }
        }

        if (showClear) {
            AlertDialog(
                onDismissRequest = { showClear = false },
                title = { Text(stringResource(R.string.trash_clear_all)) },
                text = { Text(stringResource(R.string.trash_clear_all_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { viewModel.emptyTrash() }
                        showClear = false
                    }) { Text(stringResource(R.string.trash_clear), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showClear = false }) { Text(stringResource(R.string.trash_cancel)) } }
            )
        }
    }
}

@Composable
private fun TrashRow(note: Note, onRestore: () -> Unit, onDelete: () -> Unit) {
    NkNoteCard(onClick = onRestore, onLongClick = onDelete) {
        Column {
            Text(
                text = note.title.ifBlank { stringResource(R.string.common_no_title) },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(note.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRestore) {
                    Icon(Icons.Filled.Restore, null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.trash_restore))
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.height(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.trash_delete_forever), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}