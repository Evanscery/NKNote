package com.example.nknote.ui.components

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nknote.R
import com.example.nknote.ui.theme.NKNoteTheme
import github.leavesczy.matisse.FileProviderCaptureStrategy
import github.leavesczy.matisse.GlideImageEngine
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MatisseCapture
import github.leavesczy.matisse.MatisseContract
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.SmartCaptureStrategy
import jp.wasabeef.richeditor.RichEditor
import java.net.URI
import java.time.LocalDateTime

/*
*   TODO
*    Transplant the rich text editor base on view into compose
*   source project:https://github.com/wasabeef/richeditor-android
*
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NKRichTextEditor(
    /*TODO
    *  Para list*/
    inputEnabled: Boolean = true,
    defaultBackgroundColor: Int = MaterialTheme.colorScheme.secondary.toArgb(),
    defaultEditorFontColor: Int = MaterialTheme.colorScheme.onSecondary.toArgb(),
    defaultEditorFontSize: Int = 25,
    modifier: Modifier = Modifier
) {

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

        //state variables
        var fontSize: Int by remember { mutableStateOf(3) }

        var insertedImageUri : String by remember{ mutableStateOf("")}
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

        Scaffold(
            content = {
                it
                val keyboardController = LocalSoftwareKeyboardController.current
                Column(modifier = Modifier
                    .fillMaxSize(),
                    verticalArrangement = Arrangement.Top

                ) {

                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        factory = { context ->
                            RichEditor(context).apply {
                                setPadding(10, 10, 10, 10)
                                setEditorHeight(height)
                                setEditorFontSize(defaultEditorFontSize)
                                setEditorFontColor(defaultEditorFontColor)
                                setEditorBackgroundColor(defaultBackgroundColor)
                                setPlaceholder("insert here")
                                setInputEnabled(inputEnabled)
                            }

                        },
                        update = {
                            //undo
                            if (setFocusEditor) {
                                setFocusEditor = false
                                it.focusEditor()
                                keyboardController?.show()
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

                                    it.insertImage(insertedImageUri,"dach",320)
                                insertedImageUri = ""
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
                            IconButton(onClick = { /*TODO*/ }
                            ) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "")

                            }
                        }


                    }


                }
            },
            modifier = Modifier.fillMaxSize(),
        )

    }

}

@Preview
@Composable
fun NKRichTextEditorPreview() {
    NKRichTextEditor()
}