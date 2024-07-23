package com.example.nknote.ui.pages


import android.graphics.Color
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.navigation.NavController
import com.example.nknote.R
import com.example.nknote.ui.components.NKRichTextEditor
import com.example.nknote.ui.components.NKRichTextEditorPreview
import com.example.nknote.ui.components.SimpleTextField
import com.example.nknote.ui.navigation.Destinations
import com.example.nknote.ui.theme.NKNoteTheme
import github.leavesczy.matisse.GlideImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import jp.wasabeef.richeditor.RichEditor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditPage(onNavToHomePage:()->Unit) {
    val defaultBackgroundColor: Int = MaterialTheme.colorScheme.secondary.toArgb()
    val defaultEditorFontColor: Int = MaterialTheme.colorScheme.onSecondary.toArgb()
    val defaultEditorFontSize: Int = 25
    val keyboardController = LocalSoftwareKeyboardController.current
    //var viewSize = remember { mutableStateOf(0) }

    NKNoteTheme {
        //signal for activate action
        var setFocusEditor: Boolean by rememberSaveable { mutableStateOf(false) }
        var setBoldActivated: Boolean by rememberSaveable { mutableStateOf(false) }
        var undoActivated: Boolean by rememberSaveable { mutableStateOf(false) }
        var redoActivated: Boolean by rememberSaveable { mutableStateOf(false) }
        var fontSizeChangeActivated: Boolean by rememberSaveable { mutableStateOf(false) }
        var setItalicActivated: Boolean by rememberSaveable { mutableStateOf(false) }
        var insertImageActivated: Boolean by rememberSaveable { mutableStateOf(false) }
        var setFontColorActivated : Boolean by rememberSaveable { mutableStateOf(false)}
        var navToHomePageClicked : Boolean by rememberSaveable{ mutableStateOf(false)}
        var hasInitialEditorSize : Boolean by rememberSaveable{ mutableStateOf(false)}

        //state variables
        var fontSize: Int by rememberSaveable { mutableStateOf(3) }

        var insertedImageUri : String by rememberSaveable{ mutableStateOf("")}
        val mediaPickerLauncher =
            rememberLauncherForActivityResult(contract = MatisseContract()) { result: List<MediaResource>? ->
                if (!result.isNullOrEmpty()) {
                    insertImageActivated = false
                    val mediaResource = result[0]
                    insertedImageUri = mediaResource.uri.toString()
                }
            }

        val matisse = Matisse(
            maxSelectable = 1,
            imageEngine = GlideImageEngine(),
            mediaType = MediaType.ImageOnly
        )
        val defaultTitle = stringResource(id = R.string.notepage_topbar_title_default_chs)
        var titleInput by rememberSaveable{ mutableStateOf(defaultTitle)}
        IconButton(onClick = {  }) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null
            )
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .statusBarsPadding(),
            content = {
                it
                var viewSize = 0
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            viewSize = coordinates.size.height
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    keyboardController?.show()
                                    setFocusEditor = true
                                }
                            )
                        }
                    ,
                    verticalArrangement = Arrangement.Top
                )
                {
                    TopAppBar(title = {
                        SimpleTextField(value = titleInput,
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
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null
                                )
                            }
                            IconButton(onClick = {
                                /*TODO*/
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
                    )
                    AndroidView(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .captionBarPadding(),
                        factory = { context ->
                            RichEditor(context).apply {
                                Log.d("asd","height:$viewSize")
                                setEditorHeight(viewSize)
                                setEditorFontSize(defaultEditorFontSize)
                                setEditorFontColor(defaultEditorFontColor)
                                setEditorBackgroundColor(defaultBackgroundColor)
                                setPlaceholder("insert here")
                                setInputEnabled(true)
                            }

                        },
                        update = {
                            if(!hasInitialEditorSize&&viewSize>0)
                            {
                                it.setEditorHeight(viewSize)
                            }
                            //undo
                            if (setFocusEditor) {
                                setFocusEditor = false
                                it.focusEditor()

                            }

                            //undo
                            if (undoActivated) {
                                undoActivated = false
                                it.undo()
                            }

                            //redo
                            if (redoActivated) {
                                redoActivated = false
                                it.redo()
                            }
                            //set bold
                            if (setBoldActivated) {
                                setBoldActivated = false
                                it.setBold()
                            }
                            //font add
                            if (fontSizeChangeActivated) {
                                fontSize = fontSize % 7 + 1
                                fontSizeChangeActivated = false
                                it.setFontSize(fontSize)
                            }

                            if (setItalicActivated) {
                                setItalicActivated = false
                                it.setItalic()
                            }

                            //insert image
                            if (!insertedImageUri.isNullOrEmpty()) {

                                it.insertImage(insertedImageUri, "dach", 320)
                                insertedImageUri = ""
                            }

                            //navigate back to home page
                            if(navToHomePageClicked)
                            {
                                navToHomePageClicked = false
                                //it.destroy()
                                onNavToHomePage()
                            }
                        },
                    )
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .statusBarsPadding()
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
fun NoteEditPagePreview() {
    //NoteEditPage()
}

