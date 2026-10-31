package com.anjo.statisticsui

import com.anjo.statisticsui.ui.common.ShownErrors
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ShownErrorsTest :
    FunSpec({

        val errors = mapOf("email" to "Required", "password" to "Required")

        test("hiddenUntilLeftOrSubmitted") {
            val shown = ShownErrors()
            shown.of(errors) shouldBe emptyMap()
            shown.leave("email")
            shown.of(errors) shouldBe mapOf("email" to "Required")
            shown.submit()
            shown.of(errors) shouldBe errors
        }

        test("resetHidesAgain") {
            val shown = ShownErrors()
            shown.leave("email")
            shown.submit()
            shown.reset()
            shown.of(errors) shouldBe emptyMap()
        }
    })
