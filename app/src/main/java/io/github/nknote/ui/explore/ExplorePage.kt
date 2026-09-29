package io.github.nknote.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.nknote.model.Weather
import io.github.nknote.ui.components.MoodIconMap
import io.github.nknote.ui.components.NkEmptyState
import io.github.nknote.ui.components.NkNoteCard
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.components.WeatherIconMap
import io.github.nknote.ui.components.nkListPadding
import io.github.nknote.ui.navigation.LocalDrawerState
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkIconSize
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExplorePage(
    nav: NkNoteNavigation,
    openNote: (Int) -> Unit,
    viewModel: ExploreViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val total by viewModel.totalNotes.collectAsStateWithLifecycle()

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NkTopAppBar(
                    title = stringResource(R.string.explore_title),
                    drawerState = LocalDrawerState.current
                )
            }
        ) { padding ->
            if (total == 0) {
                // Brand-new user: nothing to explore at all.
                NkEmptyState(
                    message = stringResource(R.string.explore_no_notes),
                    icon = Icons.Filled.Explore,
                    modifier = Modifier.padding(padding)
                )
                return@Scaffold
            }
            LazyColumn(
                contentPadding = nkListPadding(padding),
                verticalArrangement = Arrangement.spacedBy(NkSpacing.md),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ExploreHeader(total = total)
                }
                item {
                    SectionTitle(
                        text = stringResource(R.string.explore_on_this_day),
                        icon = Icons.Filled.AutoAwesome
                    )
                }
                if (sections.isEmpty()) {
                    item {
                        NkEmptyState(
                            message = stringResource(R.string.explore_this_day_empty),
                            fillMaxSize = false
                        )
                    }
                } else {
                    sections.forEach { section ->
                        item(key = "year-${section.year}") {
                            Text(
                                text = stringResource(R.string.explore_years_ago, section.yearsAgo, section.year),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = NkSpacing.sm)
                            )
                        }
                        items(section.notes, key = { it.id }) { note ->
                            ExploreRow(
                                note = note,
                                onClick = { openNote(note.id) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreHeader(total: Int) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = NkShapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(NkSpacing.xl)) {
            Text(stringResource(R.string.explore_notes_count, total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(NkSpacing.xs))
            Text(stringResource(R.string.explore_header_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NkSpacing.sm), modifier = Modifier.padding(top = NkSpacing.lg, bottom = NkSpacing.xs)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(NkIconSize.md))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExploreRow(note: Note, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // No onLongClick: the card is a plain clickable, so long-presses are not swallowed.
    NkNoteCard(onClick = onClick, modifier = modifier) {
        Column {
            Text(note.title.ifBlank { stringResource(R.string.common_no_title) }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(NkSpacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Weather.fromKey(note.weather)?.let {
                    Icon(WeatherIconMap.icon(it), stringResource(WeatherIconMap.labelRes(it)), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(NkIconSize.sm))
                }
                Mood.fromKey(note.mood)?.let {
                    Icon(MoodIconMap.icon(it), stringResource(MoodIconMap.labelRes(it)), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(NkIconSize.sm))
                }
                Text(note.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}