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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.DimensionValue
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.VariableValue
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.theme.SMALL_PADDING

@Composable
fun VariableBar(
    dashboard: Dashboard,
    values: Map<String, VariableValue>,
    onChange: (String, VariableValue) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        dashboard.variables.forEach { variable ->
            val selected = values[variable.name].orEmpty()
            when (variable.type) {
                "dimension" -> {
                    DimensionPicker(variable, selected) { onChange(variable.name, it) }
                }

                "bucket" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${variable.name}: ", color = Color.Gray)
                        Picker(selected.firstOrNull().orEmpty(), variable.options) { onChange(variable.name, listOf(it)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionPicker(
    variable: Variable,
    selected: VariableValue,
    state: DimensionPickerState = rememberDimensionPickerState(variable),
    onChange: (VariableValue) -> Unit,
) {
    LaunchedEffect(state.open, state.search) { state.loadOptions() }
    Box {
        OutlinedButton(onClick = { state.open = true }) { Text("${variable.name}: ${selected.joinToString().ifBlank { "all" }}") }
        DropdownMenu(expanded = state.open, onDismissRequest = { state.open = false }) {
            Field("Search", state.search, Modifier.padding(horizontal = SMALL_PADDING)) { state.search = it }
            ErrorText(state.options.exceptionOrNull()?.message)
            state.options.getOrDefault(emptyList()).forEach { option ->
                DimensionOption(option, variable.multi, checked = option.value in selected) {
                    if (variable.multi) {
                        onChange(if (option.value in selected) selected - option.value else selected + option.value)
                    } else {
                        onChange(listOf(option.value))
                        state.open = false
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionOption(
    option: DimensionValue,
    multi: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val zone = displayZone()
    DropdownMenuItem(
        text = {
            Column {
                Text(option.value)
                Text(
                    "${option.firstSeen.shortTime(zone)} – ${option.lastSeen.shortTime(zone)} · ${option.count} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        },
        leadingIcon = if (multi) ({ Checkbox(checked = checked, onCheckedChange = null) }) else null,
        onClick = onClick,
    )
}
