package com.anjo.statisticsui.api

import kotlinx.coroutines.CancellationException

suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // Throwable, not Exception: Ktor on wasm reports network failures as kotlin.Error
        Result.failure(e)
    }
