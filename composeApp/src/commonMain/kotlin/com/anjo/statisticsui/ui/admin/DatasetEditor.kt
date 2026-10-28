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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
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
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add

private val MEASURE_TYPES = listOf("number", "boolean", "text")

@Composable
fun DatasetsScreen(
    navController: NavHostController,
    state: DatasetsState = rememberDatasetsState(),
) {
    LaunchedEffect(Unit) { state.reload() }

    Column(Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        ScreenHeader("Datasets", navController)
        ErrorText(state.error)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING)) {
            AdminList(
                items = state.datasets,
                selectedId = state.selectedId,
                onSelect = state::select,
                onNew = state::startNew,
            )
            state.selected?.let { dataset ->
                DatasetForm(
                    rememberDatasetFormState(dataset, if (state.isNew) state.datasets.map { it.id } else emptyList()),
                    isNew = state.isNew,
                    onSave = state::save,
                    onDelete = { force -> state.delete(dataset.id, force) },
                )
            }
        }
    }
}

@Composable
private fun DatasetForm(
    form: DatasetFormState,
    isNew: Boolean,
    onSave: suspend (Dataset, Boolean) -> Unit,
    onDelete: suspend (Boolean) -> Unit,
) {
    val dataset = form.dataset
    val errors = form.errors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Field("Id", dataset.id, enabled = isNew, error = errors["id"]) { form.dataset = dataset.copy(id = it) }
            Field("Label", dataset.label.orEmpty()) { form.dataset = dataset.copy(label = it.ifBlank { null }) }
        }

        ListEditor("Measures", dataset.measures, { form.dataset = dataset.copy(measures = it) }, { Measure("") }) { measure, set ->
            Field("Name", measure.name, Modifier.weight(1f)) { set(measure.copy(name = it)) }
            Picker(measure.type, MEASURE_TYPES) { set(measure.copy(type = it)) }
            Field("Unit", measure.unit.orEmpty(), Modifier.weight(1f)) { set(measure.copy(unit = it.ifBlank { null })) }
            Field("Label", measure.label.orEmpty(), Modifier.weight(1f)) { set(measure.copy(label = it.ifBlank { null })) }
        }

        ListEditor(
            "Dimensions",
            dataset.dimensions,
            { form.dataset = dataset.copy(dimensions = it) },
            { Dimension("") },
        ) { dimension, set ->
            Field("Name", dimension.name, Modifier.weight(1f)) { set(dimension.copy(name = it)) }
            Field("Label", dimension.label.orEmpty(), Modifier.weight(1f)) { set(dimension.copy(label = it.ifBlank { null })) }
        }

        ListEditor("Calculated measures", form.calculated, { form.calculated = it }, { "" to "" }) { (name, expression), set ->
            Field("Name", name, Modifier.weight(1f)) { set(it to expression) }
            Field("Expression, e.g. (recovered + dead) / population", expression, Modifier.weight(3f)) { set(name to it) }
        }

        ErrorText(errors["names"])
        Field(
            "Retention days (empty = keep forever)",
            form.retention,
            Modifier.padding(top = SMALL_MEDIUM_PADDING),
            error = errors["retention"],
        ) {
            form.retention = it
        }

        Row(Modifier.padding(top = SMALL_MEDIUM_PADDING), horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Button(onClick = { form.save(onSave) }) { Text("Save") }
            if (!isNew) ConfirmButton("Delete", "Click again: deletes the dataset and all its records") { form.delete(onDelete) }
            form.retry?.let { (label, run) -> OutlinedButton(onClick = { form.runRetry(run) }) { Text(label) } }
        }
        ErrorText(form.error)
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
