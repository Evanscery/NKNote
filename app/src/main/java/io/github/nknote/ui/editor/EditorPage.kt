package io.github.nknote.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.data.templates.NoteTemplate
import io.github.nknote.model.Mood
import io.github.nknote.model.ParagraphAlignment
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.Weather
import io.github.nknote.ui.components.ColorPickerDialog
import io.github.nknote.ui.components.FontSizePickerDialog
import io.github.nknote.ui.components.MoodIconMap
import io.github.nknote.ui.components.MoodPickerDialog
import io.github.nknote.ui.components.NkDialog
import io.github.nknote.ui.components.WeatherIconMap
import io.github.nknote.ui.components.WeatherPickerDialog
import io.github.nknote.ui.editor.richtext.SpanVisualTransformation
import io.github.nknote.ui.editor.richtext.HighlightRange
import io.github.nknote.ui.editor.richtext.numberedCounters
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkIconSize
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPage(
    noteId: Int?,
    nav: NkNoteNavigation,
    viewModel: EditorViewModel = viewModel(
        factory = AppViewModelFactory.editorFactory(noteId ?: -1)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDate by remember { mutableStateOf(false) }
    var showWeather by remember { mutableStateOf(false) }
    var showMood by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }
    var showFontSize by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showLink by remember { mutableStateOf(false) }
    var findActive by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var overflowExpanded by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }

    val weatherLabel = Weather.fromKey(uiState.weatherKey)?.let { stringResource(WeatherIconMap.labelRes(it)) }.orEmpty()
    val moodLabel = Mood.fromKey(uiState.moodKey)?.let { stringResource(MoodIconMap.labelRes(it)) }.orEmpty()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.insertImageAfter(viewModel.focusedIndex, uri)
    }
    val coverImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCoverImage(uri)
    }

    // ── Autosave (todo 12) ──────────────────────────────────────────────────────
    // ON_STOP: silent save (no nav). Covers backgrounding the app / navigating away so a
    // kill-restore shows the last saved state. The VM's saveMutex + savedNoteId guard prevent a
    // concurrent manual save + this autosave from creating duplicate rows.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.save()
    }
    // Debounced: 3 s after the last edit. editVersion is bumped on every document / meta mutation
    // (and NOT on load/restore), so a freshly opened note does NOT trigger an immediate save.
    LaunchedEffect(viewModel.editVersion) {
        if (viewModel.editVersion == 0) return@LaunchedEffect
        delay(3_000)
        viewModel.save()
    }

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    EditorTopBar(
                        title = uiState.title,
                        onTitleChange = { viewModel.updateTitle(it) },
                        onBack = nav.back,
                        // Existing note: pop back to the reader (which live-updates via
                        // observeNote). New note: clear back to Home.
                        onSave = { viewModel.save { if (noteId == null) nav.toHomeAndClear() else nav.back() } },
                        overflow = {
                            // Overflow menu (templates) is only reachable on a NEW note so an
                            // existing note's content is never silently replaced by a template.
                            if (noteId == null) {
                                Box {
                                    IconButton(onClick = { overflowExpanded = true }) {
                                        Icon(
                                            Icons.Filled.MoreVert,
                                            contentDescription = stringResource(R.string.editor_overflow),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = overflowExpanded,
                                        onDismissRequest = { overflowExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.editor_templates)) },
                                            onClick = { overflowExpanded = false; showTemplates = true }
                                        )
                                    }
                                }
                            }
                        },
                        detailsExpanded = detailsExpanded,
                        onToggleDetails = { detailsExpanded = !detailsExpanded }
                    )
                    AnimatedVisibility(visible = findActive, enter = expandVertically(), exit = shrinkVertically()) {
                        FindBar(
                            query = findQuery,
                            onQueryChange = { findQuery = it; viewModel.updateFind(it) },
                            matchCount = uiState.findMatches.size,
                            matchIndex = uiState.findIndex,
                            onPrev = { viewModel.findPrev() },
                            onNext = { viewModel.findNext() },
                            onClose = {
                                findActive = false
                                findQuery = ""
                                viewModel.clearFind()
                            }
                        )
                    }
                    AnimatedVisibility(visible = detailsExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        MetaPanel(
                            title = uiState.title,
                            onTitle = { viewModel.updateTitle(it) },
                            excerpt = uiState.excerpt,
                            onExcerpt = { viewModel.updateExcerpt(it) },
                            weatherLabel = weatherLabel,
                            moodLabel = moodLabel,
                            date = uiState.date,
                            coverImagePath = uiState.coverImagePath,
                            tagNames = uiState.tagNames,
                            onAddTag = { viewModel.addTag(it) },
                            onRemoveTag = { viewModel.removeTag(it) },
                            onPickCoverImage = { coverImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onRemoveCoverImage = { viewModel.removeCoverImage() },
                            onPickWeather = { showWeather = true },
                            onPickMood = { showMood = true },
                            onPickDate = { showDate = true }
                        )
                    }
                }
            },
            bottomBar = {
                FormatBar(
                    state = uiState,
                    onBold = { viewModel.toggleBold() },
                    onItalic = { viewModel.toggleItalic() },
                    onUnderline = { viewModel.toggleUnderline() },
                    onStrike = { viewModel.toggleStrikethrough() },
                    onColor = { showColor = true },
                    onSize = { showFontSize = true },
                    onParagraphStyle = { style -> viewModel.setParagraphStyle(viewModel.focusedIndex, style) },
                    onQuote = { viewModel.setParagraphStyle(viewModel.focusedIndex, ParagraphStyle.QUOTE) },
                    onBullet = { viewModel.setParagraphStyle(viewModel.focusedIndex, ParagraphStyle.BULLET) },
                    onNumbered = { viewModel.setParagraphStyle(viewModel.focusedIndex, ParagraphStyle.NUMBERED) },
                    onCheckbox = { viewModel.setParagraphStyle(viewModel.focusedIndex, ParagraphStyle.CHECKBOX) },
                    onCode = { viewModel.toggleCode() },
                    onLink = { showLink = true },
                    onFind = { findActive = true },
                    onImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onAlign = { a -> viewModel.setParagraphAlignment(viewModel.focusedIndex, a) },
                    onIndent = { d -> viewModel.setIndentLevel(viewModel.focusedIndex, d) },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() }
                )
            }
        ) { padding ->
            EditorContent(viewModel = viewModel, nav = nav, padding = padding)
        }
    }

    if (showLink) {
        LinkDialog(
            hasLinkAtCursor = uiState.linkAtCursor,
            onApply = { url -> viewModel.setLinkOnSelection(url); showLink = false },
            onRemove = { viewModel.clearLinkOnSelection(); showLink = false },
            onDismiss = { showLink = false }
        )
    }
    if (showDate) NkDatePicker(initial = uiState.date, onDate = { viewModel.updateDate(it); showDate = false }, onDismiss = { showDate = false })
    if (showWeather) WeatherPickerDialog(onPick = { viewModel.updateWeather(it); showWeather = false }, onDismiss = { showWeather = false })
    if (showMood) MoodPickerDialog(onPick = { viewModel.updateMood(it); showMood = false }, onDismiss = { showMood = false })
    if (showColor) ColorPickerDialog(onPick = { viewModel.setColor(it); showColor = false }, onDismiss = { showColor = false })
    if (showFontSize) FontSizePickerDialog(onPick = { viewModel.setFontSizeScale(it); showFontSize = false }, onDismiss = { showFontSize = false })
    if (showTemplates) {
        TemplatePickerDialog(
            onPick = { template: NoteTemplate ->
                viewModel.applyTemplate(template.document)
                showTemplates = false
            },
            onDismiss = { showTemplates = false }
        )
    }
}

// ── Top bar with collapsible detail toggle ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    overflow: @Composable () -> Unit = {},
    detailsExpanded: Boolean,
    onToggleDetails: () -> Unit
) {
    val arrowRotation by animateFloatAsState(targetValue = if (detailsExpanded) 180f else 0f, label = "expand_arrow")
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.editor_title_placeholder)) },
                    textStyle = MaterialTheme.typography.titleLarge,
                    shape = NkShapes.mediumSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleDetails) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.editor_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            }
        },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
        actions = {
            overflow()
            IconButton(onClick = onSave) { Icon(Icons.Filled.Check, stringResource(R.string.editor_save)) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

// ── Find-in-page bar (rendered under the top bar while active) ─────────────────

@Composable
private fun FindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchCount: Int,
    matchIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = NkSpacing.md, vertical = NkSpacing.xs)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.editor_find_placeholder)) },
                shape = NkShapes.mediumSmall,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(NkSpacing.sm))
            Text(
                text = if (matchCount == 0 && query.isNotEmpty()) stringResource(R.string.editor_find_no_matches)
                else if (matchCount > 0) "${matchIndex + 1}/$matchCount" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onPrev, enabled = matchCount > 0) {
                Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.editor_find_prev), modifier = Modifier.size(NkIconSize.md))
            }
            IconButton(onClick = onNext, enabled = matchCount > 0) {
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.editor_find_next), modifier = Modifier.size(NkIconSize.md))
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, stringResource(R.string.common_close), modifier = Modifier.size(NkIconSize.md))
            }
        }
    }
}

// ── Link dialog ────────────────────────────────────────────────────────────────

@Composable
private fun LinkDialog(
    hasLinkAtCursor: Boolean,
    onApply: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    NkDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.editor_link_dialog_title),
        confirmText = stringResource(R.string.editor_link_apply),
        onConfirm = { if (url.isNotBlank()) onApply(url) else onDismiss() },
        dismissText = stringResource(R.string.common_cancel)
    ) {
        Column {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                label = { Text(stringResource(R.string.editor_link_url_label)) },
                placeholder = { Text("https://") },
                shape = NkShapes.mediumSmall,
                modifier = Modifier.fillMaxWidth()
            )
            if (hasLinkAtCursor) {
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.editor_link_remove), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ── Collapsed-by-default meta panel ────────────────────────────────────────────

@Composable
private fun MetaPanel(
    title: String,
    onTitle: (String) -> Unit,
    excerpt: String,
    onExcerpt: (String) -> Unit,
    weatherLabel: String,
    moodLabel: String,
    date: String,
    coverImagePath: String?,
    tagNames: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onPickCoverImage: () -> Unit,
    onRemoveCoverImage: () -> Unit,
    onPickWeather: () -> Unit,
    onPickMood: () -> Unit,
    onPickDate: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CoverImageSection(path = coverImagePath, onPick = onPickCoverImage, onRemove = onRemoveCoverImage)
            OutlinedTextField(
                value = title, onValueChange = onTitle, singleLine = true,
                label = { Text(stringResource(R.string.editor_title_placeholder)) },
                textStyle = MaterialTheme.typography.titleLarge, shape = NkShapes.mediumSmall,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = excerpt, onValueChange = onExcerpt,
                placeholder = { Text(stringResource(R.string.editor_description_placeholder)) },
                label = { Text(stringResource(R.string.editor_description)) },
                shape = NkShapes.mediumSmall, modifier = Modifier.fillMaxWidth()
            )
            TagsSection(tagNames = tagNames, onAddTag = onAddTag, onRemoveTag = onRemoveTag)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(text = weatherLabel, fallback = stringResource(R.string.editor_select_weather), onClick = onPickWeather)
                MetaChip(text = moodLabel, fallback = stringResource(R.string.editor_select_mood), onClick = onPickMood)
                MetaChip(text = date, fallback = stringResource(R.string.editor_select_date), onClick = onPickDate)
            }
        }
    }
}

/** Tag chips + inline input. Tags persist on save via [EditorViewModel.persistTags]. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tagNames: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    Column {
        if (tagNames.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NkSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NkSpacing.xs)
            ) {
                tagNames.forEach { name ->
                    val tagColor = io.github.nknote.ui.editor.richtext.parseHexColor(
                        io.github.nknote.data.entity.Tag.colorFor(name)
                    ) ?: MaterialTheme.colorScheme.primary
                    Surface(color = tagColor.copy(alpha = 0.14f), shape = NkShapes.small) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = NkSpacing.sm, end = NkSpacing.xs, top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { onRemoveTag(name) }, modifier = Modifier.size(NkIconSize.lg)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.common_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(NkIconSize.sm)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(NkSpacing.xs))
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            label = { Text(stringResource(R.string.editor_tags)) },
            placeholder = { Text(stringResource(R.string.editor_tag_input_placeholder)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    if (input.isNotBlank()) {
                        onAddTag(input)
                        input = ""
                    }
                }
            ),
            shape = NkShapes.mediumSmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CoverImageSection(path: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    if (path != null) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            coil.compose.AsyncImage(
                model = path, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(NkShapes.mediumSmall)
            )
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    } else {
        Surface(
            onClick = onPick,
            color = MaterialTheme.colorScheme.surface,
            shape = NkShapes.mediumSmall,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.editor_cover_image), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, fallback: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = NkShapes.mediumSmall, onClick = onClick) {
        Text(text = text.ifBlank { fallback }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

// ── Editor content with per-paragraph focus management ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorContent(
    viewModel: EditorViewModel,
    nav: NkNoteNavigation,
    padding: androidx.compose.foundation.layout.PaddingValues
) {
    val focusRequesters = remember { androidx.compose.runtime.mutableStateListOf<FocusRequester>() }
    while (focusRequesters.size < viewModel.paragraphs.size) focusRequesters.add(FocusRequester())
    while (focusRequesters.size > viewModel.paragraphs.size) focusRequesters.removeAt(focusRequesters.lastIndex)

    // Inline-image delete confirmation (file deletion is permanent and not undoable).
    var pendingImageDelete by remember { mutableStateOf<Int?>(null) }
    pendingImageDelete?.let { target ->
        NkDialog(
            onDismiss = { pendingImageDelete = null },
            title = stringResource(R.string.editor_delete_image),
            confirmText = stringResource(R.string.common_confirm),
            onConfirm = {
                viewModel.removeImageParagraph(target)
                pendingImageDelete = null
            },
            dismissText = stringResource(R.string.common_cancel)
        ) {
            Text(stringResource(R.string.editor_delete_image_confirm), style = MaterialTheme.typography.bodyMedium)
        }
    }

    // Column + verticalScroll (NOT LazyColumn): EditorParagraph has no stable id and BasicTextField
    // loses focus on LazyColumn item recycling. Diary-length docs compose fine in a Column.
    val scrollState = rememberScrollState()
    // Per-paragraph y-offset within the scrollable content (captured via onGloballyPositioned),
    // used by scroll-to-focused: when pendingFocusIndex triggers, animateScrollTo the target's y.
    val paragraphOffsets = remember { androidx.compose.runtime.mutableStateListOf<Int>() }
    while (paragraphOffsets.size < viewModel.paragraphs.size) paragraphOffsets.add(0)
    while (paragraphOffsets.size > viewModel.paragraphs.size) paragraphOffsets.removeAt(paragraphOffsets.lastIndex)

    LaunchedEffect(viewModel.pendingFocusIndex) {
        val idx = viewModel.pendingFocusIndex
        if (idx >= 0 && idx < focusRequesters.size) {
            // Scroll the focused paragraph into view, then request focus so the layout has settled.
            // Offsets are content-space (positionInParent of a direct child of the scrolled
            // Column), so they feed animateScrollTo directly; back off a small margin so the
            // paragraph isn't flush with the top edge.
            val target = ((paragraphOffsets.getOrNull(idx) ?: 0) - 48).coerceAtLeast(0)
            runCatching { scrollState.animateScrollTo(target) }
            runCatching { focusRequesters[idx].requestFocus() }
            viewModel.consumePendingFocus()
        }
    }

    // Find-in-page matches drive per-paragraph highlight overlays.
    val editorUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val findMatches = editorUiState.findMatches

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Running numbered-list counter per paragraph: 1, 2, 3 for consecutive NUMBERED paragraphs,
        // resetting to 1 after any non-NUMBERED paragraph (computed in richtext.numberedCounters).
        val counters = remember(viewModel.paragraphs) { numberedCounters(viewModel.paragraphs) }
        viewModel.paragraphs.forEachIndexed { index, para ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        // This Box is a direct child of the scrolled Column, so positionInParent
                        // IS the content-space y-offset — no root-space + scroll math (which
                        // double-counted the top bar height and overshot).
                        if (index < paragraphOffsets.size) {
                            paragraphOffsets[index] = coords.positionInParent().y.toInt()
                        }
                    }
            ) {
                if (para.image != null) {
                    ImageBlock(
                        path = para.image.path,
                        aspectRatio = para.image.width.toFloat() / para.image.height.toFloat(),
                        onLongPress = { nav.toViewer(para.image.path) },
                        onDelete = { pendingImageDelete = index }
                    )
                } else {
                    val requester = focusRequesters.getOrNull(index) ?: return@Box
                    val paraHighlights = remember(findMatches, index) {
                        findMatches.filter { it.paragraphIndex == index }
                            .map { HighlightRange(it.start, it.end) }
                    }
                    val field = @Composable {
                        ParagraphField(
                            index = index,
                            viewModel = viewModel,
                            focusRequester = requester,
                            numberedCounter = counters.getOrNull(index) ?: 0,
                            highlights = paraHighlights,
                            showPlaceholder = index == 0 && viewModel.paragraphs.size == 1 &&
                                para.text.isEmpty() && para.image == null &&
                                para.style == ParagraphStyle.BODY
                        )
                    }
                    if (para.style == ParagraphStyle.CHECKBOX) {
                        // Task-list item: a real Checkbox composable leads the field — a genuine
                        // tap target instead of a text marker with offset gymnastics.
                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = para.checked,
                                onCheckedChange = { viewModel.toggleChecked(index) },
                                modifier = Modifier.size(NkIconSize.xl)
                            )
                            Spacer(Modifier.width(NkSpacing.sm))
                            Box(Modifier.weight(1f)) { field() }
                        }
                    } else {
                        field()
                    }
                }
            }
        }
        // Issue 6: tappable blank space at bottom — focuses last text paragraph
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(Unit) {
                    detectTapGestures { _ -> viewModel.focusLastTextParagraph() }
                }
        )
    }
}

@Composable
private fun ParagraphField(
    index: Int,
    viewModel: EditorViewModel,
    focusRequester: FocusRequester,
    numberedCounter: Int,
    highlights: List<HighlightRange> = emptyList(),
    showPlaceholder: Boolean = false
) {
    val para = viewModel.paragraphs[index]
    val value = viewModel.fields.getOrNull(index) ?: TextFieldValue("")
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val textAlign = when (para.alignment) {
        ParagraphAlignment.START -> TextAlign.Start
        ParagraphAlignment.CENTER -> TextAlign.Center
        ParagraphAlignment.END -> TextAlign.End
    }
    val transformation = remember(para, numberedCounter, codeBackground, highlights) {
        SpanVisualTransformation(
            paragraph = para,
            numberedCounter = numberedCounter,
            codeBackground = codeBackground,
            highlights = highlights,
            guardPrefixLength = 1
        )
    }

    BasicTextField(
        value = value,
        // All routing (Enter/paste newline handling, soft-keyboard backspace-merge via the
        // sentinel guard, selection-only moves) lives in the ViewModel's onTextChange front door.
        onValueChange = { tv -> viewModel.onTextChange(index, tv) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = textAlign
        ),
        visualTransformation = transformation,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (showPlaceholder) {
                Box {
                    Text(
                        stringResource(R.string.editor_content_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    inner()
                }
            } else inner()
        },
        modifier = Modifier
            .fillMaxWidth()
            // Issue 6: minimum height so empty paragraphs are easy to tap
            .heightIn(min = 32.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) viewModel.onFocus(index) }
            .onPreviewKeyEvent { keyEvent ->
                // Ctrl+Z = undo, Ctrl+Shift+Z = redo (hardware keyboard). A hardware backspace
                // at paragraph start deletes the sentinel guard and flows through onTextChange —
                // the same path a soft keyboard takes — so no key-event merge branch is needed.
                if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Z && keyEvent.isCtrlPressed) {
                    if (keyEvent.isShiftPressed) viewModel.redo() else viewModel.undo()
                    true
                } else false
            }
            .padding(vertical = 4.dp)
    )
}

// ── Image block: auto-scale, long-press to open viewer ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageBlock(path: String, aspectRatio: Float, onLongPress: () -> Unit, onDelete: () -> Unit) {
    // Auto-scale: height derived from aspect ratio, capped to keep images reasonable
    val heightDp = (300f / aspectRatio.coerceIn(0.5f, 3f)).coerceIn(80f, 300f)
    Box(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
        AsyncImage(
            model = path,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(NkShapes.small)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
        )
        // Visible delete affordance — the only reliable way to remove an image with a soft
        // keyboard used to be a backspace merge; this button works everywhere.
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd).padding(NkSpacing.xs)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                shape = NkShapes.small
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.editor_delete_image),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(2.dp).size(NkIconSize.md)
                )
            }
        }
    }
}

// ── Soft format bar with scroll-edge fade hint ──────────────────────────────────

/**
 * Editor formatting toolbar (todo 10). Exposes the full power-user affordance set built natively
 * on the existing per-paragraph model — no third-party editor dependency:
 *
 * - **Span toggles**: Bold / Italic / Underline / Strikethrough / Color / Font size — highlighted
 *   when the cursor sits inside a span carrying that style (read from [EditorUiState.styleAtCursor]).
 * - **Paragraph style**: a heading-hierarchy dropdown (`Title` / `Heading` / `Subheading` / `Body`)
 *   plus `Quote` / `Bullet` / `Numbered list` quick toggles. Highlighted when the focused
 *   paragraph's [ParagraphStyle] matches ([EditorUiState.paragraphStyleAtCursor]).
 * - **Alignment**: a Left / Center / Right group calling [EditorViewModel.setParagraphAlignment],
 *   highlighted against [EditorUiState.paragraphAlignmentAtCursor].
 * - **Indent / Outdent**: +1 / -1 on [EditorViewModel.setIndentLevel] (clamped 0..3); outdent is
 *   disabled at indent 0 so it can never go negative.
 * - The horizontal scroll-edge fade (left/right gradient) is preserved.
 *
 * All buttons use [NkShapes] / [NkSpacing] design tokens (todo 5).
 */
@Composable
private fun FormatBar(
    state: EditorUiState,
    onBold: () -> Unit, onItalic: () -> Unit, onUnderline: () -> Unit, onStrike: () -> Unit,
    onColor: () -> Unit, onSize: () -> Unit,
    onParagraphStyle: (ParagraphStyle) -> Unit,
    onQuote: () -> Unit, onBullet: () -> Unit, onNumbered: () -> Unit, onCheckbox: () -> Unit,
    onCode: () -> Unit, onLink: () -> Unit, onFind: () -> Unit,
    onImage: () -> Unit,
    onAlign: (ParagraphAlignment) -> Unit,
    onIndent: (Int) -> Unit,
    onUndo: () -> Unit, onRedo: () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    var headingMenuExpanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }

    // Active-state predicates — derived from the cursor's span style (incl. armed sticky
    // overrides) and the focused paragraph's paragraph-level state.
    val spanStyle = state.styleAtCursor
    val boldToggled = spanStyle?.fontWeight == FontWeight.Bold
    val italicToggled = spanStyle?.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic
    val underlineToggled = spanStyle?.textDecoration?.contains(TextDecoration.Underline) == true
    val strikeToggled = spanStyle?.textDecoration?.contains(TextDecoration.LineThrough) == true
    val pStyle = state.paragraphStyleAtCursor
    val pAlign = state.paragraphAlignmentAtCursor
    val indentLevel = state.indentLevelAtCursor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .background(Brush.verticalGradient(listOf(Color.Transparent, bgColor.copy(alpha = 0.85f), bgColor)))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor.copy(alpha = 0.5f)))

        // ── "More" tier: expands ABOVE the primary row ──
        AnimatedVisibility(visible = moreExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = NkSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Fmt(Icons.Filled.FormatColorText, R.string.editor_format_color, onColor)
                Fmt(Icons.Filled.FormatSize, R.string.editor_format_size, onSize)
                Fmt(Icons.Filled.FormatQuote, R.string.editor_format_quote, onQuote, toggled = pStyle == ParagraphStyle.QUOTE)
                Fmt(Icons.Filled.Code, R.string.editor_format_code, onCode, toggled = state.codeAtCursor)
                Fmt(Icons.Filled.Link, R.string.editor_format_link, onLink, toggled = state.linkAtCursor)
                Fmt(Icons.Filled.Search, R.string.editor_format_find, onFind)
                Fmt(Icons.AutoMirrored.Filled.FormatAlignLeft, R.string.editor_align_left,
                    { onAlign(ParagraphAlignment.START) }, toggled = pAlign == ParagraphAlignment.START)
                Fmt(Icons.Filled.FormatAlignCenter, R.string.editor_align_center,
                    { onAlign(ParagraphAlignment.CENTER) }, toggled = pAlign == ParagraphAlignment.CENTER)
                Fmt(Icons.AutoMirrored.Filled.FormatAlignRight, R.string.editor_align_right,
                    { onAlign(ParagraphAlignment.END) }, toggled = pAlign == ParagraphAlignment.END)
                Fmt(Icons.AutoMirrored.Filled.FormatIndentIncrease, R.string.editor_indent, { onIndent(+1) })
                Fmt(Icons.AutoMirrored.Filled.FormatIndentDecrease, R.string.editor_outdent, { onIndent(-1) }, enabled = indentLevel > 0)
                Fmt(Icons.Filled.Image, R.string.editor_insert_image, onImage)
                Spacer(Modifier.width(NkSpacing.sm))
                Text(
                    text = stringResource(R.string.editor_word_char_count, state.wordCount, state.charCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = NkSpacing.sm)
                )
            }
        }

        // ── Primary tier ──
        val primaryScroll = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(primaryScroll)
                    .padding(horizontal = NkSpacing.xs, vertical = NkSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo / redo lead the bar (also reachable via Ctrl+Z / Ctrl+Shift+Z on a HW keyboard).
                Fmt(Icons.AutoMirrored.Filled.Undo, R.string.common_undo, onUndo, enabled = state.canUndo)
                Fmt(Icons.AutoMirrored.Filled.Redo, R.string.common_redo, onRedo, enabled = state.canRedo)
                Fmt(Icons.Filled.FormatBold, R.string.editor_format_bold, onBold, toggled = boldToggled)
                Fmt(Icons.Filled.FormatItalic, R.string.editor_format_italic, onItalic, toggled = italicToggled)
                Fmt(Icons.Filled.FormatUnderlined, R.string.editor_format_underline, onUnderline, toggled = underlineToggled)
                Fmt(Icons.Filled.FormatStrikethrough, R.string.editor_format_strikethrough, onStrike, toggled = strikeToggled)

                // Heading hierarchy picker: Title / Heading / Subheading / Body.
                Box {
                    IconButton(onClick = { headingMenuExpanded = true }) {
                        Icon(
                            Icons.Filled.Title,
                            stringResource(R.string.editor_format_heading),
                            tint = if (pStyle == ParagraphStyle.TITLE || pStyle == ParagraphStyle.HEADING ||
                                pStyle == ParagraphStyle.SUBHEADING) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                            modifier = Modifier.size(NkIconSize.md)
                        )
                    }
                    DropdownMenu(expanded = headingMenuExpanded, onDismissRequest = { headingMenuExpanded = false }) {
                        HeadingItem(R.string.editor_format_title, ParagraphStyle.TITLE, pStyle) {
                            onParagraphStyle(ParagraphStyle.TITLE); headingMenuExpanded = false
                        }
                        HeadingItem(R.string.editor_format_heading, ParagraphStyle.HEADING, pStyle) {
                            onParagraphStyle(ParagraphStyle.HEADING); headingMenuExpanded = false
                        }
                        HeadingItem(R.string.editor_format_subheading, ParagraphStyle.SUBHEADING, pStyle) {
                            onParagraphStyle(ParagraphStyle.SUBHEADING); headingMenuExpanded = false
                        }
                        HeadingItem(R.string.editor_format_body, ParagraphStyle.BODY, pStyle) {
                            onParagraphStyle(ParagraphStyle.BODY); headingMenuExpanded = false
                        }
                    }
                }

                Fmt(Icons.AutoMirrored.Filled.FormatListBulleted, R.string.editor_format_bullet, onBullet, toggled = pStyle == ParagraphStyle.BULLET)
                Fmt(Icons.Filled.FormatListNumbered, R.string.editor_format_numbered, onNumbered, toggled = pStyle == ParagraphStyle.NUMBERED)
                Fmt(Icons.Filled.CheckBox, R.string.editor_format_checkbox, onCheckbox, toggled = pStyle == ParagraphStyle.CHECKBOX)

                val moreRotation by animateFloatAsState(targetValue = if (moreExpanded) 180f else 0f, label = "more_arrow")
                IconButton(onClick = { moreExpanded = !moreExpanded }) {
                    Icon(
                        Icons.Filled.ExpandLess,
                        contentDescription = stringResource(R.string.editor_format_more),
                        tint = if (moreExpanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(NkIconSize.md).rotate(moreRotation)
                    )
                }
            }
            // Scroll-edge fade hints for narrow screens.
            if (primaryScroll.maxValue > 0 && primaryScroll.value < primaryScroll.maxValue) {
                Box(modifier = Modifier.width(20.dp).height(48.dp).align(Alignment.CenterEnd)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, bgColor))))
            }
            if (primaryScroll.value > 0) {
                Box(modifier = Modifier.width(20.dp).height(48.dp).align(Alignment.CenterStart)
                    .background(Brush.horizontalGradient(listOf(bgColor, Color.Transparent))))
            }
        }
    }
}

/** Heading dropdown item. Shows a leading check when its style is the active paragraph style. */
@Composable
private fun HeadingItem(labelRes: Int, style: ParagraphStyle, current: ParagraphStyle, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(labelRes)) }, onClick = onClick, leadingIcon = {
        if (style == current) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    })
}

@Composable
private fun Fmt(
    icon: ImageVector,
    desc: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    toggled: Boolean = false
) {
    // Active-state highlighting (todo 10): when the cursor's span/paragraph matches this affordance,
    // the icon uses the primary color (filled/primary-toned) instead of the muted onSurfaceVariant.
    val tint = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    } else if (toggled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, stringResource(desc), tint = tint, modifier = Modifier.size(NkIconSize.md))
    }
}

// ── Themed date picker using NkDialog ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NkDatePicker(initial: String, onDate: (String) -> Unit, onDismiss: () -> Unit) {
    val initialEpoch = runCatching {
        LocalDate.parse(initial).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
    NkDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.editor_select_date),
        confirmText = stringResource(R.string.common_confirm),
        onConfirm = {
            state.selectedDateMillis?.let {
                val d = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                onDate(d.toString())
            } ?: onDismiss()
        },
        dismissText = stringResource(R.string.common_cancel)
    ) {
        DatePicker(state = state)
    }
}