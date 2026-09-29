package io.github.nknote.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.Tag
import io.github.nknote.model.Mood
import io.github.nknote.model.RichDocument
import io.github.nknote.model.Weather
import io.github.nknote.ui.components.LocalSnackbarHostState
import io.github.nknote.ui.components.MoodIconMap
import io.github.nknote.ui.components.NkEmptyState
import io.github.nknote.ui.components.NkNoteCard
import io.github.nknote.ui.components.WeatherIconMap
import io.github.nknote.ui.editor.richtext.parseHexColor
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
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val noteTags by viewModel.noteTags.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagId.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    // The global drawer state is hoisted to NkNoteApp; the hamburger in the custom HomeTopBar
    // opens it. (Home has a search field so it can't reuse NkTopAppBar.)
    val drawerState: DrawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val trashedMsg = stringResource(R.string.home_trashed_snackbar)
    val undoLabel = stringResource(R.string.common_undo)

    HomeContent(
        notes = notes,
        tags = tags,
        noteTags = noteTags,
        selectedTagId = selectedTagId,
        query = query,
        onQueryChange = { query = it; viewModel.setQuery(it) },
        onSelectTag = { viewModel.selectTag(it) },
        onNewNote = { nav.toEditor(null) },
        onOpenNote = openNote,
        onTrash = { id ->
            viewModel.moveToTrash(id)
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = trashedMsg,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restore(id)
            }
        },
        onOpenMenu = { scope.launch { drawerState.open() } },
        onOpenCalendar = { nav.toCalendar() }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    notes: List<Note>,
    tags: List<Tag>,
    noteTags: Map<Int, List<Tag>>,
    selectedTagId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onNewNote: () -> Unit,
    onOpenNote: (Int) -> Unit,
    onTrash: (Int) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Scaffold(
        topBar = {
            HomeTopBar(
                query = query,
                onQueryChange = onQueryChange,
                tags = tags,
                selectedTagId = selectedTagId,
                onSelectTag = onSelectTag,
                onOpenMenu = onOpenMenu,
                onOpenCalendar = onOpenCalendar
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = NkShapes.mediumLarge,
                icon = { Icon(Icons.Filled.Edit, null) },
                text = { Text(stringResource(R.string.home_new_entry)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (notes.isEmpty()) {
            NkEmptyState(
                message = if (query.isBlank() && selectedTagId == null) stringResource(R.string.home_empty)
                else stringResource(R.string.home_search_empty),
                icon = Icons.Filled.EditNote,
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
                    NoteCard(
                        note = note,
                        tags = noteTags[note.id].orEmpty(),
                        onClick = { onOpenNote(note.id) },
                        onTrash = { onTrash(note.id) },
                        onLongPressTrash = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTrash(note.id)
                        },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    tags: List<Tag>,
    selectedTagId: String?,
    onSelectTag: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = NkSpacing.sm, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Filled.Menu, stringResource(R.string.a11y_open_menu))
                }
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = NkSpacing.xs)
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenCalendar) {
                    Icon(Icons.Filled.CalendarMonth, stringResource(R.string.calendar_title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.a11y_clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(NkIconSize.md)
                            )
                        }
                    }
                },
                placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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
            if (tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(NkSpacing.xs),
                    contentPadding = PaddingValues(vertical = NkSpacing.xs)
                ) {
                    items(tags, key = { it.id }) { tag ->
                        val tagColor = parseHexColor(tag.color) ?: MaterialTheme.colorScheme.primary
                        FilterChip(
                            selected = tag.id == selectedTagId,
                            onClick = { onSelectTag(tag.id) },
                            label = { Text(tag.name) },
                            leadingIcon = {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(tagColor))
                            },
                            shape = NkShapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    tags: List<Tag>,
    onClick: () -> Unit,
    onTrash: () -> Unit,
    onLongPressTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    NkNoteCard(onClick = onClick, onLongClick = onLongPressTrash, modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.common_no_title) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(NkSpacing.xs))
                val preview = remember(note.id, note.updatedAt) {
                    note.excerpt.ifBlank { plainPreview(note.content) }
                }
                Text(
                    text = preview.ifBlank { stringResource(R.string.common_no_description) },
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
                        Icon(
                            WeatherIconMap.icon(it),
                            contentDescription = stringResource(WeatherIconMap.labelRes(it)),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(NkIconSize.sm)
                        )
                    }
                    Mood.fromKey(note.mood)?.let {
                        Icon(
                            MoodIconMap.icon(it),
                            contentDescription = stringResource(MoodIconMap.labelRes(it)),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(NkIconSize.sm)
                        )
                    }
                    Text(note.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    tags.take(3).forEach { tag ->
                        val tagColor = parseHexColor(tag.color) ?: MaterialTheme.colorScheme.primary
                        Surface(color = tagColor.copy(alpha = 0.14f), shape = NkShapes.material.extraSmall) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                if (!note.coverImagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = note.coverImagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(NkShapes.small)
                    )
                    Spacer(Modifier.height(NkSpacing.xs))
                }
                // Default IconButton = 48dp touch target; the icon itself stays small.
                IconButton(onClick = onTrash) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        stringResource(R.string.home_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(NkIconSize.md)
                    )
                }
            }
        }
    }
}

private fun plainPreview(contentJson: String): String {
    if (contentJson.isBlank()) return ""
    return runCatching { documentJson.decodeFromString<RichDocument>(contentJson).plainText().trim() }
        .getOrNull().orEmpty()
}
