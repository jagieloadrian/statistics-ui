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
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.VariableValue
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.catching
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
