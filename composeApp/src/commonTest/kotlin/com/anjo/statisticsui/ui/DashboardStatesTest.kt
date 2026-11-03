package com.anjo.statisticsui.ui

import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.api.ApiException
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.DimensionValue
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.Series
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.settle
import com.anjo.statisticsui.stateScope
import com.anjo.statisticsui.ui.dashboard.DashboardEditorState
import com.anjo.statisticsui.ui.dashboard.DashboardState
import com.anjo.statisticsui.ui.dashboard.NewDashboardState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.days

class DashboardStatesTest :
    FunSpec({

        val temp = Widget(id = "temp", type = "line", query = Query("weather", filter = mapOf("city" to listOf("\$city"))))
        val hum = Widget(id = "hum", type = "stat", query = Query("weather", bucket = "\$bucket"))
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
                widgets = listOf(temp, hum),
            )
        val result =
            QueryResult("1h", listOf(Series(columns = listOf("ts", "temp"), rows = listOf(listOf(JsonPrimitive("t"), JsonPrimitive(1))))))

        fun FakeBackend.weather() {
            reply("dashboards/d1", dashboard)
            reply("datasets/weather/dimensions/city/values", listOf(DimensionValue("Krakow", "2026-01-01", "2026-02-01", 3)))
        }

        test("loadResolvesVariableValuesAndRange") {
            // given
            val backend = FakeBackend()
            backend.weather()
            val state = DashboardState("d1", backend.api)

            // when
            state.load()

            // then
            state.dashboard shouldBe dashboard
            state.values shouldBe mapOf("city" to listOf("Krakow"), "bucket" to listOf("1h"))
            state.range shouldBe 7.days
            backend.sent(HttpMethod.Get, "datasets/weather/dimensions/city/values").single().parameters["limit"] shouldBe "1"
        }

        test("loadErrorIsShown") {
            // given
            val backend = FakeBackend()
            backend.fail("dashboards/d1", HttpStatusCode.Forbidden, "Not shared with you")
            val state = DashboardState("d1", backend.api)

            // when
            state.load()

            // then
            state.error shouldBe "Not shared with you"
            state.dashboard.shouldBeNull()
        }

        test("queriesRunOncePerWidgetWithResolvedVariables") {
            // given
            val backend = FakeBackend()
            backend.weather()
            backend.reply("query", result, HttpMethod.Post)
            val state = DashboardState("d1", backend.api)
            state.load()

            // when
            state.runQueries()

            // then
            state.results.keys shouldBe setOf("temp", "hum")
            state.results["temp"]?.getOrNull() shouldBe result
            val bodies = backend.sent(HttpMethod.Post, "query").map { it.body }
            bodies.any { "\"city\":[\"Krakow\"]" in it } shouldBe true
            bodies.any { "\"bucket\":\"1h\"" in it } shouldBe true
        }

        test("failedQueryIsKeptPerWidget") {
            // given
            val backend = FakeBackend()
            backend.weather()
            backend.fail("query", HttpStatusCode.BadRequest, "Unknown measure", HttpMethod.Post)
            val state = DashboardState("d1", backend.api)
            state.load()

            // when
            state.runQueries()

            // then
            (state.results["hum"]?.exceptionOrNull() as ApiException).status shouldBe 400
        }

        test("editingPausesQueriesAndSavingKeepsDeclaredValues") {
            // given
            val backend = FakeBackend()
            backend.weather()
            backend.ok("dashboards/d1", HttpMethod.Put)
            val state = DashboardState("d1", backend.api)
            state.load()
            state.select("city", listOf("Gdansk"))
            state.editing = true

            // when
            state.runQueries()

            // then
            backend.sent(HttpMethod.Post, "query").shouldBeEmpty()

            // when
            state.save(dashboard.copy(variables = dashboard.variables.take(1)))

            // then
            state.editing shouldBe false
            state.values shouldBe mapOf("city" to listOf("Gdansk"))
            state.dashboard?.variables?.size shouldBe 1
        }

        test("editorLoadsDatasetsUsersAndShares") {
            // given
            val backend = FakeBackend()
            backend.reply("datasets", listOf(DefinitionSummary("weather")))
            backend.reply("users", listOf(User(7, "ann@example.com", role = "viewer")))
            backend.json("dashboards/d1/shares", """{"userIds":[7]}""")
            backend.reply("datasets/weather", Dataset(id = "weather"))
            val state = DashboardEditorState(dashboard, backend.api, stateScope())

            // when
            state.load()
            state.loadDatasets(setOf("weather", "gone"))

            // then
            state.datasetIds shouldBe listOf("weather")
            state.users.map { it.id } shouldBe listOf(7L)
            state.shared shouldBe setOf(7L)
            state.datasets.keys shouldBe setOf("weather")
            state.error shouldBe "No reply for datasets/gone"
            state.referencedDatasets shouldBe setOf("weather")
        }

        test("editorEditsWidgets") {
            // given
            val state = DashboardEditorState(dashboard.copy(widgets = listOf(temp)), FakeBackend().api, stateScope())

            // when
            state.addWidget()

            // then
            state.dashboard.widgets.map { it.id } shouldBe listOf("temp", "w1")
            state.expandedWidget shouldBe 1

            // when
            state.moveWidget(1, 0)
            state.updateWidget(1, temp.copy(title = "Temperature"))

            // then
            state.dashboard.widgets.map { it.id } shouldBe listOf("w1", "temp")
            state.dashboard.widgets[1].title shouldBe "Temperature"
            state.expandedWidget shouldBe 0

            // when
            state.toggleWidget(0)
            state.removeWidget(1)

            // then
            state.expandedWidget.shouldBeNull()
            state.dashboard.widgets.map { it.id } shouldBe listOf("w1")
        }

        test("editorValidatesThenSavesSharesAndDashboard") {
            // given
            val backend = FakeBackend()
            backend.ok("dashboards/d1/shares", HttpMethod.Put)
            val scope = stateScope()
            val state = DashboardEditorState(dashboard.copy(refreshSeconds = 1), backend.api, scope)
            val saved = mutableListOf<Dashboard>()

            // when
            state.save { saved += it }
            scope.settle()

            // then
            state.errors shouldBe mapOf("refresh" to "Empty or at least 5")
            saved.shouldBeEmpty()

            // when
            state.dashboard = state.dashboard.copy(refreshSeconds = 30, visibility = "users")
            state.toggleShare(3)
            state.toggleShare(4)
            state.toggleShare(3)
            state.save { saved += it }
            scope.settle()

            // then
            saved.single().refreshSeconds shouldBe 30
            backend.sent(HttpMethod.Put, "dashboards/d1/shares").single().body shouldBe """{"userIds":[4]}"""
        }

        test("editorShowsSaveErrors") {
            // given
            val scope = stateScope()
            val state = DashboardEditorState(dashboard, FakeBackend().api, scope)

            // when
            state.save { error("Dashboard changed meanwhile") }
            scope.settle()

            // then
            state.error shouldBe "Dashboard changed meanwhile"
        }

        test("newDashboardValidatesThenCreates") {
            // given
            val backend = FakeBackend()
            backend.ok("dashboards/energy", HttpMethod.Put)
            val scope = stateScope()
            val state = NewDashboardState(listOf("d1"), backend.api, scope)
            var created: String? = null

            // when
            state.id = "d1"
            state.create { created = it }

            // then
            state.errors shouldBe mapOf("id" to "Already exists")

            // when
            state.id = " energy "
            state.title = " Energy "
            state.create { created = it }
            scope.settle()

            // then
            created shouldBe "energy"
            backend.sent(HttpMethod.Put, "dashboards/energy").single().body shouldContain "\"title\":\"Energy\""
        }

        test("newDashboardShowsServerErrors") {
            // given
            val backend = FakeBackend()
            backend.fail("dashboards/energy", HttpStatusCode.Forbidden, "Editors only", HttpMethod.Put)
            val scope = stateScope()
            val state = NewDashboardState(emptyList(), backend.api, scope)
            state.id = "energy"

            // when
            state.create {}
            scope.settle()

            // then
            state.error shouldBe "Editors only"
        }
    })
