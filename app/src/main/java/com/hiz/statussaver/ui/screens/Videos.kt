package com.hiz.statussaver.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.hiz.statussaver.SnackbarController
import com.hiz.statussaver.SnackbarEvent
import com.hiz.statussaver.downloadFile
import com.hiz.statussaver.shareFile
import com.hiz.statussaver.ui.components.MediaDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var progress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableStateOf(0.toDuration(DurationUnit.MILLISECONDS)) }
    LaunchedEffect(player) {
        while (true) {
            if (player.duration > 0) {
                currentPosition = player.currentPosition.toDuration(DurationUnit.MILLISECONDS)
                progress = (player.currentPosition.toFloat() / player.duration.toFloat())
            } else 0f
            if (!isActive) {
                break
            }
            delay(500)
        }
    }

    DisposableEffect(key1 = player) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->

            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_DESTROY -> player.pause()
                else -> { /* Ignore other events */
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.pause()
        }
    }

    LaunchedEffect(isPlaying) {
        snapshotFlow { isPlaying }.collect { playing ->
            if (playing) {
                player.play()
            } else player.pause()
        }
    }

    LaunchedEffect(controlsVisible) {
        snapshotFlow { controlsVisible }.collect { visible ->
            if (controlsVisible) {
                delay(5000)
                controlsVisible = false
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = printMillis(currentPosition.inWholeMilliseconds),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,

                )
            val duration = player.duration.takeIf { it > 0 } ?: 0L
            Text(
                text = printMillis(duration),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(10.dp)
                .fillMaxWidth()
        ) {
            Slider(
                value = progress,
                onValueChange = {
                    val seekPosition = (it * player.duration).toLong()
                    player.seekTo(seekPosition)
                },
                modifier.fillMaxWidth(),
            )

        }
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = modifier
                .clickable(onClick = { controlsVisible = true })
                .fillMaxHeight(0.5f),
            contentAlignment = Alignment.Center
        ) {
            PlayerSurface(
                player, modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
            )
            if (controlsVisible) {
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    val icon = if (isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    }
                    Icon(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(100.dp),
                        tint = Color.White,
                        imageVector = icon,
                        contentDescription = "Toggle",
                    )
                }
            }

        }

    }
}


data class VideoMediaItem(val mediaItem: MediaItem, val context: Context) {

    private var _player: ExoPlayer? = null

    val player: ExoPlayer
        get() {
            if (_player == null) {
                _player = ExoPlayer.Builder(context).build().apply {
                    val playerItem =
                        androidx.media3.common.MediaItem.Builder().setUri(mediaItem.uri).build()
                    setMediaItem(playerItem)
                    prepare()
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    playWhenReady = false
                }
            }
            return _player!!
        }
    private val initialized: Boolean
        get() = _player != null

    fun release() {
        if (initialized) {
            player.release()
            _player = null
        }
    }

    fun stop() {
        if (initialized) {
            if (player.isPlaying) {
                player.pause()
            }
        }
    }
}

@Composable
fun Videos(modifier: Modifier = Modifier, mediaItems: List<MediaItem> = emptyList()) {
    val context = LocalContext.current
    val videoMediaItems = remember {
        mediaItems.map { VideoMediaItem(it, context) }
    }
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<VideoMediaItem?>(null) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { videoMediaItems.size }, initialPage = 0)


    DisposableEffect(key1 = videoMediaItems) {
        onDispose {
            videoMediaItems.map { it -> it.release() }
        }
    }


    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val item = videoMediaItems.getOrNull(page)

            if (item != selectedItem) {
                selectedItem = item
            }

            scope.launch() {
                videoMediaItems.forEach { it ->
                    if (it != item) {
                        it.stop()
                    }
                }
                videoMediaItems.forEachIndexed { index, item ->
                    if (abs(page - index) > 3) {
                        item.release()
                    }
                }
            }

        }
    }

    if (showDialog && selectedItem != null) {
        MediaDialog(
            mediaComposable = { mod, page ->
                val item = videoMediaItems.getOrNull(page)
                item!!.player.seekTo(0)
                VideoPlayer(
                    modifier = mod.fillMaxSize(),
                    player = item.player
                )

            },
            pagerState = pagerState,
            onDismissRequest = {
                showDialog = false
                selectedItem?.stop()
            },
            onShare = {
                Log.d("MediaQuery", "Sharing file: ${selectedItem!!.mediaItem.uri}")
                shareFile(context, selectedItem!!.mediaItem.uri, "video/*")
            },
            onDownload = {
                Log.d("MediaQuery", "Downloading file: ${selectedItem!!.mediaItem.path}")
                downloadFile(
                    context,
                    selectedItem!!.mediaItem.uri,
                    selectedItem!!.mediaItem.name,
                    "video/*"
                )
                scope.launch {
                    SnackbarController.sendEvent(SnackbarEvent("Image downloaded to the Downloads folder"))
                }
            })
    }
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        items(videoMediaItems) { video ->
            val modifier = Modifier.clickable() {
                val index = videoMediaItems.indexOf(video)
                scope.launch {
                    Log.d("MediaQuery", "Clicked on video at index: $index")
                    pagerState.animateScrollToPage(index)
                }
                showDialog = true
            }
            val model = ImageRequest.Builder(context)
                .data(video.mediaItem.uri)
                .videoFrameMillis(10000)
                .decoderFactory { result, options, _ ->
                    VideoFrameDecoder(
                        result.source,
                        options
                    )
                }
                .build()
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = video.mediaItem.name,
                    modifier = modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .aspectRatio(.5f)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.FillHeight,
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp),
                        tint = Color.White,
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                    )
                }
            }
        }
    }
}


fun printMillis(millis: Long): String {
    val totalSeconds = (millis / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return String.format(null, "%02d:%02d", minutes, seconds)
}