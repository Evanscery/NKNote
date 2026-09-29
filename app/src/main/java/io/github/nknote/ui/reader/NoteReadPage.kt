package io.github.nknote.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.data.entity.Tag
import io.github.nknote.model.Mood
import io.github.nknote.model.ParagraphAlignment
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.Weather
import io.github.nknote.ui.components.MoodIconMap
import io.github.nknote.ui.components.NkEmptyState
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.components.WeatherIconMap
import io.github.nknote.ui.editor.richtext.parseHexColor
import io.github.nknote.ui.editor.richtext.numberedCounters
import io.github.nknote.ui.editor.richtext.toAnnotatedString
import io.github.nknote.ui.editor.richtext.toEditor
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkIconSize
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

/**
 * Read-only note view — the screen a note opens into from Home / Explore / Calendar. Renders the
 * full rich document (headings, lists, quotes, code, images, checkboxes) with TAPPABLE
 * hyperlinks (the editor's UrlAnnotations finally have a consumer), and an edit affordance that
 * pushes the editor. Checkboxes toggle and persist directly from here.
 */
@OptIn(ExperimentalTextApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteReadPage(
    noteId: Int,
    nav: NkNoteNavigation,
    viewModel: NoteReadViewModel = viewModel(
        factory = AppViewModelFactory.readerFactory(noteId),
        key = "reader-$noteId"
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NkTopAppBar(
                    title = "",
                    onBack = nav.back
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { nav.toEditor(noteId) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = NkShapes.mediumLarge
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.reader_edit))
                }
            }
        ) { padding ->
            val note = state.note
            if (note == null) {
                if (state.loaded) {
                    NkEmptyState(
                        message = stringResource(R.string.common_no_description),
                        modifier = Modifier.padding(padding)
                    )
                }
                return@Scaffold
            }

            val codeBackground = MaterialTheme.colorScheme.surfaceVariant
            val editorDoc = remember(note.content) { state.document.toEditor() }
            val counters = remember(editorDoc) { numberedCounters(editorDoc.paragraphs) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = NkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(NkSpacing.xs)
            ) {
                // ── Header: cover / title / meta / tags ──
                note.coverImagePath?.takeIf { it.isNotBlank() }?.let { cover ->
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(NkShapes.mediumLarge)
                            .clickable { nav.toViewer(cover) }
                    )
                    Spacer(Modifier.height(NkSpacing.sm))
                }
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NkSpacing.sm),
                    modifier = Modifier.padding(vertical = NkSpacing.xs)
                ) {
                    Text(note.date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                }
                if (state.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(NkSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(NkSpacing.xs)
                    ) {
                        state.tags.forEach { name -> TagChip(name) }
                    }
                    Spacer(Modifier.height(NkSpacing.xs))
                }

                // ── Body ──
                if (state.document.isEmpty && editorDoc.paragraphs.none { it.image != null }) {
                    NkEmptyState(
                        message = stringResource(R.string.common_no_description),
                        fillMaxSize = false
                    )
                }
                editorDoc.paragraphs.forEachIndexed { index, para ->
                    val image = para.image
                    when {
                        image != null -> {
                            val aspect = image.width.toFloat() / image.height.toFloat()
                            val heightDp = (300f / aspect.coerceIn(0.5f, 3f)).coerceIn(80f, 300f)
                            AsyncImage(
                                model = image.path,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(heightDp.dp)
                                    .clip(NkShapes.small)
                                    .clickable { nav.toViewer(image.path) }
                            )
                        }
                        para.style == ParagraphStyle.CHECKBOX -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = para.checked,
                                    onCheckedChange = { viewModel.toggleChecked(index) },
                                    modifier = Modifier.size(NkIconSize.xl)
                                )
                                Spacer(Modifier.width(NkSpacing.sm))
                                ReadParagraph(
                                    annotated = para.toAnnotatedString(
                                        counters.getOrNull(index) ?: 0, codeBackground
                                    ),
                                    alignment = para.alignment,
                                    dimmed = para.checked,
                                    onUrl = { uriHandler.openUri(it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        else -> {
                            ReadParagraph(
                                annotated = para.toAnnotatedString(
                                    counters.getOrNull(index) ?: 0, codeBackground
                                ),
                                alignment = para.alignment,
                                onUrl = { uriHandler.openUri(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(NkSpacing.xxl))
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ReadParagraph(
    annotated: androidx.compose.ui.text.AnnotatedString,
    alignment: ParagraphAlignment,
    onUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    val textAlign = when (alignment) {
        ParagraphAlignment.START -> TextAlign.Start
        ParagraphAlignment.CENTER -> TextAlign.Center
        ParagraphAlignment.END -> TextAlign.End
    }
    val baseColor = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onBackground
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(color = baseColor, textAlign = textAlign),
        modifier = modifier.padding(vertical = 2.dp),
        onClick = { offset ->
            annotated.getUrlAnnotations(offset, offset).firstOrNull()?.let { onUrl(it.item.url) }
        }
    )
}

/** Small tinted tag chip; the tint is the tag's deterministic palette color. */
@Composable
private fun TagChip(name: String) {
    val tagColor = parseHexColor(Tag.colorFor(name)) ?: MaterialTheme.colorScheme.primary
    Surface(
        color = tagColor.copy(alpha = 0.14f),
        shape = NkShapes.small
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = NkSpacing.sm, vertical = 3.dp)
        ) {
            Box(Modifier.size(6.dp).clip(NkShapes.material.extraSmall).background(tagColor))
            Spacer(Modifier.width(NkSpacing.xs))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
