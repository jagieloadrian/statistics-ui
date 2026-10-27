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

/**
 * One line per series × measure column, x = [xColumn] (`options.x`, default `ts`); all lines share the merged x values
 * as positions, sorted numerically when numeric (e.g. `generation`), else as text (ISO timestamps).
 */
internal fun lineChartData(result: QueryResult, xColumn: String = "ts", zone: TimeZone = TimeZone.UTC,
                           display: UnitDisplay = UnitDisplay(null)): LineChartData? {
    val raw = result.series.flatMap { s ->
        val x = s.columns.indexOf(xColumn)
        if (x < 0) emptyList() else s.columns.indices.filter { it != x && s.columns[it] != "ts" }.map { c ->
            ChartLine((s.dims.values + s.columns[c]).joinToString(" · "), s.columns[c], emptyList()) to
                s.rows.mapNotNull { r -> r[c].number()?.let { r[x].text() to display.convert(it).toFloat() } }
        }
    }
    // ponytail: x = index of the merged x values (evenly spaced), fine for bucketed data; proportional axis if gaps matter
    val xs = raw.flatMap { (_, points) -> points.map { it.first } }.distinct()
        .sortedWith(compareBy<String> { it.toDoubleOrNull() }.thenBy { it })
    if (xs.isEmpty()) return null
    val index = xs.withIndex().associate { (i, x) -> x to i }
    val values = raw.flatMap { (_, points) -> points.map { it.second } }
    val step = (xs.size / 6).coerceAtLeast(1)
    return LineChartData(
        labels = xs.mapIndexed { i, x -> if (i % step == 0) x.toDoubleOrNull()?.let { formatNumber(it) } ?: x.shortTime(zone) else "" },
        lines = raw.map { (line, points) -> line.copy(points = points.map { (x, v) -> index.getValue(x) to v }.sortedBy { it.first }) },
        min = values.min(),
        max = values.max().let { if (it == values.min()) it + 1 else it },
    )
}

/** One value per column of each series' last row; the label is the column name (= the measure's `as`). */
internal fun statValues(result: QueryResult): List<Pair<String, JsonElement>> = result.series.flatMap { s ->
    val row = s.rows.lastOrNull() ?: return@flatMap emptyList()
    s.columns.indices.filter { s.columns[it] != "ts" }.map { c -> (s.dims.values + s.columns[c]).joinToString(" · ") to row[c] }
}

private fun JsonElement.text() = (this as? JsonPrimitive)?.content ?: toString()

internal fun JsonElement.number(): Double? =
    (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.booleanOrNull?.let { b -> if (b) 1.0 else 0.0 } }

internal fun JsonElement.format(decimals: Int? = null, zone: TimeZone = TimeZone.UTC): String {
    val p = this as? JsonPrimitive ?: return toString()
    if (p is JsonNull) return ""
    val d = if (p.isString) null else p.doubleOrNull
    return d?.let { formatNumber(it, decimals) } ?: p.content.shortTime(zone)
}

/** Stored value in the user's unit system (thresholds and gauge range stay in the declared unit). */
internal fun formatValue(v: Double, options: WidgetOptions, display: UnitDisplay) =
    formatNumber(display.convert(v), options.decimals) + display.unit?.let { " $it" }.orEmpty()

/** Exactly [decimals] places when given; otherwise whole numbers without decimals and others rounded to 2 places. */
internal fun formatNumber(d: Double, decimals: Int? = null): String {
    if (decimals == null) {
        return if (d % 1.0 == 0.0 && abs(d) < 1e15) d.toLong().toString() else ((d * 100).roundToLong() / 100.0).toString()
    }
    val places = decimals.coerceIn(0, 9)
    val scale = 10.0.pow(places).toLong()
    val n = (abs(d) * scale).roundToLong()
    val sign = if (d < 0 && n != 0L) "-" else ""
    return sign + (n / scale) + if (places == 0) "" else "." + (n % scale).toString().padStart(places, '0')
}

/** Color of the highest threshold at or below [v]. */
internal fun thresholdColor(v: Double, thresholds: List<Threshold>): Color? =
    thresholds.sortedBy { it.value }.lastOrNull { v >= it.value }?.color?.let(::parseColor)

/** `#RRGGBB` → [Color]; anything else → null (palette color used). */
internal fun parseColor(hex: String): Color? =
    hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }

/** ISO instant `2026-10-26T13:05:00Z` → `10-26 13:05` in [zone]; anything else unchanged. */
internal fun String.shortTime(zone: TimeZone = TimeZone.UTC): String {
    if (!(length >= 16 && this[4] == '-' && this[7] == '-' && this[10] == 'T')) return this
    val t = runCatching { Instant.parse(this).toLocalDateTime(zone) }.getOrNull() ?: return substring(5, 16).replace('T', ' ')
    fun Int.two() = toString().padStart(2, '0')
    return "${t.month.number.two()}-${t.day.two()} ${t.hour.two()}:${t.minute.two()}"
}
