package com.anjo.statisticsui.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.api.ApiException
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.Dimension
import com.anjo.statisticsui.model.Measure
import com.anjo.statisticsui.ui.common.ConfirmButton
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.ListEditor
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import org.koin.compose.koinInject

private val MEASURE_TYPES = listOf("number", "boolean", "text")

@Composable
fun DatasetsScreen(
    navController: NavHostController,
    api: PlatformApi = koinInject(),
) {
    var datasets by remember { mutableStateOf(emptyList<DefinitionSummary>()) }
    var selected by remember { mutableStateOf<Dataset?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val reload = { scope.attempt({ error = it }) { datasets = api.list("datasets") } }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        ScreenHeader("Datasets", navController)
        ErrorText(error)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING)) {
            AdminList(
                items = datasets,
                selectedId = selected?.id.takeUnless { isNew },
                onSelect = { id ->
                    scope.attempt({ error = it }) {
                        selected = api.dataset(id)
                        isNew = false
                    }
                },
                onNew = {
                    selected = Dataset(id = "")
                    isNew = true
                },
            )
            selected?.let { dataset ->
                DatasetForm(
                    initial = dataset,
                    isNew = isNew,
                    onSave = { saved, force ->
                        require(!isNew || datasets.none { d -> d.id == saved.id }) { "Dataset '${saved.id}' already exists" }
                        api.putDataset(saved, force)
                        selected = saved
                        isNew = false
                        reload()
                    },
                    onDelete = { force ->
                        api.delete("datasets", dataset.id, force)
                        selected = null
                        reload()
                    },
                )
            }
        }
    }
}

@Composable
private fun DatasetForm(
    initial: Dataset,
    isNew: Boolean,
    onSave: suspend (Dataset, Boolean) -> Unit,
    onDelete: suspend (Boolean) -> Unit,
) {
    var dataset by remember(initial) { mutableStateOf(initial) }
    var calculated by remember(initial) { mutableStateOf(initial.calculated.toList()) }
    var retention by remember(initial) { mutableStateOf(initial.retentionDays?.toString().orEmpty()) }
    var error by remember(initial) { mutableStateOf<String?>(null) }
    var retry by remember(initial) { mutableStateOf<Pair<String, suspend () -> Unit>?>(null) }
    val scope = rememberCoroutineScope()

    fun guarded(
        anyway: String,
        action: suspend (force: Boolean) -> Unit,
    ) = scope.attempt({ error = it }) {
        retry = null
        try {
            action(false)
        } catch (e: ApiException) {
            if (e.status == 409) retry = anyway to { action(true) }
            throw e
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Field("Id", dataset.id, enabled = isNew) { dataset = dataset.copy(id = it) }
            Field("Label", dataset.label.orEmpty()) { dataset = dataset.copy(label = it.ifBlank { null }) }
        }

        ListEditor("Measures", dataset.measures, { dataset = dataset.copy(measures = it) }, { Measure("") }) { m, set ->
            Field("Name", m.name, Modifier.weight(1f)) { set(m.copy(name = it)) }
            Picker(m.type, MEASURE_TYPES) { set(m.copy(type = it)) }
            Field("Unit", m.unit.orEmpty(), Modifier.weight(1f)) { set(m.copy(unit = it.ifBlank { null })) }
            Field("Label", m.label.orEmpty(), Modifier.weight(1f)) { set(m.copy(label = it.ifBlank { null })) }
        }

        ListEditor("Dimensions", dataset.dimensions, { dataset = dataset.copy(dimensions = it) }, { Dimension("") }) { d, set ->
            Field("Name", d.name, Modifier.weight(1f)) { set(d.copy(name = it)) }
            Field("Label", d.label.orEmpty(), Modifier.weight(1f)) { set(d.copy(label = it.ifBlank { null })) }
        }

        ListEditor("Calculated measures", calculated, { calculated = it }, { "" to "" }) { (name, expr), set ->
            Field("Name", name, Modifier.weight(1f)) { set(it to expr) }
            Field("Expression, e.g. (recovered + dead) / population", expr, Modifier.weight(3f)) { set(name to it) }
        }

        Field("Retention days (empty = keep forever)", retention, Modifier.padding(top = SMALL_MEDIUM_PADDING)) { retention = it }

        Row(Modifier.padding(top = SMALL_MEDIUM_PADDING), horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Button(onClick = {
                guarded("Save anyway") { force ->
                    val days =
                        if (retention.isBlank()) {
                            null
                        } else {
                            retention.trim().toIntOrNull()
                                ?: throw IllegalArgumentException("Retention days must be a whole number")
                        }
                    onSave(dataset.copy(calculated = calculated.toMap(), retentionDays = days), force)
                }
            }) { Text("Save") }
            if (!isNew) {
                ConfirmButton("Delete", "Click again: deletes the dataset and all its records") {
                    guarded("Delete anyway") { force -> onDelete(force) }
                }
            }
            retry?.let { (label, run) ->
                OutlinedButton(onClick = {
                    scope.attempt({ error = it }) {
                        retry = null
                        run()
                    }
                }) { Text(label) }
            }
        }
        ErrorText(error)
    }
}

@Composable
fun AdminList(
    items: List<DefinitionSummary>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    badge: @Composable (String) -> Unit = {},
) {
    Column(Modifier.width(260.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
        Button(onClick = onNew) {
            Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
            Text("New")
        }
        Spacer(Modifier.padding(top = SMALL_PADDING))
        items.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label?.ifBlank { null } ?: item.id) },
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
                badge = { badge(item.id) },
            )
        }
    }
}
