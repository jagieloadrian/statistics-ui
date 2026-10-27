package com.anjo.statisticsui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

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
data class MappingTestRequest(val payload: JsonElement, val mapping: List<Rule>? = null)

@Serializable
data class MappingTestResult(val records: List<JsonElement> = emptyList(), val errors: List<String> = emptyList())

@Serializable
data class Credential(val token: String)
