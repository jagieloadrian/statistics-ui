package com.anjo.statisticsui.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Home
import com.composables.icons.materialsymbols.rounded.Person

enum class ScreenIcons(
    val label: String,
    val icon: ImageVector,
) {
    Main("Home", MaterialSymbols.Rounded.Home),
    Account("Account", MaterialSymbols.Rounded.Person),
}
