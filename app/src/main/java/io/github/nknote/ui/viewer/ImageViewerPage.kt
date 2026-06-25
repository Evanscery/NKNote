package io.github.nknote.ui.viewer

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import io.github.nknote.R
import io.github.nknote.appContainer
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerPage(imagePath: String, nav: NkNoteNavigation) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    val context = androidx.compose.ui.platform.LocalContext.current

    NkNoteTheme {
        Scaffold(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.scrim,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.viewer_title), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    navigationIcon = { IconButton(onClick = nav.back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = {
                        IconButton(onClick = {
                            context.appContainer().imageStore.delete(imagePath)
                            nav.back()
                        }) { Icon(Icons.Filled.DeleteOutline, stringResource(R.string.viewer_delete)) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    offset = if (scale > 1f) offset + pan else Offset.Zero
                }
            }) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                )
            }
        }
    }
}