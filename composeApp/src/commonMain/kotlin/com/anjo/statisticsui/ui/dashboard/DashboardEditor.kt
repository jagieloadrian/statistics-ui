package com.anjo.statisticsui.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.QueryMeasure
import com.anjo.statisticsui.model.RelativeRange
import com.anjo.statisticsui.model.Threshold
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.model.WidgetOptions
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.ListEditor
import com.anjo.statisticsui.ui.common.NumberField
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.common.catching
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_downward
import com.composables.icons.materialsymbols.rounded.Arrow_upward
import com.composables.icons.materialsymbols.rounded.Delete
import org.koin.compose.koinInject

private val WIDGET_TYPES = listOf("line", "area", "bar", "stat", "gauge", "table")
private val BUCKETS = listOf("auto", "raw", "none", "1m", "5m", "10m", "30m", "1h", "6h", "1d")
private val AGGS = listOf("", "avg", "min", "max", "sum", "count", "first", "last", "at_max", "at_min")
private val VARIABLE_TYPES = listOf("dimension", "timeRange", "bucket")
private val SIZES = (1..12).map { it.toString() }
private val VISIBILITIES = listOf("private", "users", "all")

/** Edits the whole dashboard definition locally; nothing is stored until Save (one PUT). */
@Composable
fun DashboardEditor(initial: Dashboard, onSave: suspend (Dashboard) -> Unit, onCancel: () -> Unit, api: PlatformApi = koinInject()) {
    var dashboard by remember(initial) { mutableStateOf(initial) }
    var open by remember(initial) { mutableStateOf<Int?>(null) } // index of the widget whose form is shown
    var datasetIds by remember { mutableStateOf(emptyList<String>()) }
    val datasets = remember { mutableStateMapOf<String, Dataset>() } // measure/dimension names for the pickers
    var users by remember { mutableStateOf(emptyList<User>()) }
    var shared by remember(initial) { mutableStateOf(emptySet<Long>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { attempt({ error = it }) { datasetIds = api.list("datasets").map { it.id } } }
    LaunchedEffect(initial.id) {
        attempt({ error = it }) {
            users = api.users()
            shared = api.shares(initial.id).toSet()
        }
    }
    val referenced = (dashboard.widgets.map { it.query.dataset } + dashboard.variables.mapNotNull { it.dataset }).filter { it.isNotBlank() }.toSet()
    LaunchedEffect(referenced) {
        referenced.filter { it !in datasets }.forEach { id -> catching { api.dataset(id) }.onSuccess { datasets[id] = it } }
    }
    val widgets = dashboard.widgets
    fun setWidgets(list: List<Widget>) { dashboard = dashboard.copy(widgets = list) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Field("Title", dashboard.title, Modifier.weight(1f)) { dashboard = dashboard.copy(title = it) }
            Field("Default range (ISO-8601, e.g. P1D)", dashboard.defaultRange?.last.orEmpty(), Modifier.weight(1f)) {
                dashboard = dashboard.copy(defaultRange = it.trim().ifBlank { null }?.let(::RelativeRange))
            }
            NumberField("Refresh seconds (empty = off)", dashboard.refreshSeconds?.toDouble(), Modifier.weight(1f)) {
                dashboard = dashboard.copy(refreshSeconds = it?.toInt())
            }
            Text("Visible to", color = Color.Gray)
            Picker(dashboard.visibility, VISIBILITIES) { dashboard = dashboard.copy(visibility = it) }
        }
        if (dashboard.visibility == "users") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Text("Shared with", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                users.forEach { u ->
                    FilterChip(
                        selected = u.id in shared,
                        onClick = { shared = if (u.id in shared) shared - u.id else shared + u.id },
                        label = { Text(u.name.ifBlank { u.email }) }
                    )
                }
            }
        }

        ListEditor("Variables", dashboard.variables, { dashboard = dashboard.copy(variables = it) }, { Variable("", "dimension") }) { v, set ->
            Field("Name", v.name, Modifier.weight(1f)) { set(v.copy(name = it)) }
            Picker(v.type, VARIABLE_TYPES) { set(v.copy(type = it)) }
            when (v.type) {
                "dimension" -> {
                    Picker(v.dataset.orEmpty(), datasetIds) { set(v.copy(dataset = it, dimension = null)) }
                    Picker(v.dimension.orEmpty(), datasets[v.dataset]?.dimensions?.map { it.name }.orEmpty()) { set(v.copy(dimension = it)) }
                    Checkbox(v.multi, { set(v.copy(multi = it)) })
                    Text("multi", color = Color.Gray)
                }
                "timeRange" -> Field("Default (e.g. P7D)", v.default.orEmpty(), Modifier.weight(1f)) { set(v.copy(default = it.trim().ifBlank { null })) }
                "bucket" -> {
                    CsvField("Options, e.g. raw, 1m, 1h", v.options, Modifier.weight(2f)) { set(v.copy(options = it)) }
                    Picker(v.default.orEmpty(), v.options) { set(v.copy(default = it)) }
                }
            }
        }

        Text("Widgets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING))
        widgets.forEachIndexed { i, w ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { open = if (open == i) null else i }, modifier = Modifier.weight(1f)) {
                    Text("${w.title.ifBlank { w.id }} · ${w.type} · ${w.w}×${w.h}", modifier = Modifier.fillMaxWidth())
                }
                IconButton(enabled = i > 0, onClick = { setWidgets(widgets.swap(i, i - 1)); open = i - 1 }) {
                    Icon(MaterialSymbols.Rounded.Arrow_upward, contentDescription = "Move up", tint = Color.Gray)
                }
                IconButton(enabled = i < widgets.lastIndex, onClick = { setWidgets(widgets.swap(i, i + 1)); open = i + 1 }) {
                    Icon(MaterialSymbols.Rounded.Arrow_downward, contentDescription = "Move down", tint = Color.Gray)
                }
                IconButton(onClick = { setWidgets(widgets.filterIndexed { j, _ -> j != i }); open = null }) {
                    Icon(MaterialSymbols.Rounded.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
            if (open == i) {
                WidgetForm(w, dashboard.variables, datasetIds, datasets) { changed ->
                    setWidgets(widgets.mapIndexed { j, old -> if (j == i) changed else old })
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
            }
        }
        TextButton(onClick = {
            val id = generateSequence(1) { it + 1 }.map { "w$it" }.first { id -> widgets.none { it.id == id } }
            setWidgets(widgets + Widget(id = id, type = "line", w = 12, h = 4, query = Query(dataset = datasetIds.firstOrNull().orEmpty())))
            open = widgets.size
        }) {
            Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
            Text("Add widget")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Button(onClick = {
                scope.attempt({ error = it }) {
                    // shares first: onSave closes the editor, which cancels this scope
                    if (dashboard.visibility == "users") api.putShares(dashboard.id, shared.toList())
                    onSave(dashboard)
                }
            }) { Text("Save") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        ErrorText(error)
    }
}

/** Every field of the Widget table in definition-format; the query never gets from/to (the dashboard range does). */
@Composable
private fun WidgetForm(w: Widget, variables: List<Variable>, datasetIds: List<String>, datasets: Map<String, Dataset>,
                       onChange: (Widget) -> Unit) {
    val q = w.query
    val o = w.options
    val ds = datasets[q.dataset]
    val measures = ds?.let { it.measures.map { m -> m.name } + it.calculated.keys }.orEmpty()
    val fields = listOf("ts") + measures
    val dims = ds?.dimensions?.map { it.name }.orEmpty()
    fun setQuery(query: Query) = onChange(w.copy(query = query))
    fun setOptions(options: WidgetOptions) = onChange(w.copy(options = options))

    Column(Modifier.padding(start = MEDIUM_PADDING), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Field("Title (\$variable allowed)", w.title, Modifier.weight(1f)) { onChange(w.copy(title = it)) }
            Text("Type", color = Color.Gray)
            Picker(w.type, WIDGET_TYPES) { onChange(w.copy(type = it)) }
            Text("Width", color = Color.Gray)
            Picker(w.w.toString(), SIZES) { onChange(w.copy(w = it.toInt())) }
            Text("Height", color = Color.Gray)
            Picker(w.h.toString(), SIZES) { onChange(w.copy(h = it.toInt())) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Text("Dataset", color = Color.Gray)
            Picker(q.dataset, datasetIds) { setQuery(q.copy(dataset = it)) }
            Text("Bucket", color = Color.Gray)
            Picker(q.bucket, BUCKETS + variables.filter { it.type == "bucket" }.map { "$" + it.name }) { setQuery(q.copy(bucket = it)) }
            NumberField("Max points (auto bucket)", q.maxPoints.toDouble()) { it?.let { n -> setQuery(q.copy(maxPoints = n.toInt())) } }
        }

        ListEditor("Measures", q.measures, { setQuery(q.copy(measures = it)) }, { QueryMeasure(measures.firstOrNull().orEmpty()) }) { m, set ->
            Picker(m.field, fields) { set(m.copy(field = it)) }
            Text("agg", color = Color.Gray)
            Picker(m.agg.orEmpty(), AGGS) { set(m.copy(agg = it.ifBlank { null })) }
            if (m.agg == "at_max" || m.agg == "at_min") {
                Text("by", color = Color.Gray)
                Picker(m.by.orEmpty(), measures) { set(m.copy(by = it)) }
            }
            Field("as", m.alias.orEmpty(), Modifier.weight(1f)) { set(m.copy(alias = it.trim().ifBlank { null })) }
        }
        // ponytail: map-backed rows; two rows with the same key merge (one filter/colour per name is all a map holds anyway)
        ListEditor("Filter", q.filter.toList(), { setQuery(q.copy(filter = it.toMap())) },
            { dims.firstOrNull { it !in q.filter }.orEmpty() to emptyList() }) { (dim, allowed), set ->
            Picker(dim, dims) { set(it to allowed) }
            CsvField("Values (\$variable allowed)", allowed, Modifier.weight(1f)) { set(dim to it) }
        }
        Text("Group by", color = Color.Gray)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            dims.forEach { dim ->
                val on = dim in q.groupBy
                FilterChip(selected = on, onClick = { setQuery(q.copy(groupBy = if (on) q.groupBy - dim else q.groupBy + dim)) }, label = { Text(dim) })
            }
        }

        Text("Options", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Text("x", color = Color.Gray)
            Picker(o.x ?: "ts", fields) { setOptions(o.copy(x = it.takeUnless { x -> x == "ts" })) }
            Field("Unit", o.unit.orEmpty(), Modifier.weight(1f)) { setOptions(o.copy(unit = it.ifBlank { null })) }
            NumberField("Decimals", o.decimals?.toDouble(), Modifier.weight(1f)) { setOptions(o.copy(decimals = it?.toInt())) }
            NumberField("Min", o.min, Modifier.weight(1f)) { setOptions(o.copy(min = it)) }
            NumberField("Max", o.max, Modifier.weight(1f)) { setOptions(o.copy(max = it)) }
        }
        ListEditor("Colors", o.colors.toList(), { setOptions(o.copy(colors = it.toMap())) }, { "" to "#4DD0E1" }) { (name, color), set ->
            Field("Series or measure", name, Modifier.weight(1f)) { set(it to color) }
            Field("Color #RRGGBB", color, Modifier.weight(1f)) { set(name to it) }
        }
        ListEditor("Thresholds", o.thresholds, { setOptions(o.copy(thresholds = it)) }, { Threshold(0.0, "#EF5350") }) { t, set ->
            NumberField("From value", t.value, Modifier.weight(1f)) { it?.let { v -> set(t.copy(value = v)) } }
            Field("Color #RRGGBB", t.color, Modifier.weight(1f)) { set(t.copy(color = it)) }
        }
    }
}

/** Comma-separated list; keeps the typed text (e.g. a trailing comma) while it still parses to the same list. */
@Composable
private fun CsvField(label: String, values: List<String>, modifier: Modifier = Modifier, onChange: (List<String>) -> Unit) {
    fun parse(text: String) = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    var text by remember { mutableStateOf(values.joinToString(", ")) }
    if (parse(text) != values) text = values.joinToString(", ") // changed from outside
    Field(label, text, modifier) { text = it; onChange(parse(it)) }
}

private fun <T> List<T>.swap(i: Int, j: Int) = toMutableList().also { it[i] = this[j]; it[j] = this[i] }
