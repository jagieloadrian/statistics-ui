package com.anjo.statisticsui.ui.common

import com.anjo.statisticsui.api.runSuspendCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun CoroutineScope.attempt(
    onError: (String?) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
) = launch {
    onError(null)
    runSuspendCatching { block() }.onFailure { onError(it.message ?: it.toString()) }
}
