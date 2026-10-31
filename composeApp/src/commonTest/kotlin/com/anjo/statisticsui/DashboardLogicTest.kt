package com.anjo.statisticsui

import com.anjo.statisticsui.model.ChartLine
import com.anjo.statisticsui.model.FieldSpec
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.Rule
import com.anjo.statisticsui.model.Series
import com.anjo.statisticsui.model.TimestampSpec
import com.anjo.statisticsui.model.rows
import com.anjo.statisticsui.model.withRows
import com.anjo.statisticsui.ui.dashboard.formatNumber
import com.anjo.statisticsui.ui.dashboard.lineChartData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class DashboardLogicTest :
    FunSpec({

        fun row(
            ts: String,
            v: Number?,
        ) = listOf(JsonPrimitive(ts), v?.let(::JsonPrimitive) ?: JsonNull)

        test("lineChartMergesSeriesOnTs") {
            val result =
                QueryResult(
                    series =
                        listOf(
                            Series(
                                mapOf("device" to "a"),
                                listOf("ts", "t"),
                                listOf(row("2026-10-26T01:00:00Z", 2), row("2026-10-26T00:00:00Z", 1)),
                            ),
                            Series(
                                mapOf("device" to "b"),
                                listOf("ts", "t"),
                                listOf(row("2026-10-26T02:00:00Z", 5), row("2026-10-26T03:00:00Z", null)),
                            ),
                        ),
                )
            val chart = lineChartData(result).shouldNotBeNull()
            chart.labels shouldBe listOf("10-26 00:00", "10-26 01:00", "10-26 02:00")
            chart.lines shouldBe listOf(ChartLine("a · t", "t", listOf(0 to 1f, 1 to 2f)), ChartLine("b · t", "t", listOf(2 to 5f)))
            (chart.min to chart.max) shouldBe (1f to 5f)
            lineChartData(
                QueryResult(series = listOf(Series(columns = listOf("t"), rows = listOf(listOf(JsonPrimitive(1)))))),
            ).shouldBeNull()
        }

        test("lineChartUsesNumericXAndSkipsTs") {
            val result =
                QueryResult(
                    series =
                        listOf(
                            Series(
                                columns = listOf("ts", "generation", "infected"),
                                rows =
                                    listOf(
                                        listOf(JsonPrimitive("2026-10-26T00:00:10Z"), JsonPrimitive(10), JsonPrimitive(5)),
                                        listOf(JsonPrimitive("2026-10-26T00:00:09Z"), JsonPrimitive(9), JsonPrimitive(4)),
                                    ),
                            ),
                        ),
                )
            val chart = lineChartData(result, "generation").shouldNotBeNull()
            chart.labels shouldBe listOf("9", "10")
            chart.lines shouldBe listOf(ChartLine("infected", "infected", listOf(0 to 4f, 1 to 5f)))
        }

        test("decimalsAreFixed") {
            listOf(formatNumber(21.25, 1), formatNumber(-0.5, 2), formatNumber(2.6, 0), formatNumber(21.349)) shouldBe
                listOf("21.3", "-0.50", "3", "21.35")
        }

        test("mappingRowsRoundTrip") {
            val rule =
                Rule(
                    timestamp = TimestampSpec("timestamp"),
                    identity = listOf("deviceId"),
                    dims = mapOf("device" to FieldSpec(path = "deviceId")),
                    vals =
                        mapOf(
                            "temperature" to FieldSpec(path = "temperature", min = -40.0),
                            "humidity" to FieldSpec(path = "h", required = false),
                        ),
                )
            listOf(rule).withRows(rule.rows()) shouldBe listOf(rule)
            emptyList<Rule>().withRows(emptyList()) shouldBe emptyList()
        }
    })
