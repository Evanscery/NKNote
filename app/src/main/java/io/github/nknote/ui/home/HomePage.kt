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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
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
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.nknote.ui.components.MoodIconMap
import io.github.nknote.ui.components.NkEmptyState
import io.github.nknote.ui.components.NkNoteCard
import io.github.nknote.ui.components.WeatherIconMap
import io.github.nknote.ui.navigation.LocalDrawerState
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkIconSize
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing
import kotlinx.coroutines.launch
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
    // The global drawer state is hoisted to NkNoteApp; the hamburger in the custom HomeTopBar
    // opens it. (Home has a search field so it can't reuse NkTopAppBar.)
    val drawerState: DrawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    HomeContent(
        notes = notes,
        query = query,
        onQueryChange = { query = it; viewModel.setQuery(it) },
        onNewNote = { nav.toEditor(null) },
        onOpenNote = openNote,
        onTrash = { viewModel.moveToTrash(it) },
        onOpenMenu = { scope.launch { drawerState.open() } },
        onOpenCalendar = { nav.toCalendar() }
    )
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
                shape = NkShapes.mediumLarge,
                icon = { Icon(Icons.Filled.Edit, null) },
                text = { Text(stringResource(R.string.home_title)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (notes.isEmpty()) {
            NkEmptyState(
                message = if (query.isBlank()) stringResource(R.string.home_empty)
                else stringResource(R.string.home_search_empty),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp,
                    start = NkSpacing.lg, end = NkSpacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(NkSpacing.md),
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
        Column(modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = NkSpacing.sm, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenMenu) { Icon(Icons.Filled.Menu, null) }
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = NkSpacing.xs)
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
                shape = NkShapes.medium,
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
                Spacer(Modifier.height(NkSpacing.xs))
                Text(
                    text = note.excerpt.ifBlank { plainPreview(note.content) }.ifBlank { stringResource(R.string.common_no_description) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = NkSpacing.sm)
                ) {
                    Weather.fromKey(note.weather)?.let {
                        Icon(WeatherIconMap.icon(it), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(NkIconSize.sm))
                    }
                    Mood.fromKey(note.mood)?.let {
                        Icon(MoodIconMap.icon(it), null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(NkIconSize.sm))
                    }
                    Text(note.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onTrash, modifier = Modifier.size(NkIconSize.xl)) {
                Icon(Icons.Filled.DeleteOutline, stringResource(R.string.home_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun plainPreview(contentJson: String): String {
    if (contentJson.isBlank()) return ""
    return runCatching { documentJson.decodeFromString<RichDocument>(contentJson).plainText().trim() }
        .getOrNull().orEmpty()
}