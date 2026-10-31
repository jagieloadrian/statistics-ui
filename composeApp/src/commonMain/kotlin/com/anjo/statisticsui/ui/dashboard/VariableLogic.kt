package com.anjo.statisticsui.ui.dashboard

import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.api.runSuspendCatching
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.TimeRange
import com.anjo.statisticsui.model.VariableValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

fun initialDuration(dashboard: Dashboard): Duration =
    listOfNotNull(dashboard.variables.firstOrNull { it.type == "timeRange" }?.default, dashboard.defaultRange?.last)
        .firstNotNullOfOrNull { Duration.parseIsoStringOrNull(it) } ?: 1.days

private fun placeholder(value: String) = value.takeIf { it.startsWith("$") }?.substring(1)

fun resolve(
    query: Query,
    values: Map<String, VariableValue>,
    range: TimeRange,
): Query {
    fun valueOf(name: String) = values[name] ?: throw IllegalArgumentException("Unknown variable \$$name")
    val filter =
        query.filter
            .mapValues { (_, allowed) -> allowed.flatMap { v -> placeholder(v)?.let(::valueOf) ?: listOf(v) } }
            .filterValues { it.isNotEmpty() }
    val bucket = placeholder(query.bucket)?.let { valueOf(it).firstOrNull() ?: "auto" } ?: query.bucket
    return query.copy(filter = filter, bucket = bucket, from = range.from.toString(), to = range.to.toString())
}

private val PLACEHOLDER = Regex("\\$([A-Za-z_][A-Za-z0-9_-]*)")

fun resolveText(
    text: String,
    values: Map<String, VariableValue>,
): String = PLACEHOLDER.replace(text) { m -> values[m.groupValues[1]]?.joinToString(", ") ?: m.value }

suspend fun initialValues(
    dashboard: Dashboard,
    api: PlatformApi,
): Map<String, VariableValue> =
    dashboard.variables.filter { it.type != "timeRange" }.associate { v ->
        v.name to (
            v.default?.let(::listOf) ?: when (v.type) {
                "bucket" -> {
                    v.options.take(1)
                }

                "dimension" -> {
                    runSuspendCatching { api.dimensionValues(v.dataset.orEmpty(), v.dimension.orEmpty(), limit = 1) }
                        .getOrDefault(emptyList())
                        .map { it.value }
                }

                else -> {
                    emptyList()
                }
            }
        )
    }
