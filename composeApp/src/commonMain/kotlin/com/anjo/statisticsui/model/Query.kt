package com.anjo.statisticsui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QueryMeasure(
    val field: String,
    val agg: String? = null,
    val by: String? = null,
    @SerialName("as") val alias: String? = null,
)

@Serializable
data class RelativeRange(
    val last: String,
)

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
data class Series(
    val dims: Map<String, String> = emptyMap(),
    val columns: List<String>,
    val rows: List<List<JsonElement>>,
)

@Serializable
data class QueryResult(
    val bucket: String = "",
    val series: List<Series> = emptyList(),
    val truncated: Boolean = false,
)
