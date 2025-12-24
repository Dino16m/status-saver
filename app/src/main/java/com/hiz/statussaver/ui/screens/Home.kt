package com.hiz.statussaver.ui.screens

import android.R.attr.duration
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class TabDestinations(val label: String) {
    IMAGES("Images"),
    VIDEOS("Videos");
}

data class MediaItem(val uri: Uri, val name: String, val path: String)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(modifier: Modifier) {
    val context = LocalContext.current
    var hasPerms by remember { mutableStateOf(false) }
    var initialUri by remember { mutableStateOf("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses".toUri()) }
    val persistedPermissions = context.contentResolver.persistedUriPermissions

    // Find the specific permission for the given Uri
    val permission = persistedPermissions.find { it.uri == initialUri }
    hasPerms = permission?.isReadPermission == true
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri == null) {
            hasPerms = false
            return@rememberLauncherForActivityResult
        }
        hasPerms = true
        initialUri = treeUri

        try {
            // Persist read access permission for long-term use
            val contentResolver = context.contentResolver
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            Log.d("MediaQuery", "Access granted to: $treeUri")

        } catch (e: Exception) {
            Log.d("MediaQuery", "An error occurred: ${e.message}")
            hasPerms = false
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPerms) {
            directoryLauncher.launch(initialUri)
        }
    }

    if (hasPerms) {
        HomeContent(modifier = modifier, initialUri, context)
    } else {
        Text("Permission to access media is required.", modifier = modifier.padding(16.dp))
    }
}

fun fetchMediaItems(context: Context, rootUri: Uri): List<MediaItem> {
    val mediaItems = mutableListOf<Pair<Long, MediaItem>>()
    val contentResolver = context.contentResolver

    // 1. Prepare the Children URI
    val documentId = DocumentsContract.getTreeDocumentId(rootUri)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, documentId)

    // 2. Define the columns we want to fetch in one go
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    // 3. Query the ContentResolver
    contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val dateIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

        while (cursor.moveToNext()) {
            val mimeType = cursor.getString(mimeIndex)

            if (mimeType != DocumentsContract.Document.MIME_TYPE_DIR) {
                val docId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex) ?: "Unnamed"
                val lastModified = cursor.getLong(dateIndex)

                // Build the full URI for the specific file
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)

                mediaItems.add(
                    Pair(lastModified,  MediaItem(
                        uri = fileUri,
                        name = name,
                        path = fileUri.path ?: "No path",
                    ))

                )
            }
        }
    }
    return mediaItems.sortedByDescending { it.first }.map { it.second }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(modifier: Modifier = Modifier, treeUri: Uri, context: Context) {

    val scope = rememberCoroutineScope()

    val mediaItems = remember { emptyList<MediaItem>().toMutableStateList() }

    val images = mediaItems.filter {
        it.name.endsWith(".jpg") || it.name.endsWith(".png") || it.name.endsWith(".jpeg")
    }
    val videos = mediaItems.filter { it.name.endsWith(".mp4") }


    val pagerState = rememberPagerState(initialPage = 0) { TabDestinations.entries.size }

    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            isLoading = true
            val initialMedia = fetchMediaItems(context, treeUri)
            mediaItems.clear()
            mediaItems.addAll(initialMedia)
            isLoading = false
            Log.d("MediaQuery", "Loaded ${initialMedia.size} items in $duration ms" )
        }
    }
    val onRefresh = {
        scope.launch(Dispatchers.IO) {
            if (!isRefreshing) {
                isRefreshing = true
                Log.d("MediaQuery", "Refreshing media items...")
                val updatedMedia = fetchMediaItems(context, treeUri)
                Log.d("MediaQuery", "Found ${updatedMedia.size} items after refresh")
                mediaItems.clear()
                mediaItems.addAll(updatedMedia)
                isRefreshing = false
                Log.d("MediaQuery", "Refreshed media items...")
            }
        }.let {  }
    }


    Scaffold(modifier = modifier) { contentPadding ->
        if (isLoading){
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(contentPadding).fillMaxSize()) {
                CircularProgressIndicator(strokeWidth = 7.dp, modifier = Modifier.align(Alignment.CenterHorizontally).size(100.dp))
            }
        } else {
            Column(modifier = Modifier.padding(contentPadding)) {
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    TabDestinations.entries.forEachIndexed { index, destination ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = destination.label,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                HorizontalPager(pagerState) { page ->
                    val destination = TabDestinations.entries[page]
                    when (destination) {
                        TabDestinations.IMAGES -> {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,

                                ) {

                                Images(Modifier, images)
                            }
                        }

                        TabDestinations.VIDEOS -> {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                            ) {

                                Videos(Modifier, videos)
                            }
                        }
                    }
                }

            }
        }
    }
}
