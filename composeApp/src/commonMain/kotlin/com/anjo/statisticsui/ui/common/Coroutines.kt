package com.anjo.statisticsui.ui.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Like [runCatching], but lets coroutine cancellation through. */
suspend fun <T> catching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/** Launches [block]; clears [onError] first and reports any failure message to it. */
fun CoroutineScope.attempt(onError: (String?) -> Unit, block: suspend CoroutineScope.() -> Unit) = launch {
    onError(null)
    catching { block() }.onFailure { onError(it.message ?: it.toString()) }
}
