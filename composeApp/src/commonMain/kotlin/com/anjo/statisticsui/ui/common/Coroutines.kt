package com.anjo.statisticsui.ui.common

import com.anjo.statisticsui.api.runSuspendCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
