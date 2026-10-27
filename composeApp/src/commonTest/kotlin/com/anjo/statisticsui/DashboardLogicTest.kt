package com.anjo.statisticsui

import com.anjo.statisticsui.model.ChartLine
import com.anjo.statisticsui.model.FieldSpec
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.Rule
import com.anjo.statisticsui.model.Series
import com.anjo.statisticsui.model.TimestampSpec
import com.anjo.statisticsui.ui.admin.rows
import com.anjo.statisticsui.ui.admin.withRows
import com.anjo.statisticsui.ui.dashboard.formatNumber
import com.anjo.statisticsui.ui.dashboard.lineChartData
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
        assertEquals(listOf(ChartLine("a · t", "t", listOf(0 to 1f, 1 to 2f)), ChartLine("b · t", "t", listOf(2 to 5f))), chart.lines)
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
        assertEquals(listOf(ChartLine("infected", "infected", listOf(0 to 4f, 1 to 5f))), chart.lines)
    }

    @Test
    fun decimalsAreFixed() {
        assertEquals(listOf("21.3", "-0.50", "3", "21.35"), listOf(formatNumber(21.25, 1), formatNumber(-0.5, 2), formatNumber(2.6, 0), formatNumber(21.349)))
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
