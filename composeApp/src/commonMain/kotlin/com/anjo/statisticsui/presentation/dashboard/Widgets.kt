package com.anjo.statisticsui.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anjo.statisticsui.api.QueryResult
import com.anjo.statisticsui.api.Widget
import com.anjo.statisticsui.presentation.common.ErrorText
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.himanshoe.charty.color.ChartyColors
import com.himanshoe.charty.common.ChartScaffold
import com.himanshoe.charty.common.axis.AxisConfig
import com.himanshoe.charty.common.config.ChartScaffoldConfig
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.abs
import kotlin.math.roundToLong

private val PALETTE = ChartyColors.ModernPalette.value + ChartyColors.NaturePalette.value
private const val MAX_TABLE_ROWS = 200

/** Renders one widget; [result] null = loading, failure = error message. */
@Composable
fun Widget(widget: Widget, result: Result<QueryResult>?) {
    val data = result?.getOrNull()
    when {
        result == null -> CircularProgressIndicator()
        data == null -> ErrorText(result.exceptionOrNull()?.message ?: "Query failed")
        widget.type == "line" -> LineWidget(data, widget.h)
        widget.type == "table" -> TableWidget(data)
        else -> Text("Widget type '${widget.type}' is not supported yet", color = Color.Gray)
    }
}

internal class LineChartData(val labels: List<String>, val lines: List<Pair<String, List<Pair<Int, Float>>>>, val min: Float, val max: Float)

/** One line per series × measure column, x = `ts`; all lines share the merged, sorted `ts` values as x positions. */
internal fun lineChartData(result: QueryResult): LineChartData? {
    val raw = result.series.flatMap { s ->
        val x = s.columns.indexOf("ts")
        if (x < 0) emptyList() else s.columns.indices.filter { it != x }.map { c ->
            (s.dims.values + s.columns[c]).joinToString(" · ") to s.rows.mapNotNull { r -> r[c].number()?.let { r[x].text() to it } }
        }
    }
    // ponytail: x = index of the merged timestamps (evenly spaced), fine for bucketed data; proportional time axis if gaps matter
    val xs = raw.flatMap { (_, points) -> points.map { it.first } }.distinct().sorted()
    if (xs.isEmpty()) return null
    val index = xs.withIndex().associate { (i, x) -> x to i }
    val values = raw.flatMap { (_, points) -> points.map { it.second } }
    val step = (xs.size / 6).coerceAtLeast(1)
    return LineChartData(
        labels = xs.mapIndexed { i, x -> if (i % step == 0) x.shortTime() else "" },
        lines = raw.map { (name, points) -> name to points.map { (x, v) -> index.getValue(x) to v }.sortedBy { it.first } },
        min = values.min(),
        max = values.max().let { if (it == values.min()) it + 1 else it },
    )
}

@Composable
private fun LineWidget(result: QueryResult, h: Int) {
    val chart = remember(result) { lineChartData(result) }
    if (chart == null) {
        Text("No data in range", color = Color.Gray)
        return
    }
    // charty's MultilineChart paints every line with one brush, so lines are drawn on its scaffold here
    ChartScaffold(
        modifier = Modifier.fillMaxWidth().height((h * 70).dp),
        xLabels = chart.labels,
        yAxisConfig = AxisConfig(minValue = chart.min, maxValue = chart.max, drawAxisAtZero = false),
        config = ChartScaffoldConfig(
            axisColor = Color.Gray,
            gridColor = Color.Gray.copy(alpha = 0.2f),
            labelTextStyle = TextStyle(color = Color.LightGray, fontSize = 10.sp),
        ),
    ) { ctx ->
        chart.lines.forEachIndexed { i, (_, points) ->
            val color = PALETTE[i % PALETTE.size]
            val path = Path()
            points.forEachIndexed { n, (x, v) ->
                val p = Offset(ctx.calculateCenteredXPosition(x, chart.labels.size), ctx.convertValueToYPosition(v))
                if (n == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                if (chart.labels.size <= 60) drawCircle(color, 3.dp.toPx(), p)
            }
            drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
        chart.lines.forEachIndexed { i, (name, _) -> Text("● $name", color = PALETTE[i % PALETTE.size], fontSize = 12.sp) }
    }
}

@Composable
private fun TableWidget(result: QueryResult) {
    val (header, rows) = remember(result) {
        val dims = result.series.flatMap { it.dims.keys }.distinct()
        val columns = result.series.flatMap { it.columns }.distinct()
        dims + columns to result.series.flatMap { s ->
            s.rows.map { r -> dims.map { s.dims[it].orEmpty() } + columns.map { c -> r.getOrNull(s.columns.indexOf(c))?.format().orEmpty() } }
        }
    }
    if (rows.isEmpty()) {
        Text("No data in range", color = Color.Gray)
        return
    }
    Column {
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
                color = if (header) Color.Gray else Color.White,
            )
        }
    }
}

private fun JsonElement.text() = (this as? JsonPrimitive)?.content ?: toString()

private fun JsonElement.number(): Float? =
    (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.booleanOrNull?.let { b -> if (b) 1.0 else 0.0 } }?.toFloat()

private fun JsonElement.format(): String {
    val p = this as? JsonPrimitive ?: return toString()
    if (p is JsonNull) return ""
    val d = if (p.isString) null else p.doubleOrNull
    return when {
        d == null -> p.content
        d % 1.0 == 0.0 && abs(d) < 1e15 -> d.toLong().toString()
        else -> ((d * 100).roundToLong() / 100.0).toString()
    }
}

/** `2026-10-26T13:05:00Z` → `10-26 13:05`; anything else unchanged. */
private fun String.shortTime() = if (length >= 16 && this[10] == 'T') substring(5, 16).replace('T', ' ') else this
