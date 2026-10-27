package com.anjo.statisticsui.model

/** One simple-editor row = one entry of `dims` (dim = true) or `vals` of the first mapping rule. */
internal data class MapRow(
    val target: String = "",
    val dim: Boolean = false,
    val path: String = "",
    val required: Boolean = true,
    val min: String = "",
    val max: String = "",
    val spec: FieldSpec = FieldSpec(),
)
