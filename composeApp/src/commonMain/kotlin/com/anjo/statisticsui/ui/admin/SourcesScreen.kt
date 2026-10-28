package com.anjo.statisticsui.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.model.MapRow
import com.anjo.statisticsui.model.Rule
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.ui.common.CheckboxField
import com.anjo.statisticsui.ui.common.ConfirmButton
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.ListEditor
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.common.SectionTitle
import com.anjo.statisticsui.ui.common.isCompact
import com.anjo.statisticsui.ui.common.screenPadding
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.ui.theme.success
import com.anjo.statisticsui.ui.theme.warning
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Refresh

@Composable
fun SourcesScreen(
    navController: NavHostController,
    state: SourcesState = rememberSourcesState(),
) {
    LaunchedEffect(Unit) { state.reload() }

    Box(Modifier.fillMaxSize()) {
        SourcesContent(state, navController)
        SnackbarHost(state.snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (state.bundleOpen) BundleDialog(onDismiss = state::closeBundle)
}

@Composable
private fun SourcesContent(
    state: SourcesState,
    navController: NavHostController,
) {
    Column(Modifier.fillMaxSize().padding(screenPadding())) {
        ScreenHeader("Sources", navController) {
            OutlinedButton(onClick = { state.bundleOpen = true }) { Text("Export / import") }
        }
        ErrorText(state.error)
        AdminListDetail(
            items = state.listed,
            selectedId = state.selectedId,
            onSelect = state::select,
            onNew = state::startNew,
            newText = "New source",
            backText = "All sources",
            onBack = state::close,
            badge = { id -> state.states[id]?.let { StateChip(it) } },
            detail =
                state.selected?.let { source ->
                    {
                        SourceForm(
                            rememberSourceFormState(source, state.isNew, state.sources.map { it.id }),
                            datasets = state.datasets,
                            onSaved = state::onSaved,
                            onDelete = { state.delete(source.id) },
                        )
                    }
                },
        )
    }
}

@Composable
private fun SourceForm(
    form: SourceFormState,
    datasets: List<String>,
    onSaved: (Source) -> Unit,
    onDelete: () -> Unit,
) {
    val source = form.source
    val errors = form.errors
    val fields: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Field(
                    "ID",
                    source.id,
                    enabled = form.isNew,
                    error = errors["id"],
                    onBlur = { form.shown.leave("id") },
                ) { form.edit(source.copy(id = it)) }
                Field("Label", source.label.orEmpty()) { form.edit(source.copy(label = it.ifBlank { null })) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Picker("Dataset", source.dataset, datasets) { form.edit(source.copy(dataset = it)) }
                Field(
                    "Rate limit per second",
                    form.rate,
                    keyboardType = KeyboardType.Decimal,
                    error = errors["rate"],
                    onBlur = { form.shown.leave("rate") },
                ) { form.rate = it }
            }
            ErrorText(errors["dataset"]?.let { "Dataset: $it" })
            Text(
                "Push address: /api/v2/ingest/${source.id}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            ListEditor("Mapping (first rule)", "Add field", form.rows, form::editRows, { MapRow() }) { row, set -> MappingRow(row, set) }

            SectionTitle("Advanced: full mapping JSON")
            Field(
                "Mapping JSON",
                form.mappingJson,
                Modifier.fillMaxWidth().height(220.dp),
                singleLine = false,
                error = errors["mapping"],
                onBlur = { form.shown.leave("mapping") },
            ) { form.mappingJson = it }
            TextButton(onClick = form::applyJson) { Text("Apply JSON") }

            Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Button(onClick = { form.save(onSaved) }) { Text("Save source") }
                if (!form.isNew) OutlinedButton(onClick = onDelete) { Text("Delete source") }
            }
            ErrorText(form.error)
        }
    }
    val ops: @Composable (Modifier) -> Unit = { modifier ->
        if (!form.isNew) SourceOps(source, mapping = form::mapping, onPause = { paused -> form.setPaused(paused, onSaved) }, modifier)
    }

    // Compact: one scrolling column; wider: form and status side by side, each scrolling on its own
    if (isCompact()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(MEDIUM_PADDING)) {
            fields(Modifier)
            ops(Modifier)
        }
    } else {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING)) {
            fields(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
            ops(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
        }
    }
}

@Composable
private fun RowScope.MappingRow(
    row: MapRow,
    set: (MapRow) -> Unit,
) {
    Field("Target", row.target, Modifier.weight(1f)) { set(row.copy(target = it)) }
    Field("Path", row.path, Modifier.weight(1.5f)) { set(row.copy(path = it)) }
    CheckboxField("Dimension", row.dim) { set(row.copy(dim = it)) }
    if (!row.dim) {
        CheckboxField("Required", row.required) { set(row.copy(required = it)) }
        Field("Min", row.min, Modifier.weight(0.7f)) { set(row.copy(min = it)) }
        Field("Max", row.max, Modifier.weight(0.7f)) { set(row.copy(max = it)) }
    }
}

@Composable
private fun SourceOps(
    source: Source,
    mapping: () -> List<Rule>,
    onPause: (Boolean) -> Unit,
    modifier: Modifier,
    ops: SourceOpsState = rememberSourceOpsState(source.id),
) {
    LaunchedEffect(source) { ops.refresh() }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Text(
                "Status",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text("Paused", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = source.paused, onCheckedChange = onPause)
            IconButton(onClick = ops::refresh) {
                Icon(MaterialSymbols.Rounded.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ops.status?.let { status ->
            StateChip(status.state)
            Text("Last record: ${status.lastRecordAt ?: "never"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Accepted: ${status.accepted.hour} last hour, ${status.accepted.day} last day",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Rejected: ${status.rejected.hour} last hour, ${status.rejected.day} last day",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            status.rejections.forEach { rejection ->
                Text(
                    "${rejection.at}  ${rejection.reason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                rejection.excerpt?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Text("Credential", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        ConfirmButton("Rotate credential", "Click again: the current token stops working", ops::rotateCredential)
        ops.token?.let {
            Text("New token, shown only once — copy it now:", color = MaterialTheme.colorScheme.warning)
            SelectionContainer { Text(it, color = MaterialTheme.colorScheme.onSurface) }
        }

        Text("Test mapping", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Field(
            "Sample payload (JSON)",
            ops.sample,
            Modifier.fillMaxWidth().height(160.dp),
            singleLine = false,
            error = ops.sampleError,
        ) { ops.sample = it }
        Button(onClick = { ops.testMapping(mapping) }) { Text("Test mapping") }
        ops.test?.let { test ->
            Text("${test.records.size} record(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            test.records.forEach {
                SelectionContainer {
                    Text(it.toString(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                }
            }
            test.errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        ErrorText(ops.error)
    }
}

@Composable
private fun BundleDialog(
    onDismiss: () -> Unit,
    bundle: BundleState = rememberBundleState(),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export / import") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Field(
                    "Bundle JSON",
                    bundle.text,
                    Modifier.fillMaxWidth().height(360.dp),
                    singleLine = false,
                    error = bundle.textError,
                ) { bundle.text = it }
                bundle.result?.let { result ->
                    Text(
                        if (result.errors.isEmpty()) {
                            "Imported: ${result.created} created, ${result.updated} updated"
                        } else {
                            "Nothing imported (${result.errors.size} errors):"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    result.errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                ErrorText(bundle.error)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                OutlinedButton(onClick = bundle::export) { Text("Export") }
                Button(onClick = bundle::import) { Text("Import") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun StateChip(state: String) {
    val colors = MaterialTheme.colorScheme
    val color =
        when (state) {
            "receiving" -> colors.success
            "silent" -> colors.warning
            "erroring" -> colors.error
            else -> colors.onSurfaceVariant
        }
    Surface(shape = RoundedCornerShape(SMALL_PADDING), color = colors.surfaceContainerHighest) {
        Text(
            state,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = SMALL_PADDING, vertical = 2.dp),
        )
    }
}
