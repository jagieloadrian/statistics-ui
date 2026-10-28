package com.anjo.statisticsui.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.api.runSuspendCatching
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.ImportResult
import com.anjo.statisticsui.model.MapRow
import com.anjo.statisticsui.model.MappingTestRequest
import com.anjo.statisticsui.model.MappingTestResult
import com.anjo.statisticsui.model.Rule
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.model.SourceStatus
import com.anjo.statisticsui.model.platformJson
import com.anjo.statisticsui.model.rows
import com.anjo.statisticsui.model.withRows
import com.anjo.statisticsui.ui.common.ShownErrors
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.validation.FieldErrors
import com.anjo.statisticsui.validation.jsonError
import com.anjo.statisticsui.validation.positiveWholeNumber
import com.anjo.statisticsui.validation.sourceErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class SourcesState(
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var sources by mutableStateOf(emptyList<DefinitionSummary>())
        private set
    var datasets by mutableStateOf(emptyList<String>())
        private set
    val states = mutableStateMapOf<String, String>()
    var selected by mutableStateOf<Source?>(null)
        private set
    var isNew by mutableStateOf(false)
        private set
    var bundleOpen by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
        private set

    val selectedId get() = selected?.id.takeUnless { isNew }

    fun reload() =
        scope.attempt({ error = it }) {
            datasets = api.list("datasets").map { it.id }
            sources = api.list("sources")
            sources.forEach { source ->
                launch {
                    runSuspendCatching { api.sourceStatus(source.id) }.fold({ states[source.id] = it.state }, { error = it.message })
                }
            }
        }

    fun select(id: String) =
        scope.attempt({ error = it }) {
            selected = api.source(id)
            isNew = false
        }

    fun startNew() {
        selected = Source(id = "", dataset = datasets.firstOrNull().orEmpty())
        isNew = true
    }

    fun onSaved(source: Source) {
        selected = source
        isNew = false
        reload()
    }

    fun onDeleted() {
        selected = null
        reload()
    }

    fun closeBundle() {
        bundleOpen = false
        reload()
    }
}

internal class SourceFormState(
    private val initial: Source,
    val isNew: Boolean,
    private val existingIds: List<String>,
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var source by mutableStateOf(initial)
        private set
    var rows by mutableStateOf(initial.mapping.firstOrNull().rows())
        private set
    var rate by mutableStateOf(initial.rateLimitPerSecond.toString())
    var mappingJson by mutableStateOf(encode(initial.mapping))
    var error by mutableStateOf<String?>(null)
        private set
    val shown = ShownErrors()

    val errors: FieldErrors
        get() = shown.of(sourceErrors(source, rate, mappingJson, if (isNew) existingIds else emptyList()))

    fun edit(change: Source) {
        if (change.mapping != source.mapping) mappingJson = encode(change.mapping)
        source = change
    }

    fun editRows(changed: List<MapRow>) {
        rows = changed
        edit(source.copy(mapping = source.mapping.withRows(changed)))
    }

    fun mapping(): List<Rule> = platformJson.decodeFromString(mappingJson)

    fun applyJson() =
        scope.attempt({ error = it }) {
            val parsed = mapping()
            edit(source.copy(mapping = parsed))
            rows = parsed.firstOrNull().rows()
        }

    fun save(onSaved: (Source) -> Unit) = save(onSaved) { it }

    fun setPaused(
        paused: Boolean,
        onSaved: (Source) -> Unit,
    ) = save(onSaved) { it.copy(paused = paused) }

    fun delete(onDeleted: () -> Unit) =
        scope.attempt({ error = it }) {
            api.delete("sources", initial.id)
            onDeleted()
        }

    private fun save(
        onSaved: (Source) -> Unit,
        change: (Source) -> Source,
    ) {
        shown.submit()
        if (errors.isNotEmpty()) return
        val rateLimit = positiveWholeNumber(rate) ?: return
        scope.attempt({ error = it }) {
            val saved = change(source.copy(mapping = mapping(), rateLimitPerSecond = rateLimit))
            api.putSource(saved)
            onSaved(saved)
        }
    }

    private fun encode(mapping: List<Rule>) = platformJson.encodeToString(mapping)
}

class SourceOpsState(
    private val sourceId: String,
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var status by mutableStateOf<SourceStatus?>(null)
        private set
    var token by mutableStateOf<String?>(null)
        private set
    var sample by mutableStateOf("")
    var test by mutableStateOf<MappingTestResult?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var testSubmitted by mutableStateOf(false)

    val sampleError: String? get() = if (testSubmitted) jsonError(sample) else null

    fun refresh() = scope.attempt({ error = it }) { status = api.sourceStatus(sourceId) }

    fun rotateCredential() = scope.attempt({ error = it }) { token = api.rotateCredential(sourceId) }

    fun testMapping(mapping: () -> List<Rule>) {
        testSubmitted = true
        if (sampleError != null) return
        scope.attempt({ error = it }) {
            test = api.testMapping(sourceId, MappingTestRequest(platformJson.parseToJsonElement(sample), mapping()))
        }
    }
}

class BundleState(
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var text by mutableStateOf("")
    var result by mutableStateOf<ImportResult?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var importSubmitted by mutableStateOf(false)

    val textError: String? get() = if (importSubmitted) jsonError(text) else null

    fun export() =
        scope.attempt({ error = it }) {
            result = null
            text = platformJson.encodeToString(api.exportBundle())
        }

    fun import() {
        importSubmitted = true
        if (textError != null) return
        scope.attempt({ error = it }) { result = api.importBundle(platformJson.parseToJsonElement(text)) }
    }
}

@Composable
fun rememberSourcesState(api: PlatformApi = koinInject()): SourcesState {
    val scope = rememberCoroutineScope()
    return remember { SourcesState(api, scope) }
}

@Composable
internal fun rememberSourceFormState(
    initial: Source,
    isNew: Boolean,
    existingIds: List<String>,
    api: PlatformApi = koinInject(),
): SourceFormState {
    val scope = rememberCoroutineScope()
    return remember(initial, isNew) { SourceFormState(initial, isNew, existingIds, api, scope) }
}

@Composable
fun rememberSourceOpsState(
    sourceId: String,
    api: PlatformApi = koinInject(),
): SourceOpsState {
    val scope = rememberCoroutineScope()
    return remember(sourceId) { SourceOpsState(sourceId, api, scope) }
}

@Composable
fun rememberBundleState(api: PlatformApi = koinInject()): BundleState {
    val scope = rememberCoroutineScope()
    return remember { BundleState(api, scope) }
}
