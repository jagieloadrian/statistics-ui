package com.anjo.statisticsui.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.Dimension
import com.anjo.statisticsui.model.DimensionValue
import com.anjo.statisticsui.model.EDITOR
import com.anjo.statisticsui.model.Measure
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.Series
import com.anjo.statisticsui.model.Threshold
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.model.WidgetOptions
import com.anjo.statisticsui.ui.dashboard.DashboardScreen
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalTestApi::class)
class DashboardScreenTest :
    FunSpec({

        val ann = User(id = 1, email = "ann@example.com", name = "Ann", role = EDITOR)
        val query = Query("weather", filter = mapOf("city" to listOf("\$city")), bucket = "\$bucket")
        val thresholds = listOf(Threshold(0.0, "#00FF00"), Threshold(21.0, "#FF0000"))
        val dashboard =
            Dashboard(
                id = "d1",
                title = "Weather",
                variables =
                    listOf(
                        Variable("city", "dimension", dataset = "weather", dimension = "city"),
                        Variable("bucket", "bucket", options = listOf("1h", "1d")),
                        Variable("range", "timeRange", default = "P7D"),
                    ),
                widgets =
                    listOf(
                        Widget("temp", "Temp in \$city", "line", w = 12, query = query),
                        Widget("area", "", "area", w = 4, query = query, options = WidgetOptions(colors = mapOf("temp" to "#123456"))),
                        Widget("bar", "", "bar", w = 4, query = query, options = WidgetOptions(min = -5.0, max = 40.0)),
                        Widget("stat", "", "stat", w = 4, query = query, options = WidgetOptions(unit = "°C", thresholds = thresholds)),
                        Widget("gauge", "", "gauge", query = query, options = WidgetOptions(decimals = 1, thresholds = thresholds)),
                        Widget("table", "", "table", query = query),
                        Widget("pie", "", "pie", query = query),
                    ),
            )
        val result =
            QueryResult(
                "1h",
                listOf(
                    Series(
                        mapOf("city" to "Krakow"),
                        listOf("ts", "temp"),
                        listOf(
                            listOf(JsonPrimitive("2026-10-27T10:00:00Z"), JsonPrimitive(20)),
                            listOf(JsonPrimitive("2026-10-27T11:00:00Z"), JsonPrimitive(22.5)),
                        ),
                    ),
                ),
            )

        val krakow = DimensionValue("Krakow", "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z", 3)
        val gdansk = DimensionValue("Gdansk", "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z", 5)

        fun FakeBackend.weather() {
            session.signIn("t1", ann)
            reply("dashboards/d1", dashboard)
            reply("datasets/weather/dimensions/city/values", listOf(krakow))
            reply("query", result, HttpMethod.Post)
        }

        test("widgetsRenderEveryType") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.weather()

                // when
                show(backend) { DashboardScreen("d1", rememberNavController()) }

                // then
                awaitText("Temp in Krakow")
                awaitText("● Krakow · temp")
                awaitText("22.5 °C")
                awaitText("22.5")
                awaitText("Widget type 'pie' is not supported yet")
                awaitText("Bucket: 1h")
                awaitText("Krakow")
                awaitText("temp")
            }
        }

        test("variablesAndRangeRerunQueries") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.weather()
                show(backend) { DashboardScreen("d1", rememberNavController()) }
                awaitText("city: Krakow")
                backend.reply("datasets/weather/dimensions/city/values", listOf(krakow, gdansk))

                // when
                click("city: Krakow")
                awaitText("Gdansk")
                type("Search", "Gd")

                // then
                waitUntil(timeoutMillis = 5_000) {
                    backend.sent(HttpMethod.Get, "datasets/weather/dimensions/city/values").any { it.parameters["search"] == "Gd" }
                }

                // when
                click("Gdansk")

                // then
                awaitText("Temp in Gdansk")

                // when
                pick("bucket", "1d")
                click("Last 1h")

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Post, "query").any { "\"bucket\":\"1d\"" in it.body } }
            }
        }

        test("widgetErrorsAndEmptyResults") {
            uiTest(Size(400f, 900f)) {
                // given
                val backend = FakeBackend()
                backend.weather()
                backend.fail("query", HttpStatusCode.BadRequest, "Unknown measure hum", HttpMethod.Post)

                // when
                show(backend) { DashboardScreen("d1", rememberNavController()) }

                // then
                awaitText("Missing data definition")
                awaitText("Unknown measure hum")

                // when
                backend.reply(
                    "query",
                    QueryResult(series = listOf(Series(columns = listOf("ts", "temp"), rows = emptyList()))),
                    HttpMethod.Post,
                )
                click("Last 1h")

                // then
                awaitText("No data in range")

                // when
                backend.fail("query", HttpStatusCode.InternalServerError, "Database down", HttpMethod.Post)
                click("Last 30d")

                // then
                awaitText("Database down")
            }
        }

        test("missingDashboardShowsTheError") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.session.signIn("t1", ann)
                backend.fail("dashboards/gone", HttpStatusCode.NotFound, "No dashboard gone")

                // when
                show(backend) { DashboardScreen("gone", rememberNavController()) }

                // then
                awaitText("No dashboard gone")
            }
        }

        test("editorChangesSettingsWidgetsAndSharing") {
            uiTest(Size(1400f, 1400f)) {
                // given
                val backend = FakeBackend()
                backend.weather()
                backend.reply("datasets", listOf(DefinitionSummary("weather")))
                backend.reply("users", listOf(ann, User(2, "bob@example.com", role = "viewer")))
                backend.json("dashboards/d1/shares", """{"userIds":[]}""")
                backend.reply(
                    "datasets/weather",
                    Dataset(
                        id = "weather",
                        measures = listOf(Measure("temp"), Measure("hum")),
                        dimensions = listOf(Dimension("city")),
                        calculated = mapOf("feels" to "temp - 2"),
                    ),
                )
                backend.ok("dashboards/d1/shares", HttpMethod.Put)
                backend.ok("dashboards/d1", HttpMethod.Put)
                show(backend) { DashboardScreen("d1", rememberNavController()) }
                awaitText("Temp in Krakow")

                // when
                click("Edit dashboard")
                click("Cancel")
                click("Edit dashboard")
                awaitText("Save dashboard")
                type("Title", "Weather now")
                type("Refresh every (seconds)", "2")
                click("Save dashboard")

                // then
                awaitText("Empty or at least 5")

                // when
                type("Refresh every (seconds)", "30")
                pick("Visible to", "users")
                click("bob@example.com")
                click("Add variable")
                type("Name", "site", index = 3)
                pick("Type", "bucket", index = 3)
                type("Options", "raw, 1m", index = 1)
                pick("Default", "1m", index = 1)
                click("Add widget")
                type("Title", "Humidity", index = 1)
                pick("Type", "gauge", index = 4)
                click("Add measure")
                pick("Aggregation", "at_max")
                pick("By measure", "hum")
                type("Alias", "peak")
                click("Add filter")
                type("Values", "Krakow, \$city")
                onNode(hasText("city") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).reveal().performClick()
                type("Unit", "%")
                type("Decimals", "1")
                type("Min", "0")
                type("Max", "100")
                click("Add color")
                type("Series or measure", "peak")
                click("Add threshold")
                type("From value", "80")
                onAllNodesWithContentDescription("Move up").onLast().reveal().performClick()
                onAllNodesWithContentDescription("Move down")[0].reveal().performClick()
                onAllNodesWithContentDescription("Delete").onLast().reveal().performClick()
                click("Save dashboard")

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Put, "dashboards/d1").isNotEmpty() }
                backend.sent(HttpMethod.Put, "dashboards/d1/shares").single().body shouldContain "[2]"
                val saved = backend.sent(HttpMethod.Put, "dashboards/d1").single().body
                saved shouldContain "\"title\":\"Weather now\""
                saved shouldContain "\"refreshSeconds\":30"
                saved shouldContain "\"options\":[\"raw\",\"1m\"]"
                saved shouldContain "\"title\":\"Humidity\""
                saved shouldContain "\"agg\":\"at_max\""
                saved shouldContain "\"groupBy\":[\"city\"]"
                saved shouldContain "\"thresholds\":[{\"value\":80.0"
                awaitText("Edit dashboard")
            }
        }
    })
