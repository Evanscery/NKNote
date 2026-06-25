package io.github.nknote.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.data.entity.Note
import io.github.nknote.model.Mood
import io.github.nknote.model.RichDocument
import io.github.nknote.model.Weather
import io.github.nknote.ui.components.NkDrawer
import io.github.nknote.ui.components.NkDrawerItem
import io.github.nknote.ui.components.NkNoteCard
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme
import kotlinx.serialization.json.Json

private val documentJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    nav: NkNoteNavigation,
    openNote: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    NkNoteTheme {
        NkDrawer(
            items = listOf(
                NkDrawerItem(stringResource(R.string.nav_home), Icons.Filled.Tune),
                NkDrawerItem(stringResource(R.string.nav_explore), Icons.Filled.Explore),
                NkDrawerItem(stringResource(R.string.nav_trash), Icons.Filled.DeleteOutline),
                NkDrawerItem(stringResource(R.string.tools_title), Icons.Filled.Build),
                NkDrawerItem(stringResource(R.string.nav_settings), Icons.Filled.Settings)
            ),
            onSelect = { index ->
                when (index) {
                    1 -> nav.toExplore()
                    2 -> nav.toTrash()
                    3 -> nav.toTools()
                    4 -> nav.toSettings()
                }
            },
            content = { drawerToggle ->
                HomeContent(
                    notes = notes,
                    query = query,
                    onQueryChange = { query = it; viewModel.setQuery(it) },
                    onNewNote = { nav.toEditor(null) },
                    onOpenNote = openNote,
                    onTrash = { viewModel.moveToTrash(it) },
                    onOpenMenu = drawerToggle,
                    onOpenCalendar = { nav.toCalendar() }
                )
            }
        )
    }
}

@Composable
private fun HomeContent(
    notes: List<Note>,
    query: String,
    onQueryChange: (String) -> Unit,
    onNewNote: () -> Unit,
    onOpenNote: (Int) -> Unit,
    onTrash: (Int) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    Scaffold(
        topBar = { HomeTopBar(query = query, onQueryChange = onQueryChange, onOpenMenu = onOpenMenu, onOpenCalendar = onOpenCalendar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Filled.Edit, null) },
                text = { Text(stringResource(R.string.home_title)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (notes.isEmpty()) {
            EmptyState(
                message = if (query.isBlank()) stringResource(R.string.home_empty)
                else stringResource(R.string.home_search_empty),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp,
                    start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onOpenNote(note.id) }, onTrash = { onTrash(note.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenMenu) { Icon(Icons.Filled.Tune, null) }
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                // Issue 2: calendar button opens calendar view showing days with notes
                IconButton(onClick = onOpenCalendar) {
                    Icon(Icons.Filled.CalendarMonth, stringResource(R.string.calendar_title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onTrash: () -> Unit) {
    NkNoteCard(onClick = onClick, onLongClick = onTrash) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.common_no_title) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.description.ifBlank { plainPreview(note.content) }.ifBlank { stringResource(R.string.common_no_description) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Weather.fromKey(note.weather)?.let {
                        Icon(it.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    }
                    Mood.fromKey(note.mood)?.let {
                        Icon(it.icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                    }
                    Text(note.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onTrash, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.DeleteOutline, stringResource(R.string.home_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun plainPreview(contentJson: String): String {
    if (contentJson.isBlank()) return ""
    return runCatching { documentJson.decodeFromString<RichDocument>(contentJson).plainText().trim() }
        .getOrNull().orEmpty()
}