package com.hiz.statussaver.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hiz.statussaver.SnackbarController
import com.hiz.statussaver.SnackbarEvent
import com.hiz.statussaver.downloadFile
import com.hiz.statussaver.shareFile
import com.hiz.statussaver.ui.components.MediaDialog
import kotlinx.coroutines.launch


@Composable
fun ZoomableImage(
    modifier: Modifier = Modifier,
    imageComposable: @Composable (Modifier) -> Unit = {}
) {
    val scale = remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .clip(RectangleShape) // Clip the box content
            .pointerInput(Unit) {
                awaitEachGesture {
                    var transformEvent = awaitPointerEvent()

                    do {
                        val zoomChange = transformEvent.calculateZoom()

                        scale.floatValue *= zoomChange
                        transformEvent = awaitPointerEvent()
                    } while (transformEvent.changes.any { it.pressed })

                }
            }
    ) {
        imageComposable(
            Modifier
                .align(Alignment.Center)
                .graphicsLayer(
                    // adding some zoom limits (min 100%, max 200%)
                    scaleX = maxOf(1f, minOf(3f, scale.floatValue)),
                    scaleY = maxOf(1f, minOf(3f, scale.floatValue)),
                )
                .fillMaxSize()
        )
    }
}

@Composable
fun Images(modifier: Modifier = Modifier, mediaItems: List<MediaItem> = emptyList()) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { mediaItems.size }, initialPage = 0)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val item = mediaItems.getOrNull(page)
            if (item != selectedItem) {
                selectedItem = item
            }
        }
    }

    if (showDialog && selectedItem != null) {
        MediaDialog(
            mediaComposable = { mod, page ->
                val item = mediaItems.getOrNull(page)
                ZoomableImage(
                    modifier = Modifier.fillMaxSize(),
                    imageComposable = { mod ->
                        AsyncImage(
                            model = item!!.uri,
                            contentDescription = selectedItem!!.name,
                            modifier = mod,
                        )
                    }
                )

            },
            pagerState = pagerState,
            onDismissRequest = { showDialog = false },
            onShare = {
                Log.d("MediaQuery", "Sharing file: ${selectedItem!!.uri}")
                shareFile(context, selectedItem!!.uri, "image/*")
            },
            onDownload = {
                Log.d("MediaQuery", "Downloading file: ${selectedItem!!.path}")
                downloadFile(context, selectedItem!!.uri, selectedItem!!.name, "image/*")
                scope.launch {
                    SnackbarController.sendEvent(SnackbarEvent("Image downloaded to the Downloads folder"))
                }
            })
    }
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(5.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
    ) {

        items(mediaItems) { photo ->
            val modifier = Modifier.clickable() {
                val index = mediaItems.indexOf(photo)
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
                showDialog = true
            }
            Box(
                modifier = modifier
            ) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.name,
                    modifier = modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .aspectRatio(.5f)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.FillHeight,
                )
            }
        }
    }
}
