package com.anjo.statisticsui.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.ApiException
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.ui.common.attempt
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject

class DatasetsState(
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var datasets by mutableStateOf(emptyList<DefinitionSummary>())
        private set
    var selected by mutableStateOf<Dataset?>(null)
        private set
    var isNew by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val selectedId get() = selected?.id.takeUnless { isNew }

    fun reload() = scope.attempt({ error = it }) { datasets = api.list("datasets") }

    fun select(id: String) =
        scope.attempt({ error = it }) {
            selected = api.dataset(id)
            isNew = false
        }

    fun startNew() {
        selected = Dataset(id = "")
        isNew = true
    }

    suspend fun save(
        dataset: Dataset,
        force: Boolean,
    ) {
        require(!isNew || datasets.none { it.id == dataset.id }) { "Dataset '${dataset.id}' already exists" }
        api.putDataset(dataset, force)
        selected = dataset
        isNew = false
        reload()
    }

    suspend fun delete(
        id: String,
        force: Boolean,
    ) {
        api.delete("datasets", id, force)
        selected = null
        reload()
    }
}

class DatasetFormState(
    initial: Dataset,
    private val scope: CoroutineScope,
) {
    var dataset by mutableStateOf(initial)
    var calculated by mutableStateOf(initial.calculated.toList())
    var retention by mutableStateOf(initial.retentionDays?.toString().orEmpty())
    var error by mutableStateOf<String?>(null)
        private set
    var retry by mutableStateOf<Pair<String, suspend () -> Unit>?>(null)
        private set

    fun save(onSave: suspend (Dataset, Boolean) -> Unit) =
        retryOnConflict("Save anyway") { force ->
            val days =
                if (retention.isBlank()) {
                    null
                } else {
                    retention.trim().toIntOrNull() ?: throw IllegalArgumentException("Retention days must be a whole number")
                }
            onSave(dataset.copy(calculated = calculated.toMap(), retentionDays = days), force)
        }

    fun delete(onDelete: suspend (Boolean) -> Unit) = retryOnConflict("Delete anyway", onDelete)

    fun runRetry(run: suspend () -> Unit) =
        scope.attempt({ error = it }) {
            retry = null
            run()
        }

    private fun retryOnConflict(
        label: String,
        action: suspend (force: Boolean) -> Unit,
    ) = scope.attempt({ error = it }) {
        retry = null
        try {
            action(false)
        } catch (e: ApiException) {
            if (e.status == HttpStatusCode.Conflict.value) retry = label to { action(true) }
            throw e
        }
    }
}

@Composable
fun rememberDatasetsState(api: PlatformApi = koinInject()): DatasetsState {
    val scope = rememberCoroutineScope()
    return remember { DatasetsState(api, scope) }
}

@Composable
fun rememberDatasetFormState(initial: Dataset): DatasetFormState {
    val scope = rememberCoroutineScope()
    return remember(initial) { DatasetFormState(initial, scope) }
}
