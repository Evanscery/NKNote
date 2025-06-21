package com.example.nknote.ui.pages


import android.os.SystemClock
import android.provider.ContactsContract.CommonDataKinds.Note
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nknote.AppViewModelProvider
import com.example.nknote.databinding.NoteRichEditorLayoutBinding
import com.example.nknote.ui.components.SimpleTextField
import com.example.nknote.ui.theme.NKNoteTheme
import jp.wasabeef.richeditor.RichEditor
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import com.example.nknote.data.NoteItemRoomDatabase
import com.example.nknote.data.repository.NoteRepositoryImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCheckPage(
    onNavBack: () -> Unit,
    noteId: Int,
    modifier: Modifier = Modifier,
    viewModel: NoteCheckViewModel = viewModel(factory = AppViewModelProvider.Factory(LocalContext.current))
) {
    val uiState = viewModel.noteUiState.collectAsState()
    NoteDetails(
        noteCheckUiState = uiState.value,
        onNavBack = onNavBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetails(
    noteCheckUiState: NoteCheckUiState,
    defaultBackgroundColor: Int = MaterialTheme.colorScheme.surface.toArgb(),
    defaultEditorFontColor: Int = MaterialTheme.colorScheme.onSurface.toArgb(),
    defaultEditorFontSize: Int = 25,
    modifier: Modifier = Modifier,
    onNavBack : ()->Unit = {}
)
{
    var hasInitialEditor by remember{ mutableStateOf(false)}
    NKNoteTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = {
                Spacer(modifier = Modifier.statusBarsPadding())
                CenterAlignedTopAppBar(title = {
                    Text(text = noteCheckUiState.title,
                        style = MaterialTheme.typography.titleLarge)
                },
                    navigationIcon = {
                        //Here to exit edit page
                        IconButton(onClick = {
                            onNavBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                    },
                    colors = TopAppBarColors(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            content = {
                    paddingValues->
                var viewSize = 0
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            viewSize = coordinates.size.height
                        }
                        .verticalScroll(
                            state = rememberScrollState(),
                            reverseScrolling = true
                        ),
                    verticalArrangement = Arrangement.Top,
                )
                {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding()
                            ),
                        factory = {
                            context->
                            RichEditor(context).apply{
                                html = noteCheckUiState.content
                                setEditorFontSize(defaultEditorFontSize)
                                setEditorFontColor(defaultEditorFontColor)
                                setEditorBackgroundColor(defaultBackgroundColor)
                                setInputEnabled(false)
                            }
                        } ,
                        update = {
                            it.html = noteCheckUiState.content
                        },
                    )
                }
            }
        )
    }
}



