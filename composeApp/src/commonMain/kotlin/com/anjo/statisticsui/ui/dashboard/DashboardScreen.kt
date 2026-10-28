package com.anjo.statisticsui.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.VariableValue
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Edit
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val RANGES = listOf("1h" to 1.hours, "24h" to 1.days, "7d" to 7.days, "30d" to 30.days)
private val ROW_HEIGHT = 80.dp

@Composable
fun DashboardScreen(
    id: String,
    navController: NavHostController,
    session: Session = koinInject(),
    state: DashboardState = rememberDashboardState(id),
) {
    LaunchedEffect(state) { state.load() }
    LaunchedEffect(state.dashboard, state.range, state.values, state.editing) { state.runQueries() }
    val dashboard = state.dashboard

    Column(Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        ScreenHeader(dashboard?.title?.ifBlank { null } ?: id, navController) {
            if (!state.editing) {
                RANGES.forEach { (label, duration) ->
                    FilterChip(
                        selected = state.range == duration,
                        onClick = { state.range = duration },
                        label = { Text("Last $label") },
                        modifier = Modifier.padding(end = SMALL_PADDING),
                    )
                }
                if (session.canEdit) {
                    Button(onClick = { state.editing = true }, enabled = dashboard != null) {
                        Icon(MaterialSymbols.Rounded.Edit, contentDescription = null)
                        Text("Edit")
                    }
                }
            }
        }
        ErrorText(state.error)
        when {
            dashboard == null -> {}

            state.editing -> {
                DashboardEditor(initial = dashboard, onSave = state::save, onCancel = { state.editing = false })
            }

            else -> {
                DashboardView(dashboard, state.values, state.results, onSelect = state::select)
            }
        }
    }
}

@Composable
private fun DashboardView(
    dashboard: Dashboard,
    values: Map<String, VariableValue>,
    results: Map<String, Result<QueryResult>>,
    onSelect: (String, VariableValue) -> Unit,
) {
    VariableBar(dashboard, values, onSelect)
    if (dashboard.widgets.isEmpty()) Text("No widgets yet. Use Edit to add one.", color = Color.Gray)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = SMALL_PADDING),
        verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
    ) {
        layoutRows(dashboard.widgets).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING)) {
                row.forEach { widget -> WidgetCard(widget, values, results[widget.id], Modifier.weight(widget.span().toFloat())) }
                val free = COLUMNS - row.sumOf { it.span() }
                if (free > 0) Spacer(Modifier.weight(free.toFloat()))
            }
        }
    }
}

@Composable
private fun WidgetCard(
    widget: Widget,
    values: Map<String, VariableValue>,
    result: Result<QueryResult>?,
    modifier: Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(ROW_HEIGHT * widget.h.coerceAtLeast(1)),
    ) {
        Column(Modifier.fillMaxSize().padding(SMALL_MEDIUM_PADDING)) {
            Text(
                resolveText(widget.title.ifBlank { widget.id }, values),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = SMALL_PADDING),
            )
            Widget(widget, result, Modifier.weight(1f))
        }
    }
}

@Composable
fun NewDashboardDialog(
    existingIds: List<String>,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
    state: NewDashboardState = rememberNewDashboardState(existingIds),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New dashboard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Field("Id (letters, digits, - and _)", state.id, error = state.errors["id"]) { state.id = it }
                Field("Title", state.title) { state.title = it }
                ErrorText(state.error)
            }
        },
        confirmButton = {
            Button(onClick = { state.create(onCreated) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
