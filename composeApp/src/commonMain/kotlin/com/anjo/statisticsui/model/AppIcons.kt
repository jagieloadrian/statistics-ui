package com.anjo.statisticsui.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Bar_chart
import com.composables.icons.materialsymbols.rounded.Home
import com.composables.icons.materialsymbols.rounded.Person

enum class ScreenIcons(val label: String, val icon: ImageVector) {
    Main("Main", MaterialSymbols.Rounded.Home),
    Charts("Charts", MaterialSymbols.Rounded.Bar_chart),
    Account("Account", MaterialSymbols.Rounded.Person)
}