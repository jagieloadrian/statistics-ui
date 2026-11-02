package com.anjo.statisticsui

import com.anjo.statisticsui.ui.common.ShownErrors
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ShownErrorsTest :
    FunSpec({

        val errors = mapOf("email" to "Required", "password" to "Required")

        test("hiddenUntilLeftOrSubmitted") {
            // given
            val shown = ShownErrors()

            // then
            shown.of(errors) shouldBe emptyMap()

            // when
            shown.leave("email")

            // then
            shown.of(errors) shouldBe mapOf("email" to "Required")

            // when
            shown.submit()

            // then
            shown.of(errors) shouldBe errors
        }

        test("resetHidesAgain") {
            // given
            val shown = ShownErrors()
            shown.leave("email")
            shown.submit()

            // when
            shown.reset()

            // then
            shown.of(errors) shouldBe emptyMap()
        }
    })
