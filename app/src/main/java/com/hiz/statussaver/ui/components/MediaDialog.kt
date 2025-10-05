package com.hiz.statussaver.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hiz.statussaver.ObserveAsEvent
import com.hiz.statussaver.SnackbarController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    pagerState: PagerState,
    onShare: () -> Unit = {},
    onDownload: () -> Unit = {},
    mediaComposable: @Composable (Modifier, Int) -> Unit = { modifier, page -> }
) {

    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val scope = rememberCoroutineScope()
    ObserveAsEvent(flow = SnackbarController.events, key1 = snackbarHostState) { snackbarEvent ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = snackbarEvent.message,
                actionLabel = snackbarEvent.action?.label,
            )
            if (result == SnackbarResult.ActionPerformed) {
                snackbarEvent.action?.onClick?.invoke()
            }
        }
    }

    Dialog(
        onDismissRequest = { onDismissRequest() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
        ),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(.9f)
                .windowInsetsPadding(WindowInsets.Companion.navigationBars),
            color = Color.Companion.Transparent
        ) {

            Scaffold(
                modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.9f),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Companion.Black,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        title = {
                            Text("", maxLines = 1)
                        },
                        navigationIcon = {
                            IconButton(onClick = { onDismissRequest() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "GO BACK",
                                    tint = Color.Companion.White
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    BottomAppBar(windowInsets = WindowInsets.Companion.navigationBars, actions = {
                        IconButton(onClick = {
                            onShare()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                            )
                        }
                        IconButton(onClick = { onDownload() }) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download",
                            )
                        }
                    })
                }
            ) { contentPadding ->

                Surface(
                    modifier = Modifier.Companion.padding(contentPadding),
                    color = Color.Companion.Black
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) { page ->
                        mediaComposable(Modifier.Companion, page)
                    }

                }
            }

        }

    }
}