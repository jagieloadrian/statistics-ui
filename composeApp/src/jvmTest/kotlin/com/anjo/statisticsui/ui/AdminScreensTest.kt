package com.anjo.statisticsui.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.Counts
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.Dimension
import com.anjo.statisticsui.model.FieldSpec
import com.anjo.statisticsui.model.MappingTestResult
import com.anjo.statisticsui.model.Measure
import com.anjo.statisticsui.model.PubSubInfo
import com.anjo.statisticsui.model.Rejection
import com.anjo.statisticsui.model.Rule
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.model.SourceStatus
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.admin.DatasetsScreen
import com.anjo.statisticsui.ui.admin.SourcesScreen
import com.anjo.statisticsui.ui.admin.UsersScreen
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@OptIn(ExperimentalTestApi::class)
class AdminScreensTest :
    FunSpec({

        val pico =
            Source(
                id = "pico",
                label = "Pico",
                dataset = "weather",
                mapping = listOf(Rule(vals = mapOf("temp" to FieldSpec(path = "t", min = -40.0)))),
            )
        val status =
            SourceStatus(
                "receiving",
                lastRecordAt = "2026-10-27T12:00:00Z",
                accepted = Counts(3, 40),
                rejected = Counts(1, 2),
                rejections = listOf(Rejection("2026-10-27T11:00:00Z", "temp missing", "{\"x\":1}")),
            )

        fun FakeBackend.sources() {
            reply("datasets", listOf(DefinitionSummary("weather")))
            reply("auth/config", AuthConfig(pubsub = PubSubInfo("kafka", "ingest")))
            reply("sources", listOf(DefinitionSummary("pico", "Pico"), DefinitionSummary("lab")))
            reply("sources/pico/status", status)
            reply("sources/lab/status", SourceStatus("silent"))
            reply("sources/pico", pico)
            ok("sources/pico", HttpMethod.Put)
        }

        test("sourceDetailsShowStatusAndOperations") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.sources()
                backend.json("sources/pico/credential", """{"token":"fresh-token"}""", HttpMethod.Post)
                backend.reply(
                    "sources/pico/mapping-test",
                    MappingTestResult(listOf(JsonPrimitive(21)), listOf("hum missing")),
                    HttpMethod.Post,
                )
                show(backend) { SourcesScreen(rememberNavController()) }
                awaitText("silent")

                // when
                click("Pico")

                // then
                awaitText("Accepted: 3 last hour, 40 last day")
                awaitText("2026-10-27T11:00:00Z  temp missing")
                awaitText("Push address: /api/v2/ingest/pico")

                // when
                click("Rotate credential")
                click("Click again: the current token stops working")

                // then
                awaitText("fresh-token")

                // when
                click("Test mapping")

                // then
                awaitText("Required")

                // when
                type("Sample payload (JSON)", """{"t":21}""")
                click("Test mapping")

                // then
                awaitText("1 record(s)")
                awaitText("hum missing")

                // when
                onNodeWithContentDescription("Refresh").performClick()
                onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).reveal().performClick()

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Get, "sources/pico/status").size >= 2 }
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Put, "sources/pico").isNotEmpty() }
                backend.sent(HttpMethod.Put, "sources/pico").single().body shouldContain "\"paused\":true"
            }
        }

        test("sourceFormEditsMappingAndSaves") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.sources()
                show(backend) { SourcesScreen(rememberNavController()) }
                awaitText("Pico")
                click("Pico")
                awaitText("Save source")

                // when
                type("Label", "Pico W")
                click("Add field")
                type("Target", "city", index = 1)
                type("Path", "c", index = 1)
                onAllNodes(hasText("Dimension") and isToggleable())[1].reveal().performClick()
                onAllNodes(hasText("Required") and isToggleable())[0].reveal().performClick()
                type("Max", "60")
                type("Rate limit per second", "7")
                click("Save source")

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Put, "sources/pico").isNotEmpty() }
                val saved = backend.sent(HttpMethod.Put, "sources/pico").single().body
                saved shouldContain "\"label\":\"Pico W\""
                saved shouldContain "\"rateLimitPerSecond\":7"
                saved shouldContain "\"city\":{\"path\":\"c\""
                saved shouldContain "\"max\":60.0"

                // when
                type("Mapping JSON", "[{\"vals\":{\"hum\":{\"path\":\"h\"}}}]")
                click("Apply JSON")

                // then
                waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("hum").fetchSemanticsNodes().isNotEmpty() }

                // when
                onAllNodesWithContentDescription("Remove")[0].reveal().performClick()
                type("Mapping JSON", "[")
                click("Apply JSON")

                // then
                awaitText("Not a valid mapping", substring = true)
            }
        }

        test("newPubsubSourceShowsTheBrokerAddress") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.sources()
                show(backend) { SourcesScreen(rememberNavController()) }
                awaitText("Pico")

                // when
                click("New source")
                type("ID", "lab")
                pick("Channel", "pubsub")
                click("Save source")

                // then
                awaitText("Kafka topic ingest, record key lab, header Authorization: Bearer <source token>")
                awaitText("Already exists")
                awaitText("A pubsub source needs at least one rule, each with identity")
            }
        }

        test("deletedSourceCanBeRestored") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.sources()
                show(backend) { SourcesScreen(rememberNavController()) }
                awaitText("Pico")
                click("Pico")

                // when
                click("Delete source")

                // then
                awaitText("Deleted source pico")

                // when
                click("Undo")

                // then
                awaitText("Save source")
            }
        }

        test("compactSourcesShowListOrDetail") {
            uiTest(Size(400f, 900f)) {
                // given
                val backend = FakeBackend()
                backend.sources()
                show(backend) { SourcesScreen(rememberNavController()) }
                awaitText("Pico")

                // when
                click("Pico")

                // then
                awaitText("All sources")

                // when
                click("All sources")

                // then
                awaitText("New source")
            }
        }

        test("bundleExportsAndImports") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.sources()
                backend.reply("export", buildJsonObject { put("sources", JsonPrimitive(1)) })
                backend.json("import", """{"created":1,"updated":2}""", HttpMethod.Post)
                show(backend) { SourcesScreen(rememberNavController()) }
                awaitText("Pico")

                // when
                click("Export / import")
                click("Export")

                // then
                awaitText("\"sources\": 1", substring = true)

                // when
                click("Import")

                // then
                awaitText("Imported: 1 created, 2 updated")

                // when
                backend.json("import", """{"created":0,"updated":0,"errors":["bad id"]}""", HttpMethod.Post, HttpStatusCode.BadRequest)
                type("Bundle JSON", "{}")
                click("Import")

                // then
                awaitText("Nothing imported (1 errors):")
                awaitText("bad id")

                // when
                click("Close")

                // then
                waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Bundle JSON").fetchSemanticsNodes().isEmpty() }
            }
        }

        test("datasetFormEditsAndForcesConflictingSave") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                val weather = Dataset(id = "weather", label = "Weather", measures = listOf(Measure("temp", unit = "°C")))
                backend.reply("datasets", listOf(DefinitionSummary("weather", "Weather")))
                backend.reply("datasets/weather", weather)
                backend.fail("datasets/weather", HttpStatusCode.Conflict, "Drops records", HttpMethod.Put)
                show(backend) { DatasetsScreen(rememberNavController()) }
                awaitText("Weather")
                click("Weather")
                awaitText("Save dataset")

                // when
                click("Add measure")
                type("Name", "hum", index = 1)
                pick("Type", "boolean", index = 1)
                type("Unit", "%", index = 1)
                click("Add dimension")
                type("Name", "city", index = 2)
                click("Add calculated measure")
                type("Name", "feels", index = 3)
                type("Expression", "temp - 2")
                type("Retention in days", "30")
                click("Save dataset")

                // then
                awaitText("Drops records")

                // when
                backend.ok("datasets/weather", HttpMethod.Put)
                click("Save anyway")

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Put, "datasets/weather").size == 2 }
                val forced = backend.sent(HttpMethod.Put, "datasets/weather").last()
                forced.parameters["force"] shouldBe "true"
                forced.body shouldContain "\"calculated\":{\"feels\":\"temp - 2\"}"
                forced.body shouldContain "\"dimensions\":[{\"name\":\"city\""
            }
        }

        test("datasetIsDeletedAfterConfirmation") {
            uiTest(Size(1400f, 1000f)) {
                // given
                val backend = FakeBackend()
                backend.reply("datasets", listOf(DefinitionSummary("weather")))
                backend.reply("datasets/weather", Dataset(id = "weather", dimensions = listOf(Dimension("city"))))
                backend.ok("datasets/weather", HttpMethod.Delete)
                show(backend) { DatasetsScreen(rememberNavController()) }
                awaitText("weather")
                click("weather")

                // when
                click("Delete dataset")
                click("Click again: deletes the dataset and all its records")

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Delete, "datasets/weather").isNotEmpty() }
                awaitText("New dataset")
            }
        }

        test("newDatasetIsValidated") {
            uiTest(Size(400f, 900f)) {
                // given
                val backend = FakeBackend()
                backend.reply("datasets", listOf(DefinitionSummary("weather")))
                show(backend) { DatasetsScreen(rememberNavController()) }
                awaitText("weather")

                // when
                click("New dataset")
                type("ID", "weather")
                type("Retention in days", "-3")
                click("Save dataset")

                // then
                awaitText("Already exists")
                awaitText("A whole number above 0, or empty")

                // when
                click("All datasets")

                // then
                awaitText("New dataset")
            }
        }

        test("usersAreInvitedPromotedAndDeleted") {
            uiTest {
                // given
                val backend = FakeBackend()
                val bob = User(2, "bob@example.com", "Bob", "viewer")
                backend.reply("users", listOf(bob))
                backend.reply("users", User(3, "cy@example.com", role = "editor"), HttpMethod.Post)
                backend.ok("users/2", HttpMethod.Put)
                show(backend) { UsersScreen(rememberNavController()) }
                awaitText("bob@example.com")

                // when
                click("Create user")

                // then
                awaitText("Required")

                // when
                type("Email", "cy@example.com")
                type("Name (optional)", "Cy")
                type("Initial password", "long-enough")
                pick("Role", "editor")
                click("Create user")

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Post, "users").isNotEmpty() }
                backend.sent(HttpMethod.Post, "users").single().body shouldContain "\"role\":\"editor\""

                // when
                pick("Role", "admin", index = 1)

                // then
                waitUntil(timeoutMillis = 5_000) { backend.sent(HttpMethod.Put, "users/2").isNotEmpty() }

                // when
                click("Delete")

                // then
                awaitText("Deleted bob@example.com")
            }
        }
    })
