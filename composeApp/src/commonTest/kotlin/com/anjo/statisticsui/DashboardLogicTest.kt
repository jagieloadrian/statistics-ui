package com.anjo.statisticsui

import com.anjo.statisticsui.api.FieldSpec
import com.anjo.statisticsui.api.QueryResult
import com.anjo.statisticsui.api.Rule
import com.anjo.statisticsui.api.Series
import com.anjo.statisticsui.api.TimestampSpec
import com.anjo.statisticsui.presentation.admin.rows
import com.anjo.statisticsui.presentation.admin.withRows
import com.anjo.statisticsui.presentation.dashboard.lineChartData
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DashboardLogicTest {

    private fun row(ts: String, v: Number?) = listOf(JsonPrimitive(ts), v?.let(::JsonPrimitive) ?: JsonNull)

    @Test
    fun lineChartMergesSeriesOnTs() {
        val result = QueryResult(series = listOf(
            Series(mapOf("device" to "a"), listOf("ts", "t"), listOf(row("2026-10-26T01:00:00Z", 2), row("2026-10-26T00:00:00Z", 1))),
            Series(mapOf("device" to "b"), listOf("ts", "t"), listOf(row("2026-10-26T02:00:00Z", 5), row("2026-10-26T03:00:00Z", null))),
        ))
        val chart = assertNotNull(lineChartData(result))
        assertEquals(listOf("10-26 00:00", "10-26 01:00", "10-26 02:00"), chart.labels) // null value dropped
        assertEquals(listOf("a · t" to listOf(0 to 1f, 1 to 2f), "b · t" to listOf(2 to 5f)), chart.lines)
        assertEquals(1f to 5f, chart.min to chart.max)
        assertNull(lineChartData(QueryResult(series = listOf(Series(columns = listOf("t"), rows = listOf(listOf(JsonPrimitive(1))))))))
    }

    @Test
    fun lineChartUsesNumericXAndSkipsTs() {
        val result = QueryResult(series = listOf(Series(columns = listOf("ts", "generation", "infected"), rows = listOf(
            listOf(JsonPrimitive("2026-10-26T00:00:10Z"), JsonPrimitive(10), JsonPrimitive(5)),
            listOf(JsonPrimitive("2026-10-26T00:00:09Z"), JsonPrimitive(9), JsonPrimitive(4)),
        ))))
        val chart = assertNotNull(lineChartData(result, "generation"))
        assertEquals(listOf("9", "10"), chart.labels)
        assertEquals(listOf("infected" to listOf(0 to 4f, 1 to 5f)), chart.lines)
    }

    @Test
    fun mappingRowsRoundTrip() {
        val rule = Rule(
            timestamp = TimestampSpec("timestamp"),
            identity = listOf("deviceId"),
            dims = mapOf("device" to FieldSpec(path = "deviceId")),
            vals = mapOf("temperature" to FieldSpec(path = "temperature", min = -40.0), "humidity" to FieldSpec(path = "h", required = false)),
        )
        assertEquals(listOf(rule), listOf(rule).withRows(rule.rows()))
        assertEquals(emptyList(), emptyList<Rule>().withRows(emptyList()))
    }
}
