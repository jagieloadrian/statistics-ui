package com.anjo.statisticsui.model

internal data class MapRow(
    val target: String = "",
    val dim: Boolean = false,
    val path: String = "",
    val required: Boolean = true,
    val min: String = "",
    val max: String = "",
    val spec: FieldSpec = FieldSpec(),
)

private fun row(
    target: String,
    spec: FieldSpec,
    dim: Boolean,
) = MapRow(target, dim, spec.path.orEmpty(), spec.required, spec.min?.toString().orEmpty(), spec.max?.toString().orEmpty(), spec)

internal fun Rule?.rows(): List<MapRow> =
    this?.let { r -> r.dims.map { row(it.key, it.value, true) } + r.vals.map { row(it.key, it.value, false) } }.orEmpty()

internal fun List<Rule>.withRows(rows: List<MapRow>): List<Rule> {
    val keepsNativeRecordFormat = isEmpty() && rows.isEmpty()
    if (keepsNativeRecordFormat) return this

    fun MapRow.toSpec() =
        spec.copy(path = path.ifBlank { null }, required = required, min = min.toDoubleOrNull(), max = max.toDoubleOrNull())
    val first =
        (firstOrNull() ?: Rule()).copy(
            dims = rows.filter { it.dim }.associate { it.target to it.toSpec() },
            vals = rows.filterNot { it.dim }.associate { it.target to it.toSpec() },
        )
    return listOf(first) + drop(1)
}
