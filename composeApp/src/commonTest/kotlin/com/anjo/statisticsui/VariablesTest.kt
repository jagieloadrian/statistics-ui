package com.anjo.statisticsui

import com.anjo.statisticsui.api.Dashboard
import com.anjo.statisticsui.api.Query
import com.anjo.statisticsui.api.RelativeRange
import com.anjo.statisticsui.api.Variable
import com.anjo.statisticsui.presentation.dashboard.TimeRange
import com.anjo.statisticsui.presentation.dashboard.initialDuration
import com.anjo.statisticsui.presentation.dashboard.resolve
import com.anjo.statisticsui.presentation.dashboard.resolveText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class VariablesTest {
    private val now = Instant.parse("2026-10-27T12:00:00Z")
    private val range = TimeRange.last(1.days, now)
    private val query = Query(dataset = "temperature", filter = mapOf("device" to listOf("\$device", "pico-9"), "type" to listOf("total")), bucket = "\$bucket")

    @Test
    fun substitutesFilterValuesAndBucket() {
        val q = resolve(query, mapOf("device" to listOf("pico-1", "pico-2"), "bucket" to listOf("1h")), range)
        assertEquals(mapOf("device" to listOf("pico-1", "pico-2", "pico-9"), "type" to listOf("total")), q.filter)
        assertEquals("1h", q.bucket)
        assertEquals("2026-10-26T12:00:00Z" to "2026-10-27T12:00:00Z", q.from to q.to)
    }

    @Test
    fun emptyVariableDropsFilterAndBucketFallsBackToAuto() {
        val q = resolve(query.copy(filter = mapOf("device" to listOf("\$device"))), mapOf("device" to emptyList(), "bucket" to emptyList()), range)
        assertEquals(emptyMap(), q.filter)
        assertEquals("auto", q.bucket)
    }

    @Test
    fun unknownVariableIsAnError() {
        assertFailsWith<IllegalArgumentException> { resolve(query, mapOf("bucket" to listOf("1h")), range) }
        assertEquals("Run 7 / \$other", resolveText("Run \$run / \$other", mapOf("run" to listOf("7"))))
    }

    @Test
    fun rangeFromTimeRangeVariableElseDefaultRange() {
        val base = Dashboard(id = "d", defaultRange = RelativeRange("P30D"))
        assertEquals(30.days, initialDuration(base))
        assertEquals(30.days, initialDuration(base.copy(variables = listOf(Variable("range", "timeRange")))))
        assertEquals(1.days, initialDuration(base.copy(variables = listOf(Variable("range", "timeRange", default = "P1D")))))
        assertEquals(24.hours, initialDuration(Dashboard(id = "d")))
    }
}
