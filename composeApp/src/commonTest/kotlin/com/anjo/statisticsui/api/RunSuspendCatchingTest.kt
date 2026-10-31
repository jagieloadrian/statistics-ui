package com.anjo.statisticsui.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException

class RunSuspendCatchingTest :
    FunSpec({

        test("errorsBecomeFailures") {
            runSuspendCatching { throw Error("Fail to fetch") }.exceptionOrNull()?.message shouldBe "Fail to fetch"
        }

        test("cancellationPropagates") {
            shouldThrow<CancellationException> { runSuspendCatching { throw CancellationException() } }
        }
    })
