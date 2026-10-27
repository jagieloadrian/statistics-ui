package com.anjo.statisticsui

import com.anjo.statisticsui.auth.Callback
import com.anjo.statisticsui.auth.parseCallback
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CallbackTest : FunSpec({

    test("codeAndState") {
        parseCallback("?code=abc&state=xyz") shouldBe Callback("abc", "xyz", null)
        parseCallback("state=xyz&code=abc&iss=https%3A%2F%2Fauth.example.com") shouldBe Callback("abc", "xyz", null)
    }

    test("errorPrefersItsDescription") {
        parseCallback("?error=access_denied&error_description=User+cancelled+the+request&state=xyz") shouldBe
                Callback(null, "xyz", "User cancelled the request")
        parseCallback("?error=access_denied") shouldBe Callback(null, null, "access_denied")
    }

    test("valuesAreDecoded") {
        parseCallback("?code=a%2Fb%3Dc&state=s%20t") shouldBe Callback("a/b=c", "s t", null)
    }

    test("noCallback") {
        parseCallback("") shouldBe null
        parseCallback("?") shouldBe null
        parseCallback("?page=2&state=xyz") shouldBe null
    }
})
