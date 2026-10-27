package com.anjo.statisticsui.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.anjo.statisticsui.api.Dashboard
import com.anjo.statisticsui.api.Dataset
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.api.Query
import com.anjo.statisticsui.api.QueryMeasure
import com.anjo.statisticsui.api.QueryResult
import com.anjo.statisticsui.api.Widget
import com.anjo.statisticsui.presentation.common.ErrorText
import com.anjo.statisticsui.presentation.common.Field
import com.anjo.statisticsui.presentation.common.Picker
import com.anjo.statisticsui.presentation.common.ScreenHeader
import com.anjo.statisticsui.presentation.common.attempt
import com.anjo.statisticsui.presentation.common.catching
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val RANGES = listOf("1h" to 1.hours, "24h" to 1.days, "7d" to 7.days, "30d" to 30.days)
private val BUCKETS = listOf("auto", "raw", "none", "1m", "5m", "10m", "30m", "1h", "6h", "1d")
private val AGGS = listOf("avg", "min", "max", "sum", "count", "first", "last")
private val WIDGET_TYPES = listOf("line", "table")

@Composable
fun DashboardScreen(id: String, navController: NavHostController, api: PlatformApi = koinInject()) {
    var dashboard by remember { mutableStateOf<Dashboard?>(null) }
    var range by remember { mutableStateOf(1.days) }
    var values by remember { mutableStateOf(emptyMap<String, VariableValue>()) }
    var adding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val results = remember { mutableStateMapOf<String, Result<QueryResult>>() }

    LaunchedEffect(id) {
        attempt({ error = it }) {
            val loaded = api.dashboard(id)
            values = initialValues(loaded, api)
            range = initialDuration(loaded)
            dashboard = loaded
        }
    }
    // the dashboard range sets from/to on every widget query; widget definitions never carry their own
    LaunchedEffect(dashboard, range, values) {
        val current = dashboard ?: return@LaunchedEffect
        results.clear()
        val timeRange = TimeRange.last(range)
        current.widgets.forEach { w ->
            launch { results[w.id] = catching { api.query(resolve(w.query, values, timeRange)) } }
        }
    }

    Column(Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        ScreenHeader(dashboard?.title?.ifBlank { null } ?: id, navController) {
            RANGES.forEach { (label, duration) ->
                FilterChip(
                    selected = range == duration,
                    onClick = { range = duration },
                    label = { Text("Last $label") },
                    modifier = Modifier.padding(end = SMALL_PADDING)
                )
            }
            Button(onClick = { adding = true }, enabled = dashboard != null) {
                Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
                Text("Add widget")
            }
        }
        ErrorText(error)
        dashboard?.let { current ->
            VariableBar(current, values, api) { name, value -> values = values + (name to value) }
            if (current.widgets.isEmpty()) Text("No widgets yet. Add one.", color = Color.Gray)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING)) {
                items(current.widgets, key = { it.id }) { w ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(SMALL_MEDIUM_PADDING)) {
                            Text(resolveText(w.title.ifBlank { w.id }, values), style = MaterialTheme.typography.titleMedium, color = Color.White,
                                modifier = Modifier.padding(bottom = SMALL_PADDING))
                            Widget(w, results[w.id])
                        }
                    }
                }
            }
            if (adding) {
                AddWidgetDialog(api, onDismiss = { adding = false }) { widget ->
                    val nextId = generateSequence(1) { it + 1 }.map { "w$it" }.first { wid -> current.widgets.none { it.id == wid } }
                    val updated = current.copy(widgets = current.widgets + widget.copy(id = nextId))
                    api.putDashboard(updated)
                    dashboard = updated
                    adding = false
                }
            }
        }
    }
}

@Composable
private fun AddWidgetDialog(api: PlatformApi, onDismiss: () -> Unit, onSave: suspend (Widget) -> Unit) {
    var datasets by remember { mutableStateOf(emptyList<String>()) }
    var dataset by remember { mutableStateOf<Dataset?>(null) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("line") }
    var bucket by remember { mutableStateOf("auto") }
    var measures by remember { mutableStateOf(emptyList<Pair<String, String>>()) } // field to agg
    var groupBy by remember { mutableStateOf(emptyList<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { attempt({ error = it }) { datasets = api.list("datasets").map { it.id } } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add widget") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Field("Title", title) { title = it }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                    Text("Type")
                    Picker(type, WIDGET_TYPES) { type = it }
                    Text("Bucket")
                    Picker(bucket, BUCKETS) { bucket = it }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                    Text("Dataset")
                    Picker(dataset?.id.orEmpty(), datasets) { id ->
                        scope.attempt({ error = it }) { dataset = api.dataset(id); measures = emptyList(); groupBy = emptyList() }
                    }
                }
                dataset?.let { ds ->
                    Text("Measures")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                        (ds.measures.map { it.name } + ds.calculated.keys).forEach { name ->
                            val on = measures.any { it.first == name }
                            FilterChip(
                                selected = on,
                                onClick = { measures = if (on) measures.filterNot { it.first == name } else measures + (name to defaultAgg(ds, name)) },
                                label = { Text(name) }
                            )
                        }
                    }
                    // bucket raw returns plain values, so no aggregation is sent
                    if (bucket != "raw") {
                        measures.forEach { (name, agg) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, Modifier.weight(1f))
                                Picker(agg, AGGS) { a -> measures = measures.map { if (it.first == name) name to a else it } }
                            }
                        }
                    }
                    Text("Group by")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                        ds.dimensions.forEach { dim ->
                            val on = dim.name in groupBy
                            FilterChip(
                                selected = on,
                                onClick = { groupBy = if (on) groupBy - dim.name else groupBy + dim.name },
                                label = { Text(dim.label ?: dim.name) }
                            )
                        }
                    }
                }
                ErrorText(error)
            }
        },
        confirmButton = {
            Button(enabled = dataset != null && measures.isNotEmpty(), onClick = {
                scope.attempt({ error = it }) {
                    val query = Query(
                        dataset = checkNotNull(dataset).id,
                        measures = measures.map { (field, agg) -> QueryMeasure(field, agg = agg.takeIf { bucket != "raw" }) },
                        groupBy = groupBy,
                        bucket = bucket,
                    )
                    onSave(Widget(id = "", title = title.trim(), type = type, w = 12, h = 4, query = query))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun defaultAgg(dataset: Dataset, field: String) =
    if ((dataset.measures.find { it.name == field }?.type ?: "number") == "number") "avg" else "last"

/** Creates an empty dashboard; refuses an id that already exists, since PUT would overwrite it. */
@Composable
fun NewDashboardDialog(existingIds: List<String>, onDismiss: () -> Unit, onCreated: (String) -> Unit,
                       api: PlatformApi = koinInject()) {
    var id by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New dashboard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Field("Id (letters, digits, - and _)", id) { id = it }
                Field("Title", title) { title = it }
                ErrorText(error)
            }
        },
        confirmButton = {
            Button(enabled = id.isNotBlank(), onClick = {
                scope.attempt({ error = it }) {
                    val newId = id.trim()
                    require(newId !in existingIds) { "Dashboard '$newId' already exists" }
                    api.putDashboard(Dashboard(id = newId, title = title.trim()))
                    onCreated(newId)
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
