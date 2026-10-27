package com.anjo.statisticsui.model

internal data class ChartLine(val name: String, val column: String, val points: List<Pair<Int, Float>>)

internal class LineChartData(val labels: List<String>, val lines: List<ChartLine>, val min: Float, val max: Float)
