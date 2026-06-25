package io.github.nknote.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.appContainer
import io.github.nknote.model.Mood
import io.github.nknote.model.ParagraphStyle
import io.github.nknote.model.Weather
import io.github.nknote.ui.components.ColorPickerDialog
import io.github.nknote.ui.components.FontSizePickerDialog
import io.github.nknote.ui.components.MoodPickerDialog
import io.github.nknote.ui.components.WeatherPickerDialog
import io.github.nknote.ui.editor.richtext.SpanVisualTransformation
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPage(
    noteId: Int?,
    nav: NkNoteNavigation,
    viewModel: EditorViewModel = viewModel(
        factory = AppViewModelFactory.editorFactory(
            noteId ?: -1,
            LocalContext.current.appContainer().imageStore,
            LocalContext.current.appContainer().noteRepository
        )
    )
) {
    var showDate by remember { mutableStateOf(false) }
    var showWeather by remember { mutableStateOf(false) }
    var showMood by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }
    var showFontSize by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }

    val weatherLabel = Weather.fromKey(viewModel.weatherKey)?.let { stringResource(it.labelRes) }.orEmpty()
    val moodLabel = Mood.fromKey(viewModel.moodKey)?.let { stringResource(it.labelRes) }.orEmpty()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.insertImageAfter(viewModel.focusedIndex, uri)
    }

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    EditorTopBar(
                        title = viewModel.title,
                        onTitleChange = { viewModel.updateTitle(it) },
                        onBack = nav.back,
                        onSave = { viewModel.save { nav.toHomeAndClear() } },
                        detailsExpanded = detailsExpanded,
                        onToggleDetails = { detailsExpanded = !detailsExpanded }
                    )
                    AnimatedVisibility(visible = detailsExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        MetaPanel(
                            title = viewModel.title,
                            onTitle = { viewModel.updateTitle(it) },
                            description = viewModel.description,
                            onDescription = { viewModel.updateDescription(it) },
                            weatherLabel = weatherLabel,
                            moodLabel = moodLabel,
                            date = viewModel.date,
                            onPickWeather = { showWeather = true },
                            onPickMood = { showMood = true },
                            onPickDate = { showDate = true }
                        )
                    }
                }
            },
            bottomBar = {
                FormatBar(
                    onBold = { viewModel.toggleBold() },
                    onItalic = { viewModel.toggleItalic() },
                    onUnderline = { viewModel.toggleUnderline() },
                    onStrike = { viewModel.toggleStrikethrough() },
                    onColor = { showColor = true },
                    onSize = { showFontSize = true },
                    onHeading = { viewModel.setParagraphStyle(viewModel.focusedIndex, ParagraphStyle.HEADING) },
                    onQuote = { viewModel.setParagraphStyle(viewModel.focusedIndex, ParagraphStyle.QUOTE) },
                    onBullet = { viewModel.setParagraphStyle(viewModel.focusedIndex, ParagraphStyle.BULLET) },
                    onImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
            }
        ) { padding ->
            EditorContent(viewModel = viewModel, nav = nav, padding = padding)
        }
    }

    if (showDate) NkDatePicker(initial = viewModel.date, onDate = { viewModel.updateDate(it); showDate = false }, onDismiss = { showDate = false })
    if (showWeather) WeatherPickerDialog(onPick = { viewModel.updateWeather(it); showWeather = false }, onDismiss = { showWeather = false })
    if (showMood) MoodPickerDialog(onPick = { viewModel.updateMood(it); showMood = false }, onDismiss = { showMood = false })
    if (showColor) ColorPickerDialog(onPick = { viewModel.setColor(it); showColor = false }, onDismiss = { showColor = false })
    if (showFontSize) FontSizePickerDialog(onPick = { viewModel.setFontSizeScale(it); showFontSize = false }, onDismiss = { showFontSize = false })
}

// ── Top bar with collapsible detail toggle ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleDetails) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.editor_select_date),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            }
        },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        actions = {
            IconButton(onClick = onSave) { Icon(Icons.Filled.Check, stringResource(R.string.editor_save)) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

// ── Collapsed-by-default meta panel (title, desc, weather, mood, date) ──────────

@Composable
private fun MetaPanel(
    title: String,
    onTitle: (String) -> Unit,
    description: String,
    onDescription: (String) -> Unit,
    weatherLabel: String,
    moodLabel: String,
    date: String,
    onPickWeather: () -> Unit,
    onPickMood: () -> Unit,
    onPickDate: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitle,
                singleLine = true,
                label = { Text(stringResource(R.string.editor_title_placeholder)) },
                textStyle = MaterialTheme.typography.titleLarge,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescription,
                placeholder = { Text(stringResource(R.string.editor_description_placeholder)) },
                label = { Text(stringResource(R.string.editor_description)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(text = weatherLabel, fallback = stringResource(R.string.editor_select_weather), onClick = onPickWeather)
                MetaChip(text = moodLabel, fallback = stringResource(R.string.editor_select_mood), onClick = onPickMood)
                MetaChip(text = date, fallback = stringResource(R.string.editor_select_date), onClick = onPickDate)
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, fallback: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), onClick = onClick) {
        Text(
            text = text.ifBlank { fallback },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// ── Editor content with per-paragraph focus management ─────────────────────────

@Composable
private fun EditorContent(
    viewModel: EditorViewModel,
    nav: NkNoteNavigation,
    padding: androidx.compose.foundation.layout.PaddingValues
) {
    // FocusRequester pool grows with the paragraph count
    val focusRequesters = remember { androidx.compose.runtime.mutableStateListOf<FocusRequester>() }
    while (focusRequesters.size < viewModel.paragraphs.size) focusRequesters.add(FocusRequester())

    // After a split/merge, move focus to the requested paragraph
    LaunchedEffect(viewModel.pendingFocusIndex) {
        val idx = viewModel.pendingFocusIndex
        if (idx >= 0 && idx < focusRequesters.size) {
            runCatching { focusRequesters[idx].requestFocus() }
            viewModel.consumePendingFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        viewModel.paragraphs.forEachIndexed { index, para ->
            if (para.image != null) {
                ImageBlock(
                    path = para.image.path,
                    onClick = { nav.toViewer(para.image.path) },
                    onDelete = { viewModel.removeImageParagraph(index) }
                )
            } else {
                ParagraphField(
                    index = index,
                    viewModel = viewModel,
                    focusRequester = focusRequesters.getOrNull(index) ?: FocusRequester()
                )
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ParagraphField(
    index: Int,
    viewModel: EditorViewModel,
    focusRequester: FocusRequester
) {
    val para = viewModel.paragraphs[index]
    val value = viewModel.fields.getOrNull(index) ?: TextFieldValue("")
    val transformation = remember(para) { SpanVisualTransformation(para) }

    BasicTextField(
        value = value,
        onValueChange = { tv ->
            val nl = tv.text.indexOf('\n')
            if (nl >= 0) {
                viewModel.splitParagraph(index, tv.text.substring(0, nl), tv.text.substring(nl + 1))
            } else {
                viewModel.onTextChange(index, tv)
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
        visualTransformation = transformation,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                // Backspace at position 0 in a non-first paragraph → merge with previous
                if (keyEvent.type == KeyEventType.KeyUp &&
                    keyEvent.key == Key.Backspace &&
                    value.selection.start == 0 &&
                    value.selection.end == 0 &&
                    index > 0
                ) {
                    viewModel.mergeWithPrevious(index)
                    true
                } else false
            }
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun ImageBlock(path: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = path,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(240.dp)
        )
        IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            onClick = onClick,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        ) { Text(stringResource(R.string.viewer_title), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp)) }
    }
}

// ── Soft format bar with scroll-edge fade hint ──────────────────────────────────

@Composable
private fun FormatBar(
    onBold: () -> Unit, onItalic: () -> Unit, onUnderline: () -> Unit, onStrike: () -> Unit,
    onColor: () -> Unit, onSize: () -> Unit, onHeading: () -> Unit, onQuote: () -> Unit,
    onBullet: () -> Unit, onImage: () -> Unit
) {
    val scrollState = rememberScrollState()
    val bgColor = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .background(bgColor)
    ) {
        // Subtle separator — soft, not a hard surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(borderColor.copy(alpha = 0.5f))
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Fmt(Icons.Filled.FormatBold, R.string.editor_format_bold, onBold)
                Fmt(Icons.Filled.FormatItalic, R.string.editor_format_italic, onItalic)
                Fmt(Icons.Filled.FormatUnderlined, R.string.editor_format_underline, onUnderline)
                Fmt(Icons.Filled.FormatStrikethrough, R.string.editor_format_strikethrough, onStrike)
                Fmt(Icons.Filled.FormatColorText, R.string.editor_format_color, onColor)
                Fmt(Icons.Filled.FormatSize, R.string.editor_format_size, onSize)
                Fmt(Icons.Filled.Title, R.string.editor_format_heading, onHeading)
                Fmt(Icons.Filled.FormatQuote, R.string.editor_format_quote, onQuote)
                Fmt(Icons.AutoMirrored.Filled.FormatListBulleted, R.string.editor_format_bullet, onBullet)
                Fmt(Icons.Filled.Image, R.string.editor_insert_image, onImage)
            }
            // Edge fade hint — subtle gradient showing scrollability
            if (scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(48.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, bgColor)
                            )
                        )
                )
            }
            if (scrollState.value > 0) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(48.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                listOf(bgColor, Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun Fmt(icon: ImageVector, desc: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            stringResource(desc),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NkDatePicker(initial: String, onDate: (String) -> Unit, onDismiss: () -> Unit) {
    val initialEpoch = runCatching {
        LocalDate.parse(initial).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    val d = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    onDate(d.toString())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    ) { DatePicker(state = state) }
}