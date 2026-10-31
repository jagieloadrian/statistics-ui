package com.anjo.statisticsui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Kind(
    val path: String,
) {
    DATASETS("datasets"),
    SOURCES("sources"),
    DASHBOARDS("dashboards"),
}

@Serializable
data class DefinitionSummary(
    val id: String,
    val label: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ImportResult(
    val created: Int,
    val updated: Int,
    val errors: List<String> = emptyList(),
)

@Serializable
data class ApiError(
    val error: String,
    val message: String,
    val details: List<String> = emptyList(),
)

internal val platformJson =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
