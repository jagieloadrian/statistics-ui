package com.anjo.statisticsui.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.milliseconds

class LoopbackTest : FunSpec({
    val http = HttpClient.newHttpClient()
    fun get(url: String): String = http.send(HttpRequest.newBuilder(URI(url)).build(), HttpResponse.BodyHandlers.ofString()).body()

    test("callbackReturnsTheCode") {
        Loopback().use { loopback ->
            get("${loopback.redirectUri}?code=c1&state=s1") shouldBe "Done, you can close this window and return to StatsFlow"
            loopback.awaitCode("s1") shouldBe "c1"
        }
    }

    test("otherStateIsRefused") {
        Loopback().use { loopback ->
            get("${loopback.redirectUri}?code=c1&state=forged")
            shouldThrow<Exception> { loopback.awaitCode("s1") }.message shouldBe "Sign-in expired, try again"
        }
    }

    test("providerErrorIsShownInBrowserAndApp") {
        Loopback().use { loopback ->
            get("${loopback.redirectUri}?error=access_denied&error_description=User%20cancelled&state=s1") shouldBe "User cancelled"
            shouldThrow<Exception> { loopback.awaitCode("s1") }.message shouldBe "User cancelled"
        }
    }

    test("timeoutFailsAndFreesThePort") {
        val port = Loopback(timeout = 200.milliseconds).use { loopback ->
            shouldThrow<Exception> { loopback.awaitCode("s1") }.message shouldBe "No answer from the browser, try again"
            URI(loopback.redirectUri).port
        }
        ServerSocket(port, 0, InetAddress.getByName("127.0.0.1")).close()
    }
})
