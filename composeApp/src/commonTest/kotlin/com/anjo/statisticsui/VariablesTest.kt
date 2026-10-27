package com.anjo.statisticsui

import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.RelativeRange
import com.anjo.statisticsui.model.TimeRange
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.ui.dashboard.initialDuration
import com.anjo.statisticsui.ui.dashboard.resolve
import com.anjo.statisticsui.ui.dashboard.resolveText
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class VariablesTest : FunSpec({
    val now = Instant.parse("2026-10-27T12:00:00Z")
    val range = TimeRange.last(1.days, now)
    val query = Query(dataset = "temperature", filter = mapOf("device" to listOf("\$device", "pico-9"), "type" to listOf("total")), bucket = "\$bucket")

    test("substitutesFilterValuesAndBucket") {
        val q = resolve(query, mapOf("device" to listOf("pico-1", "pico-2"), "bucket" to listOf("1h")), range)
        q.filter shouldBe mapOf("device" to listOf("pico-1", "pico-2", "pico-9"), "type" to listOf("total"))
        q.bucket shouldBe "1h"
        (q.from to q.to) shouldBe ("2026-10-26T12:00:00Z" to "2026-10-27T12:00:00Z")
    }

    test("emptyVariableDropsFilterAndBucketFallsBackToAuto") {
        val q = resolve(query.copy(filter = mapOf("device" to listOf("\$device"))), mapOf("device" to emptyList(), "bucket" to emptyList()), range)
        q.filter shouldBe emptyMap()
        q.bucket shouldBe "auto"
    }

    test("unknownVariableIsAnError") {
        shouldThrow<IllegalArgumentException> { resolve(query, mapOf("bucket" to listOf("1h")), range) }
        resolveText("Run \$run / \$other", mapOf("run" to listOf("7"))) shouldBe "Run 7 / \$other"
    }

    test("rangeFromTimeRangeVariableElseDefaultRange") {
        val base = Dashboard(id = "d", defaultRange = RelativeRange("P30D"))
        initialDuration(base) shouldBe 30.days
        initialDuration(base.copy(variables = listOf(Variable("range", "timeRange")))) shouldBe 30.days
        initialDuration(base.copy(variables = listOf(Variable("range", "timeRange", default = "P1D")))) shouldBe 1.days
        initialDuration(Dashboard(id = "d")) shouldBe 24.hours
    }
})
