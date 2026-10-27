package com.anjo.statisticsui.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.api.Dataset
import com.anjo.statisticsui.api.DefinitionSummary
import com.anjo.statisticsui.api.Dimension
import com.anjo.statisticsui.api.Measure
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.presentation.common.ConfirmButton
import com.anjo.statisticsui.presentation.common.ErrorText
import com.anjo.statisticsui.presentation.common.Field
import com.anjo.statisticsui.presentation.common.Picker
import com.anjo.statisticsui.presentation.common.ScreenHeader
import com.anjo.statisticsui.presentation.common.attempt
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Delete
import org.koin.compose.koinInject

private val MEASURE_TYPES = listOf("number", "boolean", "text")

@Composable
fun DatasetsScreen(navController: NavHostController, api: PlatformApi = koinInject()) {
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
                onSelect = { id -> scope.attempt({ error = it }) { selected = api.dataset(id); isNew = false } },
                onNew = { selected = Dataset(id = ""); isNew = true }
            )
            selected?.let { dataset ->
                DatasetForm(
                    initial = dataset,
                    isNew = isNew,
                    onSave = {
                        // PUT is an upsert: a "new" dataset must not silently replace an existing one
                        require(!isNew || datasets.none { d -> d.id == it.id }) { "Dataset '${it.id}' already exists" }
                        api.putDataset(it); selected = it; isNew = false; reload()
                    },
                    onDelete = { api.delete("datasets", dataset.id); selected = null; reload() }
                )
            }
        }
    }
}

@Composable
private fun DatasetForm(initial: Dataset, isNew: Boolean, onSave: suspend (Dataset) -> Unit, onDelete: suspend () -> Unit) {
    var dataset by remember(initial) { mutableStateOf(initial) }
    // ponytail: calculated kept as pairs while editing so renaming a key never merges two rows
    var calculated by remember(initial) { mutableStateOf(initial.calculated.toList()) }
    var retention by remember(initial) { mutableStateOf(initial.retentionDays?.toString().orEmpty()) }
    var error by remember(initial) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                scope.attempt({ error = it }) {
                    val days = if (retention.isBlank()) null else retention.trim().toIntOrNull()
                        ?: throw IllegalArgumentException("Retention days must be a whole number")
                    onSave(dataset.copy(calculated = calculated.toMap(), retentionDays = days))
                }
            }) { Text("Save") }
            if (!isNew) ConfirmButton("Delete", "Click again: deletes the dataset and all its records") {
                scope.attempt({ error = it }) { onDelete() }
            }
        }
        ErrorText(error)
    }
}

/** Editable rows of [items] with remove buttons and an "Add" button; [row] edits one item. */
@Composable
fun <T> ListEditor(title: String, items: List<T>, onChange: (List<T>) -> Unit, new: () -> T,
                   row: @Composable RowScope.(T, (T) -> Unit) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White,
        modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING))
    items.forEachIndexed { i, item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            row(item) { changed -> onChange(items.mapIndexed { j, old -> if (j == i) changed else old }) }
            IconButton(onClick = { onChange(items.filterIndexed { j, _ -> j != i }) }) {
                Icon(MaterialSymbols.Rounded.Delete, contentDescription = "Remove", tint = Color.Gray)
            }
        }
    }
    TextButton(onClick = { onChange(items + new()) }) {
        Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
        Text("Add")
    }
}

/** Left-hand definition list with a "New" button; [badge] renders extra info per id (e.g. source state). */
@Composable
fun AdminList(items: List<DefinitionSummary>, selectedId: String?, onSelect: (String) -> Unit, onNew: () -> Unit,
              badge: @Composable (String) -> Unit = {}) {
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
                badge = { badge(item.id) }
            )
        }
    }
}
