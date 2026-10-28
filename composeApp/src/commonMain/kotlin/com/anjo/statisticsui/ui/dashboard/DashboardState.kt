package com.anjo.statisticsui.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.api.runSuspendCatching
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DimensionValue
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.TimeRange
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.VariableValue
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.common.reporting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val SEARCH_DEBOUNCE = 250.milliseconds

class DashboardState(
    private val id: String,
    private val api: PlatformApi,
) {
    var dashboard by mutableStateOf<Dashboard?>(null)
        private set
    var range by mutableStateOf(1.days)
    var values by mutableStateOf(emptyMap<String, VariableValue>())
        private set
    var editing by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
        private set
    val results = mutableStateMapOf<String, Result<QueryResult>>()

    suspend fun load() =
        reporting({ error = it }) {
            val loaded = api.dashboard(id)
            values = initialValues(loaded, api)
            range = initialDuration(loaded)
            dashboard = loaded
        }

    suspend fun runQueries() {
        val current = dashboard?.takeUnless { editing } ?: return
        results.clear()
        while (true) {
            val timeRange = TimeRange.last(range)
            coroutineScope {
                current.widgets.forEach { widget ->
                    launch { results[widget.id] = runSuspendCatching { api.query(resolve(widget.query, values, timeRange)) } }
                }
            }
            delay((current.refreshSeconds ?: break).seconds)
        }
    }

    fun select(
        variable: String,
        value: VariableValue,
    ) {
        values = values + (variable to value)
    }

    suspend fun save(saved: Dashboard) {
        api.putDashboard(saved)
        val stillDeclared = values.filterKeys { name -> saved.variables.any { it.name == name } }
        values = initialValues(saved, api) + stillDeclared
        dashboard = saved
        editing = false
    }
}

class DashboardEditorState(
    initial: Dashboard,
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var dashboard by mutableStateOf(initial)
    var expandedWidget by mutableStateOf<Int?>(null)
        private set
    var datasetIds by mutableStateOf(emptyList<String>())
        private set
    val datasets = mutableStateMapOf<String, Dataset>()
    var users by mutableStateOf(emptyList<User>())
        private set
    var shared by mutableStateOf(emptySet<Long>())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val referencedDatasets: Set<String>
        get() =
            (dashboard.widgets.map { it.query.dataset } + dashboard.variables.mapNotNull { it.dataset })
                .filter { it.isNotBlank() }
                .toSet()

    suspend fun load() =
        reporting({ error = it }) {
            launch { datasetIds = api.list("datasets").map { it.id } }
            users = api.users()
            shared = api.shares(dashboard.id).toSet()
        }

    suspend fun loadDatasets(ids: Set<String>) =
        ids.filter { it !in datasets }.forEach { id ->
            runSuspendCatching { api.dataset(id) }.fold({ datasets[id] = it }, { error = it.message })
        }

    fun toggleShare(userId: Long) {
        shared = if (userId in shared) shared - userId else shared + userId
    }

    fun toggleWidget(index: Int) {
        expandedWidget = if (expandedWidget == index) null else index
    }

    fun moveWidget(
        from: Int,
        to: Int,
    ) {
        setWidgets(dashboard.widgets.swap(from, to))
        expandedWidget = to
    }

    fun removeWidget(index: Int) {
        setWidgets(dashboard.widgets.filterIndexed { i, _ -> i != index })
        expandedWidget = null
    }

    fun updateWidget(
        index: Int,
        widget: Widget,
    ) {
        setWidgets(dashboard.widgets.mapIndexed { i, old -> if (i == index) widget else old })
    }

    fun addWidget() {
        val widgets = dashboard.widgets
        val id = generateSequence(1) { it + 1 }.map { "w$it" }.first { id -> widgets.none { it.id == id } }
        setWidgets(widgets + Widget(id = id, type = "line", w = 12, h = 4, query = Query(dataset = datasetIds.firstOrNull().orEmpty())))
        expandedWidget = widgets.size
    }

    fun save(onSave: suspend (Dashboard) -> Unit) = scope.attempt({ error = it }) { saveSharesThenDashboard(onSave) }

    private suspend fun saveSharesThenDashboard(onSave: suspend (Dashboard) -> Unit) {
        if (dashboard.visibility == "users") api.putShares(dashboard.id, shared.toList())
        onSave(dashboard)
    }

    private fun setWidgets(widgets: List<Widget>) {
        dashboard = dashboard.copy(widgets = widgets)
    }
}

class NewDashboardState(
    private val existingIds: List<String>,
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var id by mutableStateOf("")
    var title by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
        private set

    fun create(onCreated: (String) -> Unit) =
        scope.attempt({ error = it }) {
            val newId = id.trim()
            require(newId !in existingIds) { "Dashboard '$newId' already exists" }
            api.putDashboard(Dashboard(id = newId, title = title.trim()))
            onCreated(newId)
        }
}

class DimensionPickerState(
    private val variable: Variable,
    private val api: PlatformApi,
) {
    var open by mutableStateOf(false)
    var search by mutableStateOf("")
    var options by mutableStateOf(Result.success(emptyList<DimensionValue>()))
        private set

    suspend fun loadOptions() {
        if (!open) return
        delay(SEARCH_DEBOUNCE)
        options = runSuspendCatching { api.dimensionValues(variable.dataset.orEmpty(), variable.dimension.orEmpty(), search) }
    }
}

@Composable
fun rememberDashboardState(
    id: String,
    api: PlatformApi = koinInject(),
): DashboardState = remember(id) { DashboardState(id, api) }

@Composable
fun rememberDashboardEditorState(
    initial: Dashboard,
    api: PlatformApi = koinInject(),
): DashboardEditorState {
    val scope = rememberCoroutineScope()
    return remember(initial) { DashboardEditorState(initial, api, scope) }
}

@Composable
fun rememberNewDashboardState(
    existingIds: List<String>,
    api: PlatformApi = koinInject(),
): NewDashboardState {
    val scope = rememberCoroutineScope()
    return remember { NewDashboardState(existingIds, api, scope) }
}

@Composable
fun rememberDimensionPickerState(
    variable: Variable,
    api: PlatformApi = koinInject(),
): DimensionPickerState = remember(variable) { DimensionPickerState(variable, api) }
