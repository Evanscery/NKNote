package com.example.nknote.ui.pages


import ando.file.compressor.ImageCompressEngine
import ando.file.core.FileOperator.getApplication
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nknote.AppViewModelProvider
import com.example.nknote.R
import com.example.nknote.data.DataHandler
import com.example.nknote.data.DataHandler.stringWithMD5
import com.example.nknote.databinding.NoteRichEditorLayoutBinding
import com.example.nknote.ui.components.SimpleTextField
import com.example.nknote.ui.theme.NKNoteTheme
import github.leavesczy.matisse.GlideImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.SmartCaptureStrategy
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NoteEditPage(onNavToHomePage:()->Unit,
                 viewModel: NoteEditViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val defaultBackgroundColor: Int = MaterialTheme.colorScheme.surface.toArgb()
    val defaultEditorFontColor: Int = MaterialTheme.colorScheme.onSurface.toArgb()
    val defaultEditorFontSize: Int = 25
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    NKNoteTheme {
        //signal for activate action
        var setFocusEditor: Boolean by remember { mutableStateOf(false) }
        var setBoldActivated: Boolean by remember { mutableStateOf(false) }
        var undoActivated: Boolean by remember { mutableStateOf(false) }
        var redoActivated: Boolean by remember { mutableStateOf(false) }
        var fontSizeChangeActivated: Boolean by remember { mutableStateOf(false) }
        var setItalicActivated: Boolean by remember { mutableStateOf(false) }
        var insertImageActivated: Boolean by remember { mutableStateOf(false) }
        var setFontColorActivated : Boolean by remember { mutableStateOf(false)}
        var navToHomePageClicked : Boolean by remember{ mutableStateOf(false)}
        var hasInitialEditor : Boolean by rememberSaveable{ mutableStateOf(false)}
        var onSaveNote : Boolean by remember{ mutableStateOf(false)}

        //variables that should be save
        var fontSize: Int by remember { mutableStateOf(3) }
        var insertedImageUri : String by remember{ mutableStateOf("")}
        val insertedImagesMap : SnapshotStateMap<String,String> = remember {
            mutableStateMapOf()
        }
        //set up matisse for pick up image from media folder
        val mediaPickerLauncher =
            rememberLauncherForActivityResult(contract = MatisseContract()) { result: List<MediaResource>? ->
                if (!result.isNullOrEmpty()) {
                    insertImageActivated = false
                    val mediaResource = result[0]
                    insertedImageUri = mediaResource.uri.toString()
                    Log.d("asd","type:${mediaResource.mimeType}")
                }
            }

        val matisse = Matisse(
            maxSelectable = 1,
            imageEngine = GlideImageEngine(),
            mediaType = MediaType.ImageOnly,
            captureStrategy = SmartCaptureStrategy("com.NKNote.provider.image_provider")
        )
        if(!insertedImageUri.isNullOrEmpty())
        {
            val insertedImageBitmap: Bitmap? = ImageCompressEngine.compressPure(Uri.parse(insertedImageUri))
            insertedImagesMap.put(insertedImageUri,DataHandler.bitmapToString(insertedImageBitmap as Bitmap) as String)
        }
        val defaultTitle = stringResource(id = R.string.notepage_topbar_title_default_chs)
        var titleInput by remember{ mutableStateOf(defaultTitle)}
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = {
                Spacer(modifier = Modifier.statusBarsPadding())
                TopAppBar(title = {
                SimpleTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    singleLine = true,
                    placeholderText = stringResource(id = R.string.notepage_topbar_title_placerholder_chs),
                )
            },
                navigationIcon = {
                    //Here to exit edit page
                    IconButton(onClick = {
                        navToHomePageClicked = true
                        onNavToHomePage()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        /*TODO*/
                        scope.launch {
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = {
                        //send signal of saving note
                        onSaveNote = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null
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
            )},
            content = {
                paddingValues->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            state = rememberScrollState(),
                            reverseScrolling = true
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    keyboardController?.show()
                                    setFocusEditor = true
                                }
                            )
                        },
                    verticalArrangement = Arrangement.Top,
                )
                {
                    AndroidViewBinding(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding()
                            ),
                        factory = NoteRichEditorLayoutBinding::inflate ,
                        update = {
                            if(!hasInitialEditor)
                            {
                                hasInitialEditor = true
                                editor.setEditorFontSize(defaultEditorFontSize)
                                editor.setEditorFontColor(defaultEditorFontColor)
                                editor.setEditorBackgroundColor(defaultBackgroundColor)
                                editor.setInputEnabled(true)
                            }
                            //focus on editor
                            if (setFocusEditor) {
                                setFocusEditor = false
                                editor.focusEditor()
                                val inputMethodManager :InputMethodManager = editor.getContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                inputMethodManager.showSoftInput(editor,0)
                            }
                            //undo
                            if (undoActivated) {
                                undoActivated = false
                                editor.undo()
                            }
                            //redo
                            if (redoActivated) {
                                redoActivated = false
                                editor.redo()
                            }
                            //set bold
                            if (setBoldActivated) {
                                setBoldActivated = false
                                editor.setBold()
                            }
                            //font add
                            if (fontSizeChangeActivated) {
                                fontSize = fontSize % 7 + 1
                                fontSizeChangeActivated = false
                                editor.setFontSize(fontSize)
                            }
                            //set italic
                            if (setItalicActivated) {
                                setItalicActivated = false
                                editor.setItalic()
                            }
                            //insert image
                            if (!insertedImageUri.isNullOrEmpty()) {
                                editor.insertImage(insertedImageUri+"\" style=\\\"width:100%;", "",editor.width )
                                insertedImageUri = ""
                            }
                            //navigate back to home page
                            if(navToHomePageClicked)
                            {
                                navToHomePageClicked = false
                                onNavToHomePage()
                            }
                            //save note and back
                            if(onSaveNote)
                            {
                                onSaveNote = false
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                val currentDate = sdf.format(Date())
                                //create byte array using title + localtime , for example: "Today is so bad 2024-07-29 11:13:59"
                                val md5ID = stringWithMD5("${titleInput} ${currentDate}")
                                viewModel.updateUiState(
                                    NoteItemDetails(
                                        id = md5ID,
                                        title = titleInput,
                                        textHtml = editor.html ?: "",
                                        picture = insertedImagesMap.toMap(),
                                        date = currentDate,
                                )
                                )
                                insertedImagesMap.clear()
                                scope.launch {
                                    viewModel.insertNote()
                                }
                                onNavToHomePage()
                            }
                        },
                    )
                }
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
                                undoActivated = true
                            }
                            ) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.undo),
                                    contentDescription = ""
                                )
                            }
                            //Button for Redo
                            IconButton(onClick = {
                                redoActivated = true
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.redo),
                                    contentDescription = ""
                                )
                            }
                            //Button for set bold
                            IconButton(onClick = {
                                setBoldActivated = true
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.bold),
                                    contentDescription = ""
                                )
                            }
                            //Button for font size change
                            IconButton(onClick = {
                                fontSizeChangeActivated = true
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.font_add),
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = ""
                                )
                            }
                            //Button for insert picture
                            IconButton(onClick = {
                                insertImageActivated = true
                                mediaPickerLauncher.launch(matisse)
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.insert_image),
                                    contentDescription = ""
                                )
                            }
                            //Button for set italic
                            IconButton(onClick = {
                                setItalicActivated = true
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.italic),
                                    contentDescription = ""
                                )
                            }
                            //Button for set font color
                            IconButton(onClick = {
                                setFontColorActivated = true
                            }) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.font_color),
                                    contentDescription = ""
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
}


@Preview
@Composable
@SuppressLint("SetJavaScriptEnabled")
fun NoteEditPagePreview() {
    NoteEditPage({})
}

