package com.anjo.statisticsui.ui.dashboard

import androidx.compose.ui.graphics.Color
import com.anjo.statisticsui.model.ChartLine
import com.anjo.statisticsui.model.LineChartData
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.Threshold
import com.anjo.statisticsui.model.UnitDisplay
import com.anjo.statisticsui.model.WidgetOptions
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Instant

internal fun lineChartData(
    result: QueryResult,
    xColumn: String = "ts",
    zone: TimeZone = TimeZone.UTC,
    display: UnitDisplay = UnitDisplay(null),
): LineChartData? {
    val raw =
        result.series.flatMap { s ->
            val x = s.columns.indexOf(xColumn)
            if (x < 0) {
                emptyList()
            } else {
                s.columns.indices.filter { it != x && s.columns[it] != "ts" }.map { c ->
                    ChartLine((s.dims.values + s.columns[c]).joinToString(" · "), s.columns[c], emptyList()) to
                        s.rows.mapNotNull { r -> r[c].number()?.let { r[x].text() to display.convert(it).toFloat() } }
                }
            }
        }
    val xs =
        raw
            .flatMap { (_, points) -> points.map { it.first } }
            .distinct()
            .sortedWith(compareBy<String> { it.toDoubleOrNull() }.thenBy { it })
    val values = raw.flatMap { (_, points) -> points.map { it.second } }
    val min = values.minOrNull() ?: return null
    val max = values.max()
    val position = xs.withIndex().associate { (i, x) -> x to i }
    val step = (xs.size / 6).coerceAtLeast(1)
    return LineChartData(
        labels = xs.mapIndexed { i, x -> if (i % step == 0) x.toDoubleOrNull()?.let { formatNumber(it) } ?: x.shortTime(zone) else "" },
        lines =
            raw.map { (line, points) ->
                line.copy(points = points.mapNotNull { (x, v) -> position[x]?.let { it to v } }.sortedBy { it.first })
            },
        min = min,
        max = if (max == min) max + 1 else max,
    )
}

internal fun statValues(result: QueryResult): List<Pair<String, JsonElement>> =
    result.series.flatMap { s ->
        val row = s.rows.lastOrNull() ?: return@flatMap emptyList()
        s.columns.indices
            .filter { s.columns[it] != "ts" }
            .map { c -> (s.dims.values + s.columns[c]).joinToString(" · ") to row[c] }
    }

private fun JsonElement.text() = (this as? JsonPrimitive)?.content ?: toString()

internal fun JsonElement.number(): Double? =
    (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.booleanOrNull?.let { b -> if (b) 1.0 else 0.0 } }

internal fun JsonElement.format(
    decimals: Int? = null,
    zone: TimeZone = TimeZone.UTC,
): String {
    val p = this as? JsonPrimitive ?: return toString()
    if (p is JsonNull) return ""
    val d = if (p.isString) null else p.doubleOrNull
    return d?.let { formatNumber(it, decimals) } ?: p.content.shortTime(zone)
}

internal fun formatValue(
    v: Double,
    options: WidgetOptions,
    display: UnitDisplay,
) = formatNumber(display.convert(v), options.decimals) + display.unit?.let { " $it" }.orEmpty()

internal fun formatNumber(
    d: Double,
    decimals: Int? = null,
): String {
    if (decimals == null) {
        return if (d % 1.0 == 0.0 && abs(d) < 1e15) d.toLong().toString() else ((d * 100).roundToLong() / 100.0).toString()
    }
    val places = decimals.coerceIn(0, 9)
    val scale = 10.0.pow(places).toLong()
    val n = (abs(d) * scale).roundToLong()
    val sign = if (d < 0 && n != 0L) "-" else ""
    return sign + (n / scale) + if (places == 0) "" else "." + (n % scale).toString().padStart(places, '0')
}

internal fun thresholdColor(
    v: Double,
    thresholds: List<Threshold>,
): Color? =
    thresholds
        .sortedBy { it.value }
        .lastOrNull { v >= it.value }
        ?.color
        ?.let(::parseColor)

internal fun parseColor(hex: String): Color? =
    hex
        .removePrefix("#")
        .takeIf { it.length == 6 }
        ?.toLongOrNull(16)
        ?.let { Color(0xFF000000 or it) }

internal fun String.shortTime(zone: TimeZone = TimeZone.UTC): String {
    if (!(length >= 16 && this[4] == '-' && this[7] == '-' && this[10] == 'T')) return this
    val t = Instant.parseOrNull(this)?.toLocalDateTime(zone) ?: return substring(5, 16).replace('T', ' ')

    fun Int.two() = toString().padStart(2, '0')
    return "${t.month.number.two()}-${t.day.two()} ${t.hour.two()}:${t.minute.two()}"
}
