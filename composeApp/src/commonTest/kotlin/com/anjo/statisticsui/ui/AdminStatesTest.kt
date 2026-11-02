package com.anjo.statisticsui.ui

import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.awaitSnackbar
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.FieldSpec
import com.anjo.statisticsui.model.MapRow
import com.anjo.statisticsui.model.MappingTestResult
import com.anjo.statisticsui.model.Measure
import com.anjo.statisticsui.model.PubSubInfo
import com.anjo.statisticsui.model.Rule
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.model.SourceStatus
import com.anjo.statisticsui.settle
import com.anjo.statisticsui.stateScope
import com.anjo.statisticsui.ui.admin.BundleState
import com.anjo.statisticsui.ui.admin.DatasetFormState
import com.anjo.statisticsui.ui.admin.DatasetsState
import com.anjo.statisticsui.ui.admin.SourceFormState
import com.anjo.statisticsui.ui.admin.SourceOpsState
import com.anjo.statisticsui.ui.admin.SourcesState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AdminStatesTest :
    FunSpec({

        val weather = Dataset(id = "weather", measures = listOf(Measure("temp")))
        val pico = Source(id = "pico", dataset = "weather", mapping = listOf(Rule(vals = mapOf("temp" to FieldSpec(path = "t")))))

        test("datasetsSelectSaveAndDelete") {
            // given
            val backend = FakeBackend()
            backend.reply("datasets", listOf(DefinitionSummary("weather")))
            backend.reply("datasets/weather", weather)
            backend.ok("datasets/weather", HttpMethod.Put)
            backend.ok("datasets/weather", HttpMethod.Delete)
            val scope = stateScope()
            val state = DatasetsState(backend.api, scope)

            // when
            state.reload().join()
            state.select("weather").join()

            // then
            state.datasets.map { it.id } shouldBe listOf("weather")
            state.selectedId shouldBe "weather"

            // when
            state.save(weather.copy(label = "Weather"), force = false)
            scope.settle()

            // then
            state.selected?.label shouldBe "Weather"
            backend.sent(HttpMethod.Put, "datasets/weather").single().body shouldContain "\"label\":\"Weather\""

            // when
            state.delete("weather", force = true)
            scope.settle()

            // then
            state.selected.shouldBeNull()
            backend.sent(HttpMethod.Delete, "datasets/weather").single().parameters["force"] shouldBe "true"
        }

        test("datasetsStartNewAndClose") {
            // given
            val backend = FakeBackend()
            backend.fail("datasets", HttpStatusCode.Forbidden, "Admins only")
            val state = DatasetsState(backend.api, stateScope())

            // when
            state.reload().join()
            state.startNew()

            // then
            state.error shouldBe "Admins only"
            state.isNew shouldBe true
            state.selectedId.shouldBeNull()

            // when
            state.close()

            // then
            state.selected.shouldBeNull()
        }

        test("datasetFormValidatesBeforeSaving") {
            // given
            val scope = stateScope()
            val form = DatasetFormState(Dataset(id = ""), listOf("weather"), scope)
            var saved = false

            // when
            form.dataset = Dataset(id = "weather", measures = listOf(Measure("ts")))
            form.retention = "-1"
            form.save { _, _ -> saved = true }
            scope.settle()

            // then
            form.errors shouldBe
                mapOf("id" to "Already exists", "names" to "'ts' is reserved", "retention" to "A whole number above 0, or empty")
            saved shouldBe false
        }

        test("datasetFormOffersForceOnConflict") {
            // given
            val backend = FakeBackend()
            backend.fail("datasets/weather", HttpStatusCode.Conflict, "Drops measure hum", HttpMethod.Put)
            backend.reply("datasets", listOf(DefinitionSummary("weather")))
            val scope = stateScope()
            val state = DatasetsState(backend.api, scope)
            val form = DatasetFormState(weather, emptyList(), scope)
            form.calculated = listOf("double" to "temp * 2")
            form.retention = "30"

            // when
            form.save(state::save)
            scope.settle()

            // then
            form.error shouldBe "Drops measure hum"
            form.retry?.first shouldBe "Save anyway"

            // when
            backend.ok("datasets/weather", HttpMethod.Put)
            form.runRetry(form.retry!!.second)
            scope.settle()

            // then
            form.error.shouldBeNull()
            form.retry.shouldBeNull()
            val forced = backend.sent(HttpMethod.Put, "datasets/weather").last()
            forced.parameters["force"] shouldBe "true"
            forced.body shouldContain "\"retentionDays\":30"
            forced.body shouldContain "\"double\":\"temp * 2\""
        }

        test("datasetFormDeleteFailureWithoutConflictHasNoRetry") {
            // given
            val scope = stateScope()
            val form = DatasetFormState(weather, emptyList(), scope)

            // when
            form.delete { error("Network down") }
            scope.settle()

            // then
            form.error shouldBe "Network down"
            form.retry.shouldBeNull()
        }

        test("sourcesReloadLoadsStatesAndBroker") {
            // given
            val backend = FakeBackend()
            backend.reply("datasets", listOf(DefinitionSummary("weather")))
            backend.reply("auth/config", AuthConfig(pubsub = PubSubInfo("nats", "ingest")))
            backend.reply("sources", listOf(DefinitionSummary("pico"), DefinitionSummary("lost")))
            backend.reply("sources/pico/status", SourceStatus("receiving"))
            backend.fail("sources/lost/status", HttpStatusCode.NotFound, "Unknown source lost")
            val state = SourcesState(backend.api, stateScope())

            // when
            state.reload().join()

            // then
            state.datasets shouldBe listOf("weather")
            state.pubsub shouldBe PubSubInfo("nats", "ingest")
            state.states.toMap() shouldBe mapOf("pico" to "receiving")
            state.error shouldBe "Unknown source lost"
        }

        test("sourcesSelectNewAndSaved") {
            // given
            val backend = FakeBackend()
            backend.reply("datasets", listOf(DefinitionSummary("weather")))
            backend.reply("auth/config", AuthConfig())
            backend.reply("sources", listOf(DefinitionSummary("pico")))
            backend.reply("sources/pico/status", SourceStatus("silent"))
            backend.reply("sources/pico", pico)
            val scope = stateScope()
            val state = SourcesState(backend.api, scope)
            state.reload().join()

            // when
            state.startNew()

            // then
            state.selected shouldBe Source(id = "", dataset = "weather")
            state.selectedId.shouldBeNull()

            // when
            state.select("pico").join()

            // then
            state.selectedId shouldBe "pico"

            // when
            state.onSaved(pico.copy(label = "Pico"))
            state.bundleOpen = true
            state.closeBundle()
            scope.settle()

            // then
            state.selected?.label shouldBe "Pico"
            state.bundleOpen shouldBe false
            backend.sent(HttpMethod.Get, "sources").size shouldBe 3
        }

        test("sourcesDeleteUndoRestoresTheSelection") {
            // given
            val backend = FakeBackend()
            backend.reply("sources/pico", pico)
            val scope = stateScope()
            val state = SourcesState(backend.api, scope)
            state.select("pico").join()

            // when
            state.delete("pico")

            // then
            state.selected.shouldBeNull()

            // when
            state.snackbar.awaitSnackbar().performAction()
            scope.settle()

            // then
            state.selected shouldBe pico
            backend.sent(HttpMethod.Delete, "sources/pico").shouldBeEmpty()
        }

        test("sourcesDeleteRemovesAndReloads") {
            // given
            val backend = FakeBackend()
            backend.ok("sources/pico", HttpMethod.Delete)
            backend.reply("datasets", emptyList<DefinitionSummary>())
            backend.reply("auth/config", AuthConfig())
            backend.reply("sources", emptyList<DefinitionSummary>())
            val scope = stateScope()
            val state = SourcesState(backend.api, scope)

            // when
            state.delete("pico")
            state.snackbar.awaitSnackbar().dismiss()
            scope.settle()

            // then
            backend.sent(HttpMethod.Delete, "sources/pico").size shouldBe 1
            state.listed.shouldBeEmpty()
        }

        test("sourceFormKeepsRowsAndJsonInSync") {
            // given
            val form = SourceFormState(pico, isNew = false, existingIds = emptyList(), api = FakeBackend().api, scope = stateScope())

            // when
            form.editRows(form.rows + MapRow(target = "city", dim = true, path = "c"))

            // then
            form.source.mapping
                .single()
                .dims.keys shouldBe setOf("city")
            form.mappingJson shouldContain "\"city\""

            // when
            form.mappingJson = """[{"vals":{"hum":{"path":"h"}}}]"""
            form.applyJson().join()

            // then
            form.rows.map { it.target } shouldBe listOf("hum")
            form.source.mapping
                .single()
                .vals.keys shouldBe setOf("hum")
        }

        test("sourceFormRejectsInvalidJson") {
            // given
            val form = SourceFormState(pico, isNew = false, existingIds = emptyList(), api = FakeBackend().api, scope = stateScope())

            // when
            form.mappingJson = "[{"
            form.applyJson().join()

            // then
            form.error.shouldNotBeNull()
            form.rows.map { it.target } shouldBe listOf("temp")
        }

        test("sourceFormValidatesThenSaves") {
            // given
            val backend = FakeBackend()
            backend.ok("sources/pico2", HttpMethod.Put)
            val scope = stateScope()
            val form = SourceFormState(pico.copy(id = ""), isNew = true, existingIds = listOf("pico"), api = backend.api, scope = scope)
            val saved = mutableListOf<Source>()

            // when
            form.rate = "0"
            form.save { saved += it }

            // then
            form.errors shouldBe
                mapOf(
                    "id" to "Required",
                    "rate" to "A whole number above 0",
                )

            // when
            form.edit(form.source.copy(id = "pico2"))
            form.rate = "5"
            form.save { saved += it }
            scope.settle()

            // then
            saved.single().rateLimitPerSecond shouldBe 5

            // when
            form.edit(form.source.copy(id = "pico"))

            // then
            form.errors["id"] shouldBe "Already exists"
        }

        test("sourceFormPauses") {
            // given
            val backend = FakeBackend()
            backend.ok("sources/pico", HttpMethod.Put)
            val scope = stateScope()
            val form = SourceFormState(pico, isNew = false, existingIds = emptyList(), api = backend.api, scope = scope)
            var saved: Source? = null

            // when
            form.setPaused(true) { saved = it }
            scope.settle()

            // then
            saved?.paused shouldBe true
            backend.sent(HttpMethod.Put, "sources/pico").single().body shouldContain "\"paused\":true"
        }

        test("sourceOpsStatusCredentialAndMappingTest") {
            // given
            val backend = FakeBackend()
            backend.reply("sources/pico/status", SourceStatus("erroring"))
            backend.json("sources/pico/credential", """{"token":"tok"}""", HttpMethod.Post)
            backend.reply("sources/pico/mapping-test", MappingTestResult(errors = listOf("temp missing")), HttpMethod.Post)
            val scope = stateScope()
            val ops = SourceOpsState("pico", backend.api, scope)

            // when
            ops.refresh().join()
            ops.rotateCredential().join()
            ops.testMapping { pico.mapping }

            // then
            ops.status?.state shouldBe "erroring"
            ops.token shouldBe "tok"
            ops.sampleError shouldBe "Required"

            // when
            ops.sample = """{"t":21}"""
            ops.testMapping { pico.mapping }
            scope.settle()

            // then
            ops.test?.errors shouldBe listOf("temp missing")
            backend.sent(HttpMethod.Post, "sources/pico/mapping-test").single().body shouldContain "\"payload\":{\"t\":21}"
        }

        test("bundleExportsAndImports") {
            // given
            val backend = FakeBackend()
            backend.reply("export", buildJsonObject { put("sources", JsonPrimitive(0)) })
            backend.json("import", """{"created":1,"updated":2}""", HttpMethod.Post)
            val scope = stateScope()
            val bundle = BundleState(backend.api, scope)

            // when
            bundle.export().join()

            // then
            bundle.text shouldBe "{\n    \"sources\": 0\n}"

            // when
            bundle.text = "not json"
            bundle.import()

            // then
            bundle.textError.orEmpty() shouldStartWith "Not valid JSON"

            // when
            bundle.text = "{}"
            bundle.import()
            scope.settle()

            // then
            bundle.result?.updated shouldBe 2
            bundle.error.shouldBeNull()
        }
    })
