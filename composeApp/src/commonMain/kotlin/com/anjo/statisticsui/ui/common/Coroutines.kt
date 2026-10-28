package com.anjo.statisticsui.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.anjo.statisticsui.api.runSuspendCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun reporting(
    onError: (String?) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
) {
    onError(null)
    runSuspendCatching { coroutineScope(block) }.onFailure { onError(it.message ?: it.toString()) }
}

fun CoroutineScope.attempt(
    onError: (String?) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
) = launch { reporting(onError, block) }

/**
 * Deferred delete: nothing reaches the server until the snackbar closes without Undo, so undoing needs no server
 * support. Leaving the screen while the snackbar shows commits the delete. Returns whether it deleted.
 */
suspend fun SnackbarHostState.deleteWithUndo(
    message: String,
    delete: suspend () -> Unit,
): Boolean {
    val undone =
        try {
            showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed
        } catch (e: CancellationException) {
            withContext(NonCancellable) { delete() }
            throw e
        }
    if (!undone) delete()
    return !undone
}
