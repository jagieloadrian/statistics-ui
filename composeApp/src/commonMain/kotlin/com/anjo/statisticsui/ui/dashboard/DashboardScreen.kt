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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.TimeRange
import com.anjo.statisticsui.model.VariableValue
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.common.catching
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Edit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val RANGES = listOf("1h" to 1.hours, "24h" to 1.days, "7d" to 7.days, "30d" to 30.days)
private val ROW_HEIGHT = 80.dp

@Composable
fun DashboardScreen(id: String, navController: NavHostController, api: PlatformApi = koinInject(), session: Session = koinInject()) {
    var dashboard by remember { mutableStateOf<Dashboard?>(null) }
    var range by remember { mutableStateOf(1.days) }
    var values by remember { mutableStateOf(emptyMap<String, VariableValue>()) }
    var editing by remember { mutableStateOf(false) }
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
    // the dashboard range sets from/to on every widget query; widget definitions never carry their own.
    // Cancelled when the screen leaves composition, which also stops the refresh loop.
    LaunchedEffect(dashboard, range, values, editing) {
        val current = dashboard?.takeUnless { editing } ?: return@LaunchedEffect
        results.clear()
        while (true) {
            val timeRange = TimeRange.last(range)
            coroutineScope {
                current.widgets.forEach { w ->
                    launch { results[w.id] = catching { api.query(resolve(w.query, values, timeRange)) } }
                }
            }
            delay((current.refreshSeconds ?: break).seconds)
        }
    }

    Column(Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        ScreenHeader(dashboard?.title?.ifBlank { null } ?: id, navController) {
            if (!editing) {
                RANGES.forEach { (label, duration) ->
                    FilterChip(
                        selected = range == duration,
                        onClick = { range = duration },
                        label = { Text("Last $label") },
                        modifier = Modifier.padding(end = SMALL_PADDING)
                    )
                }
                // ponytail: owner isn't in the dashboard JSON, so an editor sees Edit on others' dashboards and gets the 403 on save
                if (session.canEdit) {
                    Button(onClick = { editing = true }, enabled = dashboard != null) {
                        Icon(MaterialSymbols.Rounded.Edit, contentDescription = null)
                        Text("Edit")
                    }
                }
            }
        }
        ErrorText(error)
        val current = dashboard ?: return@Column
        if (editing) {
            DashboardEditor(
                initial = current,
                api = api,
                onSave = { saved ->
                    api.putDashboard(saved)
                    // keep chosen values of variables that still exist, start new ones from their defaults
                    values = initialValues(saved, api) + values.filterKeys { name -> saved.variables.any { it.name == name } }
                    dashboard = saved
                    editing = false
                },
                onCancel = { editing = false }
            )
            return@Column
        }
        VariableBar(current, values, api) { name, value -> values = values + (name to value) }
        if (current.widgets.isEmpty()) Text("No widgets yet. Use Edit to add one.", color = Color.Gray)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = SMALL_PADDING),
            verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING)
        ) {
            layoutRows(current.widgets).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING)) {
                    row.forEach { w ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(w.span().toFloat()).height(ROW_HEIGHT * w.h.coerceAtLeast(1))
                        ) {
                            Column(Modifier.fillMaxSize().padding(SMALL_MEDIUM_PADDING)) {
                                Text(resolveText(w.title.ifBlank { w.id }, values), style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = SMALL_PADDING))
                                Widget(w, results[w.id], Modifier.weight(1f))
                            }
                        }
                    }
                    val free = COLUMNS - row.sumOf { it.span() }
                    if (free > 0) Spacer(Modifier.weight(free.toFloat()))
                }
            }
        }
    }
}

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
