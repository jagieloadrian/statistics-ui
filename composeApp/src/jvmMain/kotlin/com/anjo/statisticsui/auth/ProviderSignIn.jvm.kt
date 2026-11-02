package com.anjo.statisticsui.auth

import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.User
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

actual suspend fun providerSignIn(api: PlatformApi): User? =
    Loopback().use { loopback ->
        val start = api.oidcStart(loopback.redirectUri)
        Desktop.getDesktop().browse(URI(start.authorizationUrl))
        api.oidcLogin(loopback.awaitCode(start.state), start.codeVerifier, loopback.redirectUri)
    }

actual suspend fun resumeProviderSignIn(api: PlatformApi): User? = null

internal class Loopback(
    private val timeout: Duration = 5.minutes,
) : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val callback = CompletableDeferred<Callback>()
    val redirectUri = "http://127.0.0.1:${server.address.port}/callback"

    init {
        server.createContext("/callback") { exchange ->
            val c = parseCallback(exchange.requestURI.rawQuery.orEmpty())
            val text =
                when {
                    c == null -> "Not a sign-in answer"
                    c.error != null -> c.error
                    else -> "Done, you can close this window and return to StatsFlow"
                }.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
            exchange.sendResponseHeaders(if (c == null) 400 else 200, text.size.toLong())
            exchange.responseBody.use { it.write(text) }
            c?.let(callback::complete)
        }
        server.start()
    }

    suspend fun awaitCode(state: String): String {
        val c = withTimeoutOrNull(timeout) { callback.await() } ?: error("No answer from the browser, try again")
        c.error?.let { error(it) }
        if (c.code == null || c.state != state) error("Sign-in expired, try again")
        return c.code
    }

    override fun close() = server.stop(0)
}
