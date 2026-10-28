package com.anjo.statisticsui.ui.dashboard

import com.anjo.statisticsui.model.Widget

internal const val COLUMNS = 12

/** 12-column flow layout: widgets fill a row left to right; one that no longer fits starts the next row. */
internal fun layoutRows(widgets: List<Widget>): List<List<Widget>> =
    widgets.fold(mutableListOf<MutableList<Widget>>()) { rows, w ->
        val last = rows.lastOrNull()
        if (last != null && last.sumOf { it.span() } + w.span() <= COLUMNS) last += w else rows += mutableListOf(w)
        rows
    }

internal fun Widget.span() = w.coerceIn(1, COLUMNS)

internal fun <T> List<T>.swap(
    i: Int,
    j: Int,
) = toMutableList().also {
    it[i] = this[j]
    it[j] = this[i]
}
