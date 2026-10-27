package com.anjo.statisticsui.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

// Mirror of StatisticsService spec 004 contracts/definition-format.md and api-v2.md

/** Used for the JSON text areas (advanced mapping, sample payloads, records). */
val platformJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

@Serializable
data class Measure(val name: String, val type: String = "number", val unit: String? = null, val label: String? = null)

@Serializable
data class Dimension(val name: String, val label: String? = null)

@Serializable
data class Dataset(
    val version: Int = 1,
    val kind: String = "dataset",
    val id: String,
    val label: String? = null,
    val measures: List<Measure> = emptyList(),
    val dimensions: List<Dimension> = emptyList(),
    val calculated: Map<String, String> = emptyMap(),
    val retentionDays: Int? = null,
)

@Serializable
data class Channel(val type: String = "push")

@Serializable
data class TimestampSpec(val path: String, val format: String = "iso", val maxAgeHours: Int? = null)

@Serializable
data class FieldSpec(
    val path: String? = null,
    @SerialName("const") val constValue: JsonPrimitive? = null,
    val required: Boolean = true,
    val notBlank: Boolean = true,
    val min: Double? = null,
    val max: Double? = null,
    val scale: Double? = null,
)

@Serializable
data class Rule(
    val each: String? = null,
    val timestamp: TimestampSpec? = null,
    val identity: List<String> = emptyList(),
    val dims: Map<String, FieldSpec> = emptyMap(),
    val vals: Map<String, FieldSpec> = emptyMap(),
    @SerialName("const") val constDims: Map<String, String> = emptyMap(),
)

@Serializable
data class Source(
    val version: Int = 1,
    val kind: String = "source",
    val id: String,
    val label: String? = null,
    val channel: Channel = Channel(),
    val paused: Boolean = false,
    val rateLimitPerSecond: Int = 50,
    val silentAfterMinutes: Int = 10,
    val dataset: String,
    val mapping: List<Rule> = emptyList(),
)

@Serializable
data class QueryMeasure(val field: String, val agg: String? = null, val by: String? = null, @SerialName("as") val alias: String? = null)

@Serializable
data class RelativeRange(val last: String)

@Serializable
data class Query(
    val dataset: String,
    val measures: List<QueryMeasure> = emptyList(),
    val filter: Map<String, List<String>> = emptyMap(),
    val groupBy: List<String> = emptyList(),
    val from: String? = null,
    val to: String? = null,
    val bucket: String = "auto",
    val maxPoints: Int = 1000,
)

@Serializable
data class Threshold(val value: Double, val color: String)

@Serializable
data class WidgetOptions(
    val x: String? = null,
    val unit: String? = null,
    val colors: Map<String, String> = emptyMap(),
    val min: Double? = null,
    val max: Double? = null,
    val thresholds: List<Threshold> = emptyList(),
    val decimals: Int? = null,
)

@Serializable
data class Widget(
    val id: String,
    val title: String = "",
    val type: String,
    val w: Int = 6,
    val h: Int = 3,
    val query: Query,
    val options: WidgetOptions = WidgetOptions(),
)

@Serializable
data class Variable(
    val name: String,
    val type: String,
    val dataset: String? = null,
    val dimension: String? = null,
    val multi: Boolean = false,
    val default: String? = null,
    val options: List<String> = emptyList(),
)

@Serializable
data class Dashboard(
    val version: Int = 1,
    val kind: String = "dashboard",
    val id: String,
    val title: String = "",
    val visibility: String = "private",
    val refreshSeconds: Int? = null,
    val defaultRange: RelativeRange? = null,
    val variables: List<Variable> = emptyList(),
    val widgets: List<Widget> = emptyList(),
)

@Serializable
data class Bundle(
    val version: Int = 1,
    val datasets: List<Dataset> = emptyList(),
    val sources: List<Source> = emptyList(),
    val dashboards: List<Dashboard> = emptyList(),
)

/** `GET /{kind}` row; `label` carries the dashboard title. */
@Serializable
data class DefinitionSummary(val id: String, val label: String? = null, val updatedAt: String? = null)

@Serializable
data class Series(val dims: Map<String, String> = emptyMap(), val columns: List<String>, val rows: List<List<JsonElement>>)

@Serializable
data class QueryResult(val bucket: String = "", val series: List<Series> = emptyList(), val truncated: Boolean = false)

@Serializable
data class Counts(val hour: Long = 0, val day: Long = 0)

@Serializable
data class Rejection(val at: String, val reason: String, val excerpt: String? = null)

@Serializable
data class SourceStatus(
    val state: String,
    val lastRecordAt: String? = null,
    val accepted: Counts = Counts(),
    val rejected: Counts = Counts(),
    val rejections: List<Rejection> = emptyList(),
)

@Serializable
data class DimensionValue(val value: String, val firstSeen: String, val lastSeen: String, val count: Long)

@Serializable
data class MappingTestRequest(val payload: JsonElement, val mapping: List<Rule>? = null)

@Serializable
data class MappingTestResult(val records: List<JsonElement> = emptyList(), val errors: List<String> = emptyList())

@Serializable
data class Credential(val token: String)

@Serializable
data class ApiError(val error: String, val message: String, val details: List<String> = emptyList())
