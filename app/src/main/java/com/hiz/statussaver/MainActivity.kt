package com.hiz.statussaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.hiz.statussaver.ui.screens.Home
import com.hiz.statussaver.ui.theme.StatusSaverTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StatusSaverTheme {
                App()
            }
        }
    }
}

@Composable
fun <T> ObserveAsEvent(flow: Flow<T>, key1: Any? = null, key2: Any? = null, onEvent: (T) -> Unit) {
    val lifeCycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifeCycleOwner.lifecycle, key1, key2, flow) {
        lifeCycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}

@Composable
fun App() {
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
    Scaffold(snackbarHost = {
        SnackbarHost(hostState = snackbarHostState)
    }, modifier = Modifier.fillMaxSize()) { innerPadding ->
        Home(
            modifier = Modifier.padding(innerPadding)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun AppPreview() {
    StatusSaverTheme {
        App()
    }
}