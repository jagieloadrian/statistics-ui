package com.anjo.statisticsui.ui.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

fun CoroutineScope.attempt(
    onError: (String?) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
) = launch {
    onError(null)
    runSuspendCatching { block() }.onFailure { onError(it.message ?: it.toString()) }
}
