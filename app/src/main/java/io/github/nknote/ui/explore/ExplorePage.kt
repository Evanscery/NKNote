package io.github.nknote.ui.explore

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import io.github.nknote.ui.components.NkNoteCard
import io.github.nknote.ui.components.WeatherIconMap
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorePage(
    nav: NkNoteNavigation,
    openNote: (Int) -> Unit,
    viewModel: ExploreViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    val thisDay by viewModel.todayThisDay.collectAsStateWithLifecycle()
    val total by viewModel.totalNotes.collectAsStateWithLifecycle()

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.explore_title)) },
                    navigationIcon = { IconButton(onClick = nav.back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 24.dp,
                    start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                if (thisDay.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.explore_this_day_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(thisDay, key = { it.id }) { note ->
                        ExploreRow(note = note, onClick = { openNote(note.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreHeader(total: Int) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.explore_dates_written, total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.drawer_greeting), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExploreRow(note: Note, onClick: () -> Unit) {
    NkNoteCard(onClick = onClick, onLongClick = {}) {
        Column {
            Text(note.title.ifBlank { stringResource(R.string.common_no_title) }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Weather.fromKey(note.weather)?.let { Icon(WeatherIconMap.icon(it), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) }
                Mood.fromKey(note.mood)?.let { Icon(MoodIconMap.icon(it), null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp)) }
                Text(note.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}