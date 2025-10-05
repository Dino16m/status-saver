package com.hiz.statussaver

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File

fun drawableToUri(context: android.content.Context, drawableId: Int): Uri {
    val inputStream = context.resources.openRawResource(drawableId)
    val file = File(
        context.cacheDir,
        "wa_shared_status_${System.currentTimeMillis()}.png"
    )
    inputStream.use { stream ->
        file.outputStream().use { outputStream ->
            stream.copyTo(outputStream)
        }
    }

    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        file
    )
    return uri
}


fun shareFile(context: android.content.Context, uri: Uri, mimeType: String) {
    val intent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        type = mimeType
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        android.content.Intent.createChooser(
            intent,
            "Share Status"
        )
    )
}

fun downloadFile(context: android.content.Context, uri: Uri, fileName: String, mimeType: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
    }
    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    val outputUri = context.contentResolver.insert(collection, contentValues)
    Log.d("MediaQuery", "Output URI: $outputUri")
    if (outputUri != null) {
        context.contentResolver.openOutputStream(outputUri).use { outputStream ->
            context.contentResolver.openInputStream(uri).use { s ->
                s?.copyTo(outputStream!!)
            }
        }
    }
}

data class SnackbarEvent(
    val message: String,
    val action: SnackbarAction? = null
)

data class SnackbarAction(
    val label: String,
    val onClick: () -> Unit
)

object SnackbarController {
    private val _events = Channel<SnackbarEvent>()
    val events = _events.receiveAsFlow()

    suspend fun sendEvent(event: SnackbarEvent) {
        _events.send(event)
    }
}