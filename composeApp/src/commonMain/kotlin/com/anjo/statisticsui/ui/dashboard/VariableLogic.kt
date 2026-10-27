package com.anjo.statisticsui.ui.dashboard

import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.TimeRange
import com.anjo.statisticsui.model.VariableValue
import com.anjo.statisticsui.ui.common.catching
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** Starting dashboard range: the `timeRange` variable's default, else `defaultRange`, else 24 h. */
fun initialDuration(dashboard: Dashboard): Duration =
    listOfNotNull(dashboard.variables.firstOrNull { it.type == "timeRange" }?.default, dashboard.defaultRange?.last)
        .firstNotNullOfOrNull { runCatching { Duration.parseIsoString(it) }.getOrNull() } ?: 1.days

private fun placeholder(value: String) = value.takeIf { it.startsWith("$") }?.substring(1)

/**
 * Replaces `$name` placeholders in `filter` values and `bucket` and sets `from`/`to` from [range].
 * An unknown variable throws; a variable without a value drops its filter (= all values) or leaves the bucket `auto`.
 */
fun resolve(query: Query, values: Map<String, VariableValue>, range: TimeRange): Query {
    fun valueOf(name: String) = values[name] ?: throw IllegalArgumentException("Unknown variable \$$name")
    val filter = query.filter
        .mapValues { (_, allowed) -> allowed.flatMap { v -> placeholder(v)?.let(::valueOf) ?: listOf(v) } }
        .filterValues { it.isNotEmpty() }
    val bucket = placeholder(query.bucket)?.let { valueOf(it).firstOrNull() ?: "auto" } ?: query.bucket
    return query.copy(filter = filter, bucket = bucket, from = range.from.toString(), to = range.to.toString())
}

private val PLACEHOLDER = Regex("\\$([A-Za-z_][A-Za-z0-9_-]*)")

/** `Run $run` → `Run 42` for widget titles; unknown names stay as written. */
fun resolveText(text: String, values: Map<String, VariableValue>): String =
    PLACEHOLDER.replace(text) { m -> values[m.groupValues[1]]?.joinToString(", ") ?: m.value }

/** Starting values: a variable's `default`, else a bucket's first option or a dimension's most recently seen value. */
suspend fun initialValues(dashboard: Dashboard, api: PlatformApi): Map<String, VariableValue> =
    dashboard.variables.filter { it.type != "timeRange" }.associate { v ->
        v.name to (v.default?.let(::listOf) ?: when (v.type) {
            "bucket" -> v.options.take(1)
            "dimension" -> catching { api.dimensionValues(v.dataset.orEmpty(), v.dimension.orEmpty(), limit = 1) }
                .getOrDefault(emptyList()).map { it.value }
            else -> emptyList()
        })
    }
