package com.anjo.statisticsui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

/** `POST /import` answer; `created`/`updated` have no defaults so an [ApiError] body never decodes as one. */
@Serializable
data class ImportResult(val created: Int, val updated: Int, val errors: List<String> = emptyList())

@Serializable
data class ApiError(val error: String, val message: String, val details: List<String> = emptyList())

/** Used for the JSON text areas (advanced mapping, sample payloads, records). */
internal val platformJson = Json { ignoreUnknownKeys = true; prettyPrint = true }
