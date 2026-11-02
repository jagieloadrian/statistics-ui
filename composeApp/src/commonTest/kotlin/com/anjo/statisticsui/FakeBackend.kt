package com.anjo.statisticsui

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.api.platformConfig
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.platformJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.yield

class Sent(
    val method: HttpMethod,
    val path: String,
    val body: String,
    val parameters: Parameters,
    val authorization: String?,
)

internal class FakeBackend {
    private class Reply(
        val status: HttpStatusCode,
        val body: String,
    )

    private val replies = mutableMapOf<String, Reply>()
    private val calls = MutableStateFlow(emptyList<Sent>())
    var offline = false
    val session = Session()
    val api =
        PlatformApi(
            HttpClient(
                MockEngine { request ->
                    val path = request.url.encodedPath.substringAfter("/api/v2/")
                    val call =
                        Sent(
                            request.method,
                            path,
                            request.body.toByteArray().decodeToString(),
                            request.url.parameters,
                            request.headers[HttpHeaders.Authorization],
                        )
                    calls.update { it + call }
                    check(!offline) { "offline" }
                    val reply = replies["${request.method.value} $path"] ?: Reply(HttpStatusCode.NotFound, errorJson("No reply for $path"))
                    respond(reply.body, reply.status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ) { platformConfig() },
            session,
        )

    fun json(
        path: String,
        body: String,
        method: HttpMethod = HttpMethod.Get,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        replies["${method.value} $path"] = Reply(status, body)
    }

    inline fun <reified T> reply(
        path: String,
        body: T,
        method: HttpMethod = HttpMethod.Get,
    ) = json(path, platformJson.encodeToString(body), method)

    fun ok(
        path: String,
        method: HttpMethod,
    ) = json(path, "", method, HttpStatusCode.NoContent)

    fun fail(
        path: String,
        status: HttpStatusCode,
        message: String,
        method: HttpMethod = HttpMethod.Get,
    ) = json(path, errorJson(message), method, status)

    fun sent(): List<Sent> = calls.value

    fun sent(
        method: HttpMethod,
        path: String,
    ): List<Sent> = sent().filter { it.method == method && it.path == path }

    private fun errorJson(message: String) = """{"error":"failed","message":"$message"}"""
}

suspend fun stateScope(): CoroutineScope = CoroutineScope(currentCoroutineContext() + Job())

suspend fun CoroutineScope.settle() {
    while (true) {
        val running =
            coroutineContext.job.children
                .filterNot { it.isCompleted }
                .toList()
        if (running.isEmpty()) return
        running.joinAll()
    }
}

suspend fun SnackbarHostState.awaitSnackbar(): SnackbarData {
    while (true) {
        currentSnackbarData?.let { return it }
        yield()
    }
}
