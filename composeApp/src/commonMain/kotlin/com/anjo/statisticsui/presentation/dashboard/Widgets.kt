package com.anjo.statisticsui.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anjo.statisticsui.api.ApiException
import com.anjo.statisticsui.api.QueryResult
import com.anjo.statisticsui.api.Threshold
import com.anjo.statisticsui.api.Widget
import com.anjo.statisticsui.api.WidgetOptions
import com.anjo.statisticsui.auth.LocalPrefs
import com.anjo.statisticsui.presentation.common.ErrorText
import com.anjo.statisticsui.ui.theme.Colors
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.himanshoe.charty.color.ChartyColors
import com.himanshoe.charty.common.ChartScaffold
import com.himanshoe.charty.common.axis.AxisConfig
import com.himanshoe.charty.common.config.ChartScaffoldConfig
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

private val PALETTE = ChartyColors.ModernPalette.value + ChartyColors.NaturePalette.value
private const val MAX_TABLE_ROWS = 200
private const val GAUGE_START = 150f
private const val GAUGE_SWEEP = 240f

/** Renders one widget filling [modifier]; [result] null = loading, failure = error message. */
@Composable
fun Widget(widget: Widget, result: Result<QueryResult>?, modifier: Modifier = Modifier) {
    val data = result?.getOrNull()
    val zone = displayZone()
    val display = unitDisplay(widget.options.unit, LocalPrefs.current.units)
    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val error = result?.exceptionOrNull()
            when {
                result == null -> CircularProgressIndicator()
                // the server answers 400 naming an unknown dataset/field, e.g. after a measure was deleted with force
                (error as? ApiException)?.status == 400 -> Column {
                    Text("Missing data definition", color = Colors.LIGHT_ORANGE, fontWeight = FontWeight.Bold)
                    Text(error.message.orEmpty(), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                data == null -> ErrorText(error?.message ?: "Query failed")
                data.series.all { it.rows.isEmpty() } -> Text("No data in range", color = Color.Gray)
                widget.type in setOf("line", "area", "bar") -> SeriesChart(data, widget, zone, display)
                widget.type == "stat" -> StatWidget(data, widget.options, zone, display)
                widget.type == "gauge" -> GaugeWidget(data, widget.options, display)
                widget.type == "table" -> TableWidget(data, widget.options, zone)
                else -> Text("Widget type '${widget.type}' is not supported yet", color = Color.Gray)
            }
        }
        data?.bucket?.takeIf { it.isNotBlank() }?.let { Text("Bucket: $it", color = Color.Gray, fontSize = 11.sp) }
    }
}

internal data class ChartLine(val name: String, val column: String, val points: List<Pair<Int, Float>>)

internal class LineChartData(val labels: List<String>, val lines: List<ChartLine>, val min: Float, val max: Float)

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

/** `line`, `area` or `bar`; charty's MultilineChart paints every line with one brush, so series are drawn on its scaffold. */
@Composable
private fun SeriesChart(result: QueryResult, widget: Widget, zone: TimeZone, display: UnitDisplay) {
    val options = widget.options
    val chart = remember(result, options.x, zone, display.unit) { lineChartData(result, options.x ?: "ts", zone, display) }
    if (chart == null) {
        Text("No data in range", color = Color.Gray)
        return
    }
    val bar = widget.type == "bar"
    val area = widget.type == "area"
    // bars grow from zero, so zero must be on the axis
    val min = options.min?.let { display.convert(it).toFloat() } ?: if (bar) minOf(chart.min, 0f) else chart.min
    val max = (options.max?.let { display.convert(it).toFloat() } ?: if (bar) maxOf(chart.max, 0f) else chart.max).let { if (it <= min) min + 1 else it }
    val colors = chart.lines.mapIndexed { i, line ->
        (options.colors[line.column] ?: options.colors[line.name])?.let(::parseColor) ?: PALETTE[i % PALETTE.size]
    }
    Column(Modifier.fillMaxSize()) {
        ChartScaffold(
            modifier = Modifier.fillMaxWidth().weight(1f),
            xLabels = chart.labels,
            yAxisConfig = AxisConfig(minValue = min, maxValue = max, drawAxisAtZero = false),
            config = ChartScaffoldConfig(
                axisColor = Color.Gray,
                gridColor = Color.Gray.copy(alpha = 0.2f),
                labelTextStyle = TextStyle(color = Color.LightGray, fontSize = 10.sp),
            ),
        ) { ctx ->
            val slot = ctx.width / chart.labels.size
            val barWidth = slot * 0.8f / chart.lines.size
            val zero = ctx.convertValueToYPosition(0f.coerceIn(min, max))
            chart.lines.forEachIndexed { i, line ->
                val color = colors[i]
                val path = Path()
                val fill = Path()
                line.points.forEachIndexed { n, (x, v) ->
                    val p = Offset(ctx.calculateCenteredXPosition(x, chart.labels.size), ctx.convertValueToYPosition(v))
                    if (bar) {
                        val left = ctx.left + slot * x + slot * 0.1f + barWidth * i
                        drawRect(color, Offset(left, minOf(p.y, zero)), Size(barWidth, abs(zero - p.y)))
                    } else {
                        if (n == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                        if (n == 0) fill.moveTo(p.x, ctx.bottom)
                        fill.lineTo(p.x, p.y)
                        if (n == line.points.lastIndex) fill.lineTo(p.x, ctx.bottom)
                        if (chart.labels.size <= 60) drawCircle(color, 3.dp.toPx(), p)
                    }
                }
                if (area) drawPath(fill, color.copy(alpha = 0.25f))
                if (!bar) drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            val unit = display.unit?.let { " ($it)" }.orEmpty()
            chart.lines.forEachIndexed { i, line -> Text("● ${line.name}$unit", color = colors[i], fontSize = 12.sp) }
        }
    }
}

/** One value per column of each series' last row; the label is the column name (= the measure's `as`). */
internal fun statValues(result: QueryResult): List<Pair<String, JsonElement>> = result.series.flatMap { s ->
    val row = s.rows.lastOrNull() ?: return@flatMap emptyList()
    s.columns.indices.filter { s.columns[it] != "ts" }.map { c -> (s.dims.values + s.columns[c]).joinToString(" · ") to row[c] }
}

@Composable
private fun StatWidget(result: QueryResult, options: WidgetOptions, zone: TimeZone, display: UnitDisplay) {
    val values = remember(result) { statValues(result) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        values.forEach { (label, value) ->
            val number = value.number()
            Column {
                Text(
                    number?.let { formatValue(it, options, display) } ?: value.format(zone = zone),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = number?.let { thresholdColor(it, options.thresholds) } ?: MaterialTheme.colorScheme.onSurface
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

/** 240° arc per numeric value between `options.min`/`max` (default 0–100); thresholds tint the track and the value. */
@Composable
private fun GaugeWidget(result: QueryResult, options: WidgetOptions, display: UnitDisplay) {
    val values = remember(result) { statValues(result).mapNotNull { (label, v) -> v.number()?.let { label to it } } }
    val min = options.min ?: 0.0
    val max = (options.max ?: 100.0).let { if (it <= min) min + 1 else it }
    val thresholds = options.thresholds.sortedBy { it.value }
    fun sweep(v: Double) = ((v - min) / (max - min)).coerceIn(0.0, 1.0).toFloat() * GAUGE_SWEEP
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING)) {
        values.forEach { (label, v) ->
            val color = thresholdColor(v, thresholds) ?: MaterialTheme.colorScheme.primary
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val width = 12.dp.toPx()
                        val topLeft = Offset(width / 2, width / 2)
                        val arcSize = Size(size.width - width, size.height - width)
                        drawArc(Color.Gray.copy(alpha = 0.3f), GAUGE_START, GAUGE_SWEEP, false, topLeft, arcSize, style = Stroke(width))
                        thresholds.forEachIndexed { i, t ->
                            val end = thresholds.getOrNull(i + 1)?.value ?: max
                            parseColor(t.color)?.let { c ->
                                drawArc(c.copy(alpha = 0.35f), GAUGE_START + sweep(t.value), sweep(end) - sweep(t.value), false,
                                    topLeft, arcSize, style = Stroke(width))
                            }
                        }
                        drawArc(color, GAUGE_START, sweep(v), false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
                    }
                    Text(formatValue(v, options, display), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun TableWidget(result: QueryResult, options: WidgetOptions, zone: TimeZone) {
    val (header, rows) = remember(result, options.decimals, zone) {
        val dims = result.series.flatMap { it.dims.keys }.distinct()
        val columns = result.series.flatMap { it.columns }.distinct()
        dims + columns to result.series.flatMap { s ->
            s.rows.map { r ->
                dims.map { s.dims[it].orEmpty() } + columns.map { c -> r.getOrNull(s.columns.indexOf(c))?.format(options.decimals, zone).orEmpty() }
            }
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TableRow(header, header = true)
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        // ponytail: plain Column, capped; virtualise if large raw tables are needed
        rows.take(MAX_TABLE_ROWS).forEach { TableRow(it) }
        if (rows.size > MAX_TABLE_ROWS) Text("… ${rows.size - MAX_TABLE_ROWS} more rows", color = Color.Gray)
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        cells.forEach {
            Text(
                it,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                color = if (header) Color.Gray else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun JsonElement.text() = (this as? JsonPrimitive)?.content ?: toString()

private fun JsonElement.number(): Double? =
    (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.booleanOrNull?.let { b -> if (b) 1.0 else 0.0 } }

private fun JsonElement.format(decimals: Int? = null, zone: TimeZone = TimeZone.UTC): String {
    val p = this as? JsonPrimitive ?: return toString()
    if (p is JsonNull) return ""
    val d = if (p.isString) null else p.doubleOrNull
    return d?.let { formatNumber(it, decimals) } ?: p.content.shortTime(zone)
}

/** Stored value in the user's unit system (thresholds and gauge range stay in the declared unit). */
private fun formatValue(v: Double, options: WidgetOptions, display: UnitDisplay) =
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
private fun thresholdColor(v: Double, thresholds: List<Threshold>): Color? =
    thresholds.sortedBy { it.value }.lastOrNull { v >= it.value }?.color?.let(::parseColor)

/** `#RRGGBB` → [Color]; anything else → null (palette color used). */
internal fun parseColor(hex: String): Color? =
    hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }

/** The signed-in user's time zone, else the system zone. */
@Composable
internal fun displayZone(): TimeZone {
    val id = LocalPrefs.current.timeZone
    return remember(id) { id?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: TimeZone.currentSystemDefault() }
}

/** ISO instant `2026-10-26T13:05:00Z` → `10-26 13:05` in [zone]; anything else unchanged. */
internal fun String.shortTime(zone: TimeZone = TimeZone.UTC): String {
    if (!(length >= 16 && this[4] == '-' && this[7] == '-' && this[10] == 'T')) return this
    val t = runCatching { Instant.parse(this).toLocalDateTime(zone) }.getOrNull() ?: return substring(5, 16).replace('T', ' ')
    fun Int.two() = toString().padStart(2, '0')
    return "${t.month.number.two()}-${t.day.two()} ${t.hour.two()}:${t.minute.two()}"
}
