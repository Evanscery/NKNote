package com.example.nknote.ui.pages

import GlideEngine
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nknote.AppViewModelProvider
import com.example.nknote.ImageViewerActivity
import com.example.nknote.R
import com.example.nknote.databinding.NoteRichEditorLayoutBinding
import com.example.nknote.ui.components.ColorPickerDialog
import com.example.nknote.ui.components.FontSizeDialog
import com.example.nknote.ui.components.SimpleTextField
import com.example.nknote.ui.theme.NKNoteTheme
import com.google.accompanist.flowlayout.FlowRow
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.nknote.ui.components.DatePickerDialog
import com.example.nknote.ui.components.WeatherPickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NoteEditPage(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteEditViewModel = viewModel(factory = AppViewModelProvider.Factory(LocalContext.current))
) {
    val defaultBackgroundColor: Int = MaterialTheme.colorScheme.surface.toArgb()
    val defaultEditorFontColor: Int = MaterialTheme.colorScheme.onSurface.toArgb()
    val defaultEditorFontSize: Int = 18
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 天气选择对话框状态
    var showWeatherDialog by remember { mutableStateOf(false) }
    // 日期选择对话框状态
    var showDateDialog by remember { mutableStateOf(false) }
    
    // 下拉箭头动画
    val rotation by animateFloatAsState(
        targetValue = if (viewModel.isDetailsExpanded) 180f else 0f,
        label = "dropdown_rotation"
    )

    NKNoteTheme {
        //signal for activate action
        var insertImageActivated: Boolean by remember { mutableStateOf(false) }
        var onSaveNote: Boolean by remember { mutableStateOf(false) }

        val imageViewerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra("action")?.let { action ->
                    when (action) {
                        "delete" -> {
                            val imageUrl = result.data?.getStringExtra("imageUrl")
                            // 从富文本中删除该图片
                            viewModel.editorRef?.let { editor ->
                                val pattern =
                                    """<div class="image-wrapper" style="display:block; text-align:center; margin:10px 0;" onclick="javascript:showImage\('${
                                        imageUrl.toString().replace("/", "\\/")
                                    }'[^>]*\)">[\s\S]*?</div>[\s\n]*""".toRegex()
                                editor.html = editor.html?.replace(pattern, "")
                            }
                            // 从外部存储中删除压缩后的图片
                            viewModel.deleteImage(imageUrl ?: "")
                        }
                        else -> {
                            //TODO: OTHER ACTIONS
                        }
                    }
                }
            }
        }

        //set up matisse for pick up image from media folder
        val mediaPickerLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                viewModel.handleImageSelection(context, result)
            }

        // 处理图片插入
        LaunchedEffect(viewModel.insertedImageUri) {
            if (viewModel.insertedImageUri.isNotEmpty()) {
                val imageHtml = """
                    <div class="image-wrapper" style="display:block; text-align:center; margin:10px 0;" onclick="javascript:showImage('${viewModel.insertedImageUri}')">
                        <img src="${viewModel.insertedImageUri}" style="width:80vw; height:auto;" alt="" />
                    </div>
                    <p><br></p>
                """.trimIndent()
                viewModel.editorRef?.let { editor ->
                    editor.html = "${editor.html.orEmpty()}${imageHtml}"
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        // 顶部工具栏
                        TopAppBar(
                            title = {
                                SimpleTextField(
                                    value = viewModel.title,
                                    onValueChange = { viewModel.updateTitle(it) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    placeholderText = stringResource(id = R.string.notepage_topbar_title_placerholder_chs),
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            },
                            actions = {
                                // 日期显示
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd").format(Date()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { onSaveNote = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            },
                            colors = TopAppBarColors(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.onPrimary,
                                MaterialTheme.colorScheme.onPrimary,
                                MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        // 下拉详情区域
                        AnimatedVisibility(
                            visible = viewModel.isDetailsExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // 描述输入框
                                    OutlinedTextField(
                                        value = viewModel.description,
                                        onValueChange = { 
                                            if (it.count { char -> char == '\n' } < 10) {
                                                viewModel.updateDescription(it)
                                            }
                                        },
                                        label = { Text("简介", color = MaterialTheme.colorScheme.onPrimary) },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .heightIn(max = 200.dp)
                                    )

                                    // 标签区域
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "标签",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        
                                        // 标签输入框
                                        var tagInput by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = tagInput,
                                            onValueChange = { tagInput = it },
                                            placeholder = { Text("输入标签后按回车添加", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)) },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                cursorColor = MaterialTheme.colorScheme.onPrimary,
                                                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            singleLine = true,
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    if (tagInput.isNotBlank()) {
                                                        val newTags = if (viewModel.tags.isBlank()) {
                                                            tagInput
                                                        } else {
                                                            "${viewModel.tags},${tagInput}"
                                                        }
                                                        viewModel.updateTags(newTags)
                                                        tagInput = ""
                                                    }
                                                }
                                            )
                                        )
                                        
                                        // 显示已添加的标签
                                        FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            viewModel.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    shape = RoundedCornerShape(16.dp),
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = tag.trim(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSecondary
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = {
                                                                val updatedTags = viewModel.tags.split(",")
                                                                    .filter { it.trim() != tag.trim() }
                                                                    .joinToString(",")
                                                                viewModel.updateTags(updatedTags)
                                                            },
                                                            modifier = Modifier.size(16.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "删除标签",
                                                                tint = MaterialTheme.colorScheme.onSecondary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 天气和日期选择区域
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 天气选择
                                        OutlinedButton(
                                            onClick = { showWeatherDialog = true },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)
                                        ) {
                                            Icon(
                                                bitmap = ImageBitmap.imageResource(id = R.drawable.placeholder),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = viewModel.weather.ifEmpty { "选择天气" },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // 日期选择
                                        OutlinedButton(
                                            onClick = { showDateDialog = true },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)
                                        ) {
                                            Icon(
                                                bitmap = ImageBitmap.imageResource(id = R.drawable.placeholder),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = viewModel.date.ifEmpty { "选择日期" },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 下拉按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleDetailsExpanded() },
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "展开/收起详情",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.rotate(rotation)
                                )
                            }
                        }
                    }
                }
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                )
                {
                    AndroidViewBinding(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding()
                            ),
                        factory = NoteRichEditorLayoutBinding::inflate,
                        update = {
                            viewModel.setEditorRef(editor)
                            if (!viewModel.hasInitialEditor) {
                                viewModel.initializeEditor(
                                    defaultFontSize = defaultEditorFontSize,
                                    defaultFontColor = defaultEditorFontColor,
                                    defaultBackgroundColor = defaultBackgroundColor
                                )
                                editor.addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun showImage(url: String) {
                                            val intent = Intent(
                                                context,
                                                ImageViewerActivity::class.java
                                            ).apply {
                                                putExtra("imageUrl", url)
                                            }
                                            imageViewerLauncher.launch(intent)
                                        }
                                    },
                                    "Android"
                                )

                                // 在编辑器加载完成后注入JavaScript函数
                                editor.setOnInitialLoadListener {
                                    editor.evaluateJavascript(
                                        """
                function showImage(url) {
                    Android.showImage(url);
                }
            """.trimIndent(), null
                                    )
                                }
                            }
                            //save note and back
                            if (onSaveNote) {
                                onSaveNote = false
                                scope.launch {
                                    viewModel.saveNote()
                                }
                                onNavigateBack()
                            }
                        },
                    )
                }
                FontSizeDialog(
                    showDialog = viewModel.showFontSizeDialog,
                    currentSize = viewModel.fontSize,
                    onDismiss = { viewModel.hideFontSizeDialog() },
                    onConfirm = { newSize ->
                        viewModel.setFontSize(newSize)
                        viewModel.hideFontSizeDialog()
                    }
                )
                ColorPickerDialog(
                    showDialog = viewModel.showColorPickerDialog,
                    onDismiss = { viewModel.hideColorPicker() },
                    onColorSelected = { color ->
                        viewModel.setTextColor(color.toArgb())
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .fillMaxWidth(),
                        ) {
                            //Button for Undo
                            IconButton(onClick = {
                                viewModel.editorRef?.undo()
                            }
                            ) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.undo),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            //Button for Redo
                            IconButton(onClick = {
                                viewModel.editorRef?.redo()
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.redo),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            //Button for set bold
                            IconButton(onClick = {
                                viewModel.editorRef?.setBold()
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.bold),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            //Button for font size change
                            IconButton(onClick = {
                                viewModel.showFontSizeDialog()
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.font_size),
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = ""
                                )
                            }
                            //Button for insert picture
                            IconButton(onClick = {
                                PictureSelector.create(context)
                                    .openGallery(SelectMimeType.ofImage())
                                    .setImageEngine(GlideEngine.createGlideEngine())
                                    .setMaxSelectNum(1)
                                    .setCompressEngine(viewModel.getImageCompressEngine(context))
                                    .forResult(mediaPickerLauncher)
                                insertImageActivated = true
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.insert_image),
                                    contentDescription = "",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            //Button for set italic
                            IconButton(onClick = {
                                viewModel.editorRef?.setItalic()
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.italic),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            //Button for set font color
                            IconButton(onClick = {
                                viewModel.showColorPicker()
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.font_color),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                keyboardController?.hide()
                            }
                            ) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "")
                            }
                        }
                    }
                }
            }
        )
    }

    // 天气选择对话框
    if (showWeatherDialog) {
        WeatherPickerDialog(
            onDismiss = { showWeatherDialog = false },
            onWeatherSelected = { weather ->
                viewModel.updateWeather(weather)
            },
            currentWeather = viewModel.weather
        )
    }

    // 日期选择对话框
    if (showDateDialog) {
        DatePickerDialog(
            onDismiss = { showDateDialog = false },
            onDateSelected = { date ->
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                viewModel.updateDate(date.format(formatter))
            }
        )
    }
}

@Preview
@Composable
@SuppressLint("SetJavaScriptEnabled")
fun NoteEditPagePreview() {
    NoteEditPage({})
}

