package com.anjo.statisticsui

import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.ui.dashboard.layoutRows
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LayoutTest : FunSpec({
    fun widget(id: String, w: Int) = Widget(id = id, type = "line", w = w, query = Query(dataset = "d"))

    test("widgetsFillTwelveColumnRowsLeftToRight") {
        val rows = layoutRows(listOf(widget("a", 8), widget("b", 4), widget("c", 6), widget("d", 7), widget("e", 5), widget("f", 12)))
        rows.map { row -> row.map { it.id } } shouldBe listOf(listOf("a", "b"), listOf("c"), listOf("d", "e"), listOf("f"))
    }

    test("widthIsClampedToOneToTwelve") {
        val rows = layoutRows(listOf(widget("a", 20), widget("b", 0), widget("c", 11)))
        rows.map { row -> row.map { it.id } } shouldBe listOf(listOf("a"), listOf("b", "c"))
    }
})
