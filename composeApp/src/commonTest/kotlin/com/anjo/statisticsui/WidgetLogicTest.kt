package com.anjo.statisticsui

import androidx.compose.ui.graphics.Color
import com.anjo.statisticsui.model.OidcLoginRequest
import com.anjo.statisticsui.model.OidcStart
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.QueryMeasure
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.Rejection
import com.anjo.statisticsui.model.Series
import com.anjo.statisticsui.model.SourceStatus
import com.anjo.statisticsui.model.Threshold
import com.anjo.statisticsui.model.UnitDisplay
import com.anjo.statisticsui.model.WidgetOptions
import com.anjo.statisticsui.model.platformJson
import com.anjo.statisticsui.ui.dashboard.format
import com.anjo.statisticsui.ui.dashboard.formatValue
import com.anjo.statisticsui.ui.dashboard.number
import com.anjo.statisticsui.ui.dashboard.parseColor
import com.anjo.statisticsui.ui.dashboard.shortTime
import com.anjo.statisticsui.ui.dashboard.statValues
import com.anjo.statisticsui.ui.dashboard.thresholdColor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

class WidgetLogicTest :
    FunSpec({

        test("statShowsTheLastRowPerSeries") {
            // given
            val result =
                QueryResult(
                    series =
                        listOf(
                            Series(
                                mapOf("city" to "Krakow"),
                                listOf("ts", "temp"),
                                listOf(listOf(JsonPrimitive("t1"), JsonPrimitive(1)), listOf(JsonPrimitive("t2"), JsonPrimitive(2))),
                            ),
                            Series(columns = listOf("temp"), rows = emptyList()),
                        ),
                )

            // when and then
            statValues(result) shouldBe listOf("Krakow · temp" to JsonPrimitive(2))
        }

        test("valuesFormatByType") {
            // when and then
            JsonPrimitive(true).number() shouldBe 1.0
            JsonPrimitive(false).number() shouldBe 0.0
            JsonPrimitive("text").number().shouldBeNull()
            JsonNull.format() shouldBe ""
            JsonPrimitive(2.345).format(decimals = 1) shouldBe "2.3"
            JsonPrimitive("2026-10-27T12:30:00Z").format(zone = TimeZone.of("Europe/Warsaw")) shouldBe "10-27 13:30"
            buildJsonArray { add(JsonPrimitive(1)) }.format() shouldBe "[1]"
        }

        test("formattedValueCarriesTheUnit") {
            // when and then
            formatValue(21.456, WidgetOptions(decimals = 1), UnitDisplay("°C")) shouldBe "21.5 °C"
            formatValue(3.0, WidgetOptions(), UnitDisplay(null)) shouldBe "3"
        }

        test("thresholdPicksTheHighestReachedColor") {
            // given
            val thresholds = listOf(Threshold(50.0, "#FF0000"), Threshold(0.0, "#00FF00"), Threshold(20.0, "bad"))

            // when and then
            thresholdColor(10.0, thresholds) shouldBe Color(0xFF00FF00)
            thresholdColor(60.0, thresholds) shouldBe Color(0xFFFF0000)
            thresholdColor(30.0, thresholds).shouldBeNull()
            thresholdColor(-1.0, thresholds).shouldBeNull()
            parseColor("#12345").shouldBeNull()
        }

        test("shortTimeFallsBackOnUnparsableTimestamps") {
            // when and then
            "2026-10-27T12:30:99Z".shortTime() shouldBe "10-27 12:30"
            "today".shortTime() shouldBe "today"
        }

        test("remainingModelsRoundTrip") {
            // given
            val status = SourceStatus("erroring", rejections = listOf(Rejection("2026-10-27T12:00:00Z", "bad json", "{")))
            val query = Query("weather", measures = listOf(QueryMeasure("temp", "at_max", "hum", "peak")))
            val start = OidcStart("https://idp/auth", "s1", "v1")
            val login = OidcLoginRequest("c1", "v1", "http://127.0.0.1/callback")
            val options = WidgetOptions(thresholds = listOf(Threshold(1.0, "#FFFFFF")))

            // when and then
            platformJson.decodeFromString<SourceStatus>(platformJson.encodeToString(status)) shouldBe status
            platformJson.decodeFromString<Query>(platformJson.encodeToString(query)) shouldBe query
            platformJson.decodeFromString<OidcStart>(platformJson.encodeToString(start)) shouldBe start
            platformJson.decodeFromString<OidcLoginRequest>(platformJson.encodeToString(login)) shouldBe login
            platformJson.decodeFromString<WidgetOptions>(platformJson.encodeToString(options)) shouldBe options
        }
    })
