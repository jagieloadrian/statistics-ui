package com.anjo.statisticsui.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.api.PlatformApi
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
import com.anjo.statisticsui.ui.common.ConfirmButton
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.ListEditor
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.common.runSuspendCatching
import com.anjo.statisticsui.ui.theme.Colors
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Refresh
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val STATE_COLORS =
    mapOf(
        "receiving" to Color(0xFF4CAF50),
        "waiting-for-data" to Color.Gray,
        "silent" to Colors.LIGHT_ORANGE,
        "paused" to Color(0xFF90A4AE),
        "erroring" to Colors.LIGHT_RED,
    )

@Composable
fun SourcesScreen(
    navController: NavHostController,
    api: PlatformApi = koinInject(),
) {
    var sources by remember { mutableStateOf(emptyList<DefinitionSummary>()) }
    var datasets by remember { mutableStateOf(emptyList<String>()) }
    val states = remember { mutableStateMapOf<String, String>() }
    var selected by remember { mutableStateOf<Source?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var bundleOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val reload = {
        scope.attempt({ error = it }) {
            datasets = api.list("datasets").map { it.id }
            sources = api.list("sources")
            sources.forEach { s -> launch { runSuspendCatching { api.sourceStatus(s.id) }.onSuccess { states[s.id] = it.state } } }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        ScreenHeader("Sources", navController) {
            OutlinedButton(onClick = { bundleOpen = true }) { Text("Export / import") }
        }
        ErrorText(error)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING)) {
            AdminList(
                items = sources,
                selectedId = selected?.id.takeUnless { isNew },
                onSelect = { id ->
                    scope.attempt({ error = it }) {
                        selected = api.source(id)
                        isNew = false
                    }
                },
                onNew = {
                    selected = Source(id = "", dataset = datasets.firstOrNull().orEmpty())
                    isNew = true
                },
                badge = { id -> states[id]?.let { StateChip(it) } },
            )
            selected?.let { source ->
                SourceForm(
                    initial = source,
                    isNew = isNew,
                    existingIds = sources.map { it.id },
                    datasets = datasets,
                    api = api,
                    onSaved = {
                        selected = it
                        isNew = false
                        reload()
                    },
                    onDeleted = {
                        selected = null
                        reload()
                    },
                )
            }
        }
    }
    if (bundleOpen) {
        BundleDialog(api) {
            bundleOpen = false
            reload()
        }
    }
}

@Composable
private fun SourceForm(
    initial: Source,
    isNew: Boolean,
    existingIds: List<String>,
    datasets: List<String>,
    api: PlatformApi,
    onSaved: (Source) -> Unit,
    onDeleted: () -> Unit,
) {
    var source by remember(initial) { mutableStateOf(initial) }
    var rows by remember(initial) { mutableStateOf(initial.mapping.firstOrNull().rows()) }
    var rate by remember(initial) { mutableStateOf(initial.rateLimitPerSecond.toString()) }
    var mappingJson by remember(source.mapping) { mutableStateOf(platformJson.encodeToString(source.mapping)) }
    var error by remember(initial) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun mapping(): List<Rule> = platformJson.decodeFromString(mappingJson)

    fun current() =
        source.copy(
            mapping = mapping(),
            rateLimitPerSecond = rate.trim().toIntOrNull() ?: throw IllegalArgumentException("Rate limit must be a whole number"),
        )
    val save = { s: () -> Source ->
        scope.attempt({ error = it }) {
            val saved = s()
            require(!isNew || saved.id !in existingIds) { "Source '${saved.id}' already exists" }
            api.putSource(saved)
            onSaved(saved)
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Field("Id", source.id, Modifier.weight(1f), enabled = isNew) { source = source.copy(id = it) }
                Field("Label", source.label.orEmpty(), Modifier.weight(1f)) { source = source.copy(label = it.ifBlank { null }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Text("Dataset", color = Color.Gray)
                Picker(source.dataset, datasets) { source = source.copy(dataset = it) }
                Field("Rate limit / s", rate) { rate = it }
            }
            Text("Push address: /api/v2/ingest/${source.id}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

            ListEditor(
                "Mapping (first rule)",
                rows,
                {
                    rows = it
                    source = source.copy(mapping = source.mapping.withRows(it))
                },
                { MapRow() },
            ) { r, set ->
                Field("Target", r.target, Modifier.weight(1f)) { set(r.copy(target = it)) }
                Field("Path", r.path, Modifier.weight(1.5f)) { set(r.copy(path = it)) }
                Checkbox(r.dim, { set(r.copy(dim = it)) })
                Text("dim", color = Color.Gray)
                if (!r.dim) {
                    Checkbox(r.required, { set(r.copy(required = it)) })
                    Text("required", color = Color.Gray)
                    Field("Min", r.min, Modifier.weight(0.7f)) { set(r.copy(min = it)) }
                    Field("Max", r.max, Modifier.weight(0.7f)) { set(r.copy(max = it)) }
                }
            }

            Text(
                "Advanced: full mapping JSON",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING),
            )
            Field("Mapping", mappingJson, Modifier.fillMaxWidth().height(220.dp), singleLine = false) { mappingJson = it }
            TextButton(onClick = {
                scope.attempt({ error = it }) {
                    mapping().let { m ->
                        source = source.copy(mapping = m)
                        rows = m.firstOrNull().rows()
                    }
                }
            }) { Text("Apply JSON") }

            Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Button(onClick = { save { current() } }) { Text("Save") }
                if (!isNew) {
                    ConfirmButton("Delete", "Click again to delete") {
                        scope.attempt({ error = it }) {
                            api.delete("sources", initial.id)
                            onDeleted()
                        }
                    }
                }
            }
            ErrorText(error)
        }
        if (!isNew) {
            SourceOps(
                initial,
                api,
                mapping = ::mapping,
                onPause = { paused -> save { current().copy(paused = paused) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SourceOps(
    source: Source,
    api: PlatformApi,
    mapping: () -> List<Rule>,
    onPause: (Boolean) -> Unit,
    modifier: Modifier,
) {
    var status by remember(source) { mutableStateOf<SourceStatus?>(null) }
    var token by remember(source.id) { mutableStateOf<String?>(null) }
    var sample by remember(source.id) { mutableStateOf("") }
    var test by remember(source.id) { mutableStateOf<MappingTestResult?>(null) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val refresh = { scope.attempt({ error = it }) { status = api.sourceStatus(source.id) } }
    LaunchedEffect(source) { refresh() }

    Column(modifier.fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Text(
                "Status",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text("Paused", color = Color.Gray)
            Switch(checked = source.paused, onCheckedChange = onPause)
            IconButton(onClick = { refresh() }) { Icon(MaterialSymbols.Rounded.Refresh, contentDescription = "Refresh", tint = Color.Gray) }
        }
        status?.let { s ->
            StateChip(s.state)
            Text("Last record: ${s.lastRecordAt ?: "never"}", color = Color.LightGray)
            Text("Accepted: ${s.accepted.hour} last hour, ${s.accepted.day} last day", color = Color.LightGray)
            Text("Rejected: ${s.rejected.hour} last hour, ${s.rejected.day} last day", color = Color.LightGray)
            s.rejections.forEach { r ->
                Text("${r.at}  ${r.reason}", color = Colors.LIGHT_RED, style = MaterialTheme.typography.bodySmall)
                r.excerpt?.let { Text(it, color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
            }
        }

        Text("Credential", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        ConfirmButton("Rotate credential", "Click again: the current token stops working") {
            scope.attempt({ error = it }) { token = api.rotateCredential(source.id) }
        }
        token?.let {
            Text("New token, shown only once — copy it now:", color = Colors.LIGHT_ORANGE)
            SelectionContainer { Text(it, color = MaterialTheme.colorScheme.onSurface) }
        }

        Text("Test mapping", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Field("Sample payload (JSON)", sample, Modifier.fillMaxWidth().height(160.dp), singleLine = false) { sample = it }
        Button(onClick = {
            scope.attempt({ error = it }) {
                test = api.testMapping(source.id, MappingTestRequest(platformJson.parseToJsonElement(sample), mapping()))
            }
        }) { Text("Test mapping") }
        test?.let { t ->
            Text("${t.records.size} record(s)", color = Color.LightGray)
            t.records.forEach {
                SelectionContainer {
                    Text(
                        it.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            t.errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        ErrorText(error)
    }
}

@Composable
private fun BundleDialog(
    api: PlatformApi,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ImportResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export / import") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Field("Bundle JSON", text, Modifier.fillMaxWidth().height(360.dp), singleLine = false) { text = it }
                result?.let { r ->
                    Text(
                        if (r.errors.isEmpty()) {
                            "Imported: ${r.created} created, ${r.updated} updated"
                        } else {
                            "Nothing imported (${r.errors.size} errors):"
                        },
                        color = Color.LightGray,
                    )
                    r.errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                ErrorText(error)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                OutlinedButton(onClick = {
                    scope.attempt({ error = it }) {
                        result = null
                        text = platformJson.encodeToString(api.exportBundle())
                    }
                }) { Text("Export") }
                Button(enabled = text.isNotBlank(), onClick = {
                    scope.attempt({ error = it }) { result = api.importBundle(platformJson.parseToJsonElement(text)) }
                }) { Text("Import") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun StateChip(state: String) {
    val color = STATE_COLORS[state] ?: Color.Gray
    Surface(shape = RoundedCornerShape(SMALL_PADDING), color = color.copy(alpha = 0.2f)) {
        Text(
            state,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = SMALL_PADDING, vertical = 2.dp),
        )
    }
}
