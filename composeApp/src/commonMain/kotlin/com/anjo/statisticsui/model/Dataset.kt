package com.anjo.statisticsui.model

import kotlinx.serialization.Serializable

// Mirror of StatisticsService spec 004 contracts/definition-format.md and api-v2.md

@Serializable
data class Measure(
    val name: String,
    val type: String = "number",
    val unit: String? = null,
    val label: String? = null,
)

@Serializable
data class Dimension(
    val name: String,
    val label: String? = null,
)

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
data class DimensionValue(
    val value: String,
    val firstSeen: String,
    val lastSeen: String,
    val count: Long,
)
