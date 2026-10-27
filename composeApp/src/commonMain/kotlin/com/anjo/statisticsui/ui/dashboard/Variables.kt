package com.anjo.statisticsui.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.DimensionValue
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.TimeRange
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.VariableValue
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.catching
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** Starting dashboard range: the `timeRange` variable's default, else `defaultRange`, else 24 h. */
fun initialDuration(dashboard: Dashboard): Duration =
    listOfNotNull(dashboard.variables.firstOrNull { it.type == "timeRange" }?.default, dashboard.defaultRange?.last)
        .firstNotNullOfOrNull { runCatching { Duration.parseIsoString(it) }.getOrNull() } ?: 1.days

private fun placeholder(value: String) = value.takeIf { it.startsWith("$") }?.substring(1)

/**
 * Replaces `$name` placeholders in `filter` values and `bucket` and sets `from`/`to` from [range].
 * An unknown variable throws; a variable without a value drops its filter (= all values) or leaves the bucket `auto`.
 */
fun resolve(query: Query, values: Map<String, VariableValue>, range: TimeRange): Query {
    fun valueOf(name: String) = values[name] ?: throw IllegalArgumentException("Unknown variable \$$name")
    val filter = query.filter
        .mapValues { (_, allowed) -> allowed.flatMap { v -> placeholder(v)?.let(::valueOf) ?: listOf(v) } }
        .filterValues { it.isNotEmpty() }
    val bucket = placeholder(query.bucket)?.let { valueOf(it).firstOrNull() ?: "auto" } ?: query.bucket
    return query.copy(filter = filter, bucket = bucket, from = range.from.toString(), to = range.to.toString())
}

private val PLACEHOLDER = Regex("\\$([A-Za-z_][A-Za-z0-9_-]*)")

/** `Run $run` → `Run 42` for widget titles; unknown names stay as written. */
fun resolveText(text: String, values: Map<String, VariableValue>): String =
    PLACEHOLDER.replace(text) { m -> values[m.groupValues[1]]?.joinToString(", ") ?: m.value }

/** Starting values: a variable's `default`, else a bucket's first option or a dimension's most recently seen value. */
suspend fun initialValues(dashboard: Dashboard, api: PlatformApi): Map<String, VariableValue> =
    dashboard.variables.filter { it.type != "timeRange" }.associate { v ->
        v.name to (v.default?.let(::listOf) ?: when (v.type) {
            "bucket" -> v.options.take(1)
            "dimension" -> catching { api.dimensionValues(v.dataset.orEmpty(), v.dimension.orEmpty(), limit = 1) }
                .getOrDefault(emptyList()).map { it.value }
            else -> emptyList()
        })
    }

/** Pickers for `dimension` and `bucket` variables; a `timeRange` variable is the dashboard range selector itself. */
@Composable
fun VariableBar(dashboard: Dashboard, values: Map<String, VariableValue>, api: PlatformApi, onChange: (String, VariableValue) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        dashboard.variables.forEach { v ->
            val selected = values[v.name].orEmpty()
            when (v.type) {
                "dimension" -> DimensionPicker(v, selected, api) { onChange(v.name, it) }
                "bucket" -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${v.name}: ", color = Color.Gray)
                    Picker(selected.firstOrNull().orEmpty(), v.options) { onChange(v.name, listOf(it)) }
                }
            }
        }
    }
}

@Composable
private fun DimensionPicker(variable: Variable, selected: VariableValue, api: PlatformApi, onChange: (VariableValue) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(emptyList<DimensionValue>()) }
    val zone = displayZone()
    LaunchedEffect(open, search) {
        if (!open) return@LaunchedEffect
        delay(250.milliseconds) // debounce typing
        options = catching { api.dimensionValues(variable.dataset.orEmpty(), variable.dimension.orEmpty(), search) }
            .getOrDefault(emptyList())
    }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("${variable.name}: ${selected.joinToString().ifBlank { "all" }}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Field("Search", search, Modifier.padding(horizontal = SMALL_PADDING)) { search = it }
            options.forEach { dv ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(dv.value)
                            Text("${dv.firstSeen.shortTime(zone)} – ${dv.lastSeen.shortTime(zone)} · ${dv.count} records",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    },
                    leadingIcon = if (variable.multi) {
                        { Checkbox(checked = dv.value in selected, onCheckedChange = null) }
                    } else null,
                    onClick = {
                        if (variable.multi) {
                            onChange(if (dv.value in selected) selected - dv.value else selected + dv.value)
                        } else {
                            onChange(listOf(dv.value))
                            open = false
                        }
                    }
                )
            }
        }
    }
}
