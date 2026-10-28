package com.anjo.statisticsui.ui.dashboard

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
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.UnitDisplay
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.model.WidgetOptions
import com.anjo.statisticsui.model.unitDisplay
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.LocalPrefs
import com.anjo.statisticsui.ui.theme.Colors
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.color.ChartyColors
import com.himanshoe.charty.common.ChartScaffold
import com.himanshoe.charty.common.axis.AxisConfig
import com.himanshoe.charty.common.config.ChartScaffoldConfig
import kotlinx.datetime.TimeZone
import kotlin.math.abs

private val PALETTE = ChartyColors.ModernPalette.value + ChartyColors.NaturePalette.value
private const val MAX_TABLE_ROWS = 200
private const val GAUGE_START = 150f
private const val GAUGE_SWEEP = 240f

@Composable
fun Widget(
    widget: Widget,
    result: Result<QueryResult>?,
    modifier: Modifier = Modifier,
) {
    val data = result?.getOrNull()
    val zone = displayZone()
    val display = unitDisplay(widget.options.unit, LocalPrefs.current.units)
    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val error = result?.exceptionOrNull()
            when {
                result == null -> {
                    CircularProgressIndicator()
                }

                (error as? ApiException)?.status == 400 -> {
                    Column {
                        Text("Missing data definition", color = Colors.LIGHT_ORANGE, fontWeight = FontWeight.Bold)
                        Text(error.message.orEmpty(), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }

                data == null -> {
                    ErrorText(error?.message ?: "Query failed")
                }

                data.series.all { it.rows.isEmpty() } -> {
                    Text("No data in range", color = Color.Gray)
                }

                widget.type in setOf("line", "area", "bar") -> {
                    SeriesChart(data, widget, zone, display)
                }

                widget.type == "stat" -> {
                    StatWidget(data, widget.options, zone, display)
                }

                widget.type == "gauge" -> {
                    GaugeWidget(data, widget.options, display)
                }

                widget.type == "table" -> {
                    TableWidget(data, widget.options, zone)
                }

                else -> {
                    Text("Widget type '${widget.type}' is not supported yet", color = Color.Gray)
                }
            }
        }
        data?.bucket?.takeIf { it.isNotBlank() }?.let { Text("Bucket: $it", color = Color.Gray, fontSize = 11.sp) }
    }
}

@Composable
private fun SeriesChart(
    result: QueryResult,
    widget: Widget,
    zone: TimeZone,
    display: UnitDisplay,
) {
    val options = widget.options
    val chart = remember(result, options.x, zone, display.unit) { lineChartData(result, options.x ?: "ts", zone, display) }
    if (chart == null) {
        Text("No data in range", color = Color.Gray)
        return
    }
    val bar = widget.type == "bar"
    val area = widget.type == "area"
    val min = options.min?.let { display.convert(it).toFloat() } ?: if (bar) minOf(chart.min, 0f) else chart.min
    val max =
        (options.max?.let { display.convert(it).toFloat() } ?: if (bar) maxOf(chart.max, 0f) else chart.max).let {
            if (it <=
                min
            ) {
                min + 1
            } else {
                it
            }
        }
    val colors =
        chart.lines.mapIndexed { i, line ->
            (options.colors[line.column] ?: options.colors[line.name])?.let(::parseColor) ?: PALETTE[i % PALETTE.size]
        }
    Column(Modifier.fillMaxSize()) {
        ChartScaffold(
            modifier = Modifier.fillMaxWidth().weight(1f),
            xLabels = chart.labels,
            yAxisConfig = AxisConfig(minValue = min, maxValue = max, drawAxisAtZero = false),
            config =
                ChartScaffoldConfig(
                    axisColor = ChartyColor.Solid(Color.Gray),
                    gridColor = ChartyColor.Solid(Color.Gray.copy(alpha = 0.2f)),
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

@Composable
private fun StatWidget(
    result: QueryResult,
    options: WidgetOptions,
    zone: TimeZone,
    display: UnitDisplay,
) {
    val values = remember(result) { statValues(result) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MEDIUM_PADDING), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        values.forEach { (label, value) ->
            val number = value.number()
            Column {
                Text(
                    number?.let { formatValue(it, options, display) } ?: value.format(zone = zone),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = number?.let { thresholdColor(it, options.thresholds) } ?: MaterialTheme.colorScheme.onSurface,
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun GaugeWidget(
    result: QueryResult,
    options: WidgetOptions,
    display: UnitDisplay,
) {
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
                                drawArc(
                                    c.copy(alpha = 0.35f),
                                    GAUGE_START + sweep(t.value),
                                    sweep(end) - sweep(t.value),
                                    false,
                                    topLeft,
                                    arcSize,
                                    style = Stroke(width),
                                )
                            }
                        }
                        drawArc(color, GAUGE_START, sweep(v), false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
                    }
                    Text(
                        formatValue(v, options, display),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun TableWidget(
    result: QueryResult,
    options: WidgetOptions,
    zone: TimeZone,
) {
    val (header, rows) =
        remember(result, options.decimals, zone) {
            val dims = result.series.flatMap { it.dims.keys }.distinct()
            val columns = result.series.flatMap { it.columns }.distinct()
            dims + columns to
                result.series.flatMap { s ->
                    s.rows.map { r ->
                        dims.map { s.dims[it].orEmpty() } +
                            columns.map { c -> r.getOrNull(s.columns.indexOf(c))?.format(options.decimals, zone).orEmpty() }
                    }
                }
        }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TableRow(header, header = true)
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        rows.take(MAX_TABLE_ROWS).forEach { TableRow(it) }
        if (rows.size > MAX_TABLE_ROWS) Text("… ${rows.size - MAX_TABLE_ROWS} more rows", color = Color.Gray)
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    header: Boolean = false,
) {
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

@Composable
internal fun displayZone(): TimeZone {
    val id = LocalPrefs.current.timeZone
    return remember(id) { id?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: TimeZone.currentSystemDefault() }
}
