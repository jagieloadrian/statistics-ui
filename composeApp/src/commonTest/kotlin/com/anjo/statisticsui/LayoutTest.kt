package com.anjo.statisticsui

import com.anjo.statisticsui.api.Query
import com.anjo.statisticsui.api.Widget
import com.anjo.statisticsui.presentation.dashboard.layoutRows
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutTest {
    private fun widget(id: String, w: Int) = Widget(id = id, type = "line", w = w, query = Query(dataset = "d"))

    @Test
    fun widgetsFillTwelveColumnRowsLeftToRight() {
        val rows = layoutRows(listOf(widget("a", 8), widget("b", 4), widget("c", 6), widget("d", 7), widget("e", 5), widget("f", 12)))
        assertEquals(listOf(listOf("a", "b"), listOf("c"), listOf("d", "e"), listOf("f")), rows.map { row -> row.map { it.id } })
    }

    @Test
    fun widthIsClampedToOneToTwelve() {
        val rows = layoutRows(listOf(widget("a", 20), widget("b", 0), widget("c", 11)))
        assertEquals(listOf(listOf("a"), listOf("b", "c")), rows.map { row -> row.map { it.id } })
    }
}
