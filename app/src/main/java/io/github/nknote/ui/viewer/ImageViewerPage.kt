package io.github.nknote.ui.viewer

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkNoteTheme
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerPage(
    imagePath: String,
    nav: NkNoteNavigation,
    viewModel: ImageViewerViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    // Vertical-swipe distance required to trigger dismiss. Larger than the gesture slop so an
    // accidental drag doesn't leave the viewer; tuned to feel like a deliberate fling.
    val dismissThresholdPx = with(density) { 160.dp.toPx() }

    // Offsets the image vertically while swiping; springs back to 0 on release, or animates out
    // and dismisses when the swipe exceeds [dismissThresholdPx]. Separate from the zoom/pan
    // [offset] so the two gestures never fight over the same state.
    val dismissOffset = remember { Animatable(0f) }

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.scrim,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.viewer_title),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = nav.back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val intent = viewModel.buildShareIntent(imagePath)
                            if (intent != null) {
                                context.startActivity(Intent.createChooser(intent, null))
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.viewer_share_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) { Icon(Icons.Filled.Share, stringResource(R.string.viewer_share)) }
                        IconButton(onClick = {
                            viewModel.delete(imagePath)
                            nav.back()
                        }) { Icon(Icons.Filled.DeleteOutline, stringResource(R.string.viewer_delete)) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // Transform gestures: pinch-to-zoom + pan. Keyed on scale so the detector
                    // restarts when scale changes, avoiding the stale-state bug. Pan is visually
                    // inert at scale==1 (offset forced to Zero), leaving vertical drags free for
                    // the dismiss gesture below.
                    .pointerInput(scale) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale > 1f) {
                                val maxX = size.width * (newScale - 1f) / 2f
                                val maxY = size.height * (newScale - 1f) / 2f
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            } else Offset.Zero
                        }
                    }
                    // Swipe-to-dismiss: only active at scale==1 (not while zoomed in & panning).
                    // Tracks vertical drag; on release, dismisses past the threshold or springs back.
                    .pointerInput(scale) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (scale <= 1f) {
                                    change.consume()
                                    scope.launch {
                                        dismissOffset.snapTo(dismissOffset.value + dragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (scale <= 1f) {
                                    val v = dismissOffset.value
                                    if (abs(v) > dismissThresholdPx) {
                                        // Animate a little further then exit — the nav transition fades.
                                        scope.launch {
                                            dismissOffset.animateTo(v * 2f)
                                            nav.back()
                                        }
                                    } else {
                                        scope.launch { dismissOffset.animateTo(0f, spring()) }
                                    }
                                }
                            },
                            onDragCancel = {
                                if (scale <= 1f) {
                                    scope.launch { dismissOffset.animateTo(0f, spring()) }
                                }
                            }
                        )
                    }
                    // Double-tap to toggle zoom
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
            ) {
                // Fade the image as it's dragged away so the dismiss feels like a physical lift.
                val dismissAlpha =
                    (1f - abs(dismissOffset.value) / (dismissThresholdPx * 2f))
                        .coerceIn(0.3f, 1f)
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y + dismissOffset.value,
                            alpha = dismissAlpha
                        )
                )

                // Image-info overlay: dimensions (BitmapFactory.outWidth/Height) + on-disk size.
                val info = remember(imagePath) {
                    runCatching {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(imagePath, opts)
                        ImageInfo(opts.outWidth, opts.outHeight, File(imagePath).length())
                    }.getOrNull()
                }
                if (info != null && info.width > 0 && info.height > 0) {
                    Text(
                        text = stringResource(
                            R.string.viewer_image_info,
                            info.width,
                            info.height,
                            formatFileSize(info.sizeBytes)
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(InfoOverlayPadding)
                            .graphicsLayer { alpha = dismissAlpha }
                    )
                }
            }
        }
    }
}

/** Decoded dimensions + on-disk byte size for the info overlay. */
private data class ImageInfo(val width: Int, val height: Int, val sizeBytes: Long)

/** Human-readable file size for the info overlay (B / KB / MB). */
private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private val InfoOverlayPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
