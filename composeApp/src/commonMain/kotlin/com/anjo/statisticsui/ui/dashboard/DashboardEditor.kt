package com.anjo.statisticsui.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.QueryMeasure
import com.anjo.statisticsui.model.RelativeRange
import com.anjo.statisticsui.model.Threshold
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.model.WidgetOptions
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.ListEditor
import com.anjo.statisticsui.ui.common.NumberField
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_downward
import com.composables.icons.materialsymbols.rounded.Arrow_upward
import com.composables.icons.materialsymbols.rounded.Delete

private val WIDGET_TYPES = listOf("line", "area", "bar", "stat", "gauge", "table")
private val BUCKETS = listOf("auto", "raw", "none", "1m", "5m", "10m", "30m", "1h", "6h", "1d")
private val AGGS = listOf("", "avg", "min", "max", "sum", "count", "first", "last", "at_max", "at_min")
private val VARIABLE_TYPES = listOf("dimension", "timeRange", "bucket")
private val SIZES = (1..12).map { it.toString() }
private val VISIBILITIES = listOf("private", "users", "all")

@Composable
fun DashboardEditor(
    initial: Dashboard,
    onSave: suspend (Dashboard) -> Unit,
    onCancel: () -> Unit,
    state: DashboardEditorState = rememberDashboardEditorState(initial),
) {
    LaunchedEffect(state) { state.load() }
    val referenced = state.referencedDatasets
    LaunchedEffect(referenced) { state.loadDatasets(referenced) }
    val dashboard = state.dashboard

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        DashboardSettings(state)
        ListEditor("Variables", dashboard.variables, { state.dashboard = dashboard.copy(variables = it) }, { Variable("", "dimension") }) {
            variable,
            set,
            ->
            VariableRow(variable, state.datasetIds, state.datasets, set)
        }
        ErrorText(state.errors["variables"])

        Text(
            "Widgets",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING),
        )
        dashboard.widgets.forEachIndexed { index, widget -> WidgetEntry(state, index, widget) }
        ErrorText(state.errors["widgets"])
        TextButton(onClick = state::addWidget) {
            Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
            Text("Add widget")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Button(onClick = { state.save(onSave) }) { Text("Save") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        ErrorText(state.error)
    }
}

@Composable
private fun DashboardSettings(state: DashboardEditorState) {
    val dashboard = state.dashboard
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        Field("Title", dashboard.title, Modifier.weight(1f)) { state.dashboard = dashboard.copy(title = it) }
        Field("Default range (ISO-8601, e.g. P1D)", dashboard.defaultRange?.last.orEmpty(), Modifier.weight(1f)) {
            state.dashboard = dashboard.copy(defaultRange = it.trim().ifBlank { null }?.let(::RelativeRange))
        }
        NumberField(
            "Refresh seconds (empty = off)",
            dashboard.refreshSeconds?.toDouble(),
            Modifier.weight(1f),
            error = state.errors["refresh"],
        ) {
            state.dashboard = dashboard.copy(refreshSeconds = it?.toInt())
        }
        Text("Visible to", color = Color.Gray)
        Picker(dashboard.visibility, VISIBILITIES) { state.dashboard = dashboard.copy(visibility = it) }
    }
    if (dashboard.visibility == "users") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Text("Shared with", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
            state.users.forEach { user ->
                FilterChip(
                    selected = user.id in state.shared,
                    onClick = { state.toggleShare(user.id) },
                    label = { Text(user.name.ifBlank { user.email }) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.VariableRow(
    variable: Variable,
    datasetIds: List<String>,
    datasets: Map<String, Dataset>,
    set: (Variable) -> Unit,
) {
    Field("Name", variable.name, Modifier.weight(1f)) { set(variable.copy(name = it)) }
    Picker(variable.type, VARIABLE_TYPES) { set(variable.copy(type = it)) }
    when (variable.type) {
        "dimension" -> {
            Picker(variable.dataset.orEmpty(), datasetIds) { set(variable.copy(dataset = it, dimension = null)) }
            Picker(variable.dimension.orEmpty(), datasets[variable.dataset]?.dimensions?.map { it.name }.orEmpty()) {
                set(variable.copy(dimension = it))
            }
            Checkbox(variable.multi, { set(variable.copy(multi = it)) })
            Text("multi", color = Color.Gray)
        }

        "timeRange" -> {
            Field("Default (e.g. P7D)", variable.default.orEmpty(), Modifier.weight(1f)) {
                set(variable.copy(default = it.trim().ifBlank { null }))
            }
        }

        "bucket" -> {
            CsvField("Options, e.g. raw, 1m, 1h", variable.options, Modifier.weight(2f)) { set(variable.copy(options = it)) }
            Picker(variable.default.orEmpty(), variable.options) { set(variable.copy(default = it)) }
        }
    }
}

@Composable
private fun WidgetEntry(
    state: DashboardEditorState,
    index: Int,
    widget: Widget,
) {
    val widgets = state.dashboard.widgets
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { state.toggleWidget(index) }, modifier = Modifier.weight(1f)) {
            Text("${widget.title.ifBlank { widget.id }} · ${widget.type} · ${widget.w}×${widget.h}", modifier = Modifier.fillMaxWidth())
        }
        IconButton(enabled = index > 0, onClick = { state.moveWidget(index, index - 1) }) {
            Icon(MaterialSymbols.Rounded.Arrow_upward, contentDescription = "Move up", tint = Color.Gray)
        }
        IconButton(enabled = index < widgets.lastIndex, onClick = { state.moveWidget(index, index + 1) }) {
            Icon(MaterialSymbols.Rounded.Arrow_downward, contentDescription = "Move down", tint = Color.Gray)
        }
        IconButton(onClick = { state.removeWidget(index) }) {
            Icon(MaterialSymbols.Rounded.Delete, contentDescription = "Delete", tint = Color.Gray)
        }
    }
    if (state.expandedWidget == index) {
        WidgetForm(widget, state.dashboard.variables, state.datasetIds, state.datasets) { state.updateWidget(index, it) }
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
    }
}

@Composable
private fun WidgetForm(
    w: Widget,
    variables: List<Variable>,
    datasetIds: List<String>,
    datasets: Map<String, Dataset>,
    onChange: (Widget) -> Unit,
) {
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

        ListEditor(
            "Measures",
            q.measures,
            { setQuery(q.copy(measures = it)) },
            { QueryMeasure(measures.firstOrNull().orEmpty()) },
        ) { m, set ->
            Picker(m.field, fields) { set(m.copy(field = it)) }
            Text("agg", color = Color.Gray)
            Picker(m.agg.orEmpty(), AGGS) { set(m.copy(agg = it.ifBlank { null })) }
            if (m.agg == "at_max" || m.agg == "at_min") {
                Text("by", color = Color.Gray)
                Picker(m.by.orEmpty(), measures) { set(m.copy(by = it)) }
            }
            Field("as", m.alias.orEmpty(), Modifier.weight(1f)) { set(m.copy(alias = it.trim().ifBlank { null })) }
        }
        ListEditor(
            "Filter",
            q.filter.toList(),
            { setQuery(q.copy(filter = it.toMap())) },
            { dims.firstOrNull { it !in q.filter }.orEmpty() to emptyList() },
        ) { (dim, allowed), set ->
            Picker(dim, dims) { set(it to allowed) }
            CsvField("Values (\$variable allowed)", allowed, Modifier.weight(1f)) { set(dim to it) }
        }
        Text("Group by", color = Color.Gray)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            dims.forEach { dim ->
                val on = dim in q.groupBy
                FilterChip(
                    selected = on,
                    onClick = { setQuery(q.copy(groupBy = if (on) q.groupBy - dim else q.groupBy + dim)) },
                    label = { Text(dim) },
                )
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

@Composable
private fun CsvField(
    label: String,
    values: List<String>,
    modifier: Modifier = Modifier,
    onChange: (List<String>) -> Unit,
) {
    fun parse(text: String) = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    var text by remember { mutableStateOf(values.joinToString(", ")) }
    val changedOutside = parse(text) != values
    if (changedOutside) text = values.joinToString(", ")
    Field(label, text, modifier) {
        text = it
        onChange(parse(it))
    }
}
