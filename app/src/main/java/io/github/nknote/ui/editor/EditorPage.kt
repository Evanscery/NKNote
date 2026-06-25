package io.github.nknote.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = viewModel.title,
                                onValueChange = { viewModel.updateTitle(it) },
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.editor_title_placeholder)) },
                                textStyle = MaterialTheme.typography.titleLarge,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = { IconButton(onClick = nav.back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                        actions = {
                            IconButton(onClick = { viewModel.save { nav.toHomeAndClear() } }) {
                                Icon(Icons.Filled.Check, stringResource(R.string.editor_save))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                    AnimatedVisibility(visible = detailsExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        MetaPanel(
                            description = viewModel.description,
                            onDescription = { viewModel.updateDescription(it) },
                            weatherLabel = weatherLabel,
                            moodLabel = moodLabel,
                            date = viewModel.date,
                            onToggle = { detailsExpanded = !detailsExpanded },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = viewModel.date,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                viewModel.paragraphs.forEachIndexed { index, para ->
                    if (para.image != null) {
                        ImageBlock(
                            path = para.image.path,
                            onClick = { nav.toViewer(para.image.path) },
                            onDelete = { viewModel.removeImageParagraph(index) }
                        )
                    } else {
                        ParagraphField(index = index, viewModel = viewModel)
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (showDate) NkDatePicker(initial = viewModel.date, onDate = { viewModel.updateDate(it); showDate = false }, onDismiss = { showDate = false })
    if (showWeather) WeatherPickerDialog(onPick = { viewModel.updateWeather(it); showWeather = false }, onDismiss = { showWeather = false })
    if (showMood) MoodPickerDialog(onPick = { viewModel.updateMood(it); showMood = false }, onDismiss = { showMood = false })
    if (showColor) ColorPickerDialog(onPick = { viewModel.setColor(it); showColor = false }, onDismiss = { showColor = false })
    if (showFontSize) FontSizePickerDialog(onPick = { viewModel.setFontSizeScale(it); showFontSize = false }, onDismiss = { showFontSize = false })
}

@Composable
private fun MetaPanel(
    description: String,
    onDescription: (String) -> Unit,
    weatherLabel: String,
    moodLabel: String,
    date: String,
    onToggle: () -> Unit,
    onPickWeather: () -> Unit,
    onPickMood: () -> Unit,
    onPickDate: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = description,
                onValueChange = onDescription,
                placeholder = { Text(stringResource(R.string.editor_description_placeholder)) },
                label = { Text(stringResource(R.string.editor_description)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
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

@Composable
private fun ParagraphField(index: Int, viewModel: EditorViewModel) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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

@Composable
private fun FormatBar(
    onBold: () -> Unit, onItalic: () -> Unit, onUnderline: () -> Unit, onStrike: () -> Unit,
    onColor: () -> Unit, onSize: () -> Unit, onHeading: () -> Unit, onQuote: () -> Unit,
    onBullet: () -> Unit, onImage: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(4.dp))
            Fmt(Icons.Filled.FormatBold, R.string.editor_format_bold, onBold)
            Fmt(Icons.Filled.FormatItalic, R.string.editor_format_italic, onItalic)
            Fmt(Icons.Filled.FormatUnderlined, R.string.editor_format_underline, onUnderline)
            Fmt(Icons.Filled.FormatStrikethrough, R.string.editor_format_strikethrough, onStrike)
            Fmt(Icons.Filled.FormatColorText, R.string.editor_format_color, onColor)
            Fmt(Icons.Filled.FormatSize, R.string.editor_format_size, onSize)
            Fmt(Icons.Filled.Title, R.string.editor_format_heading, onHeading)
            Fmt(Icons.Filled.FormatQuote, R.string.editor_format_quote, onQuote)
            Fmt(Icons.Filled.FormatListBulleted, R.string.editor_format_bullet, onBullet)
            Fmt(Icons.Filled.Image, R.string.editor_insert_image, onImage)
            Spacer(Modifier.size(4.dp))
        }
    }
}

@Composable
private fun Fmt(icon: ImageVector, desc: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, stringResource(desc), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
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