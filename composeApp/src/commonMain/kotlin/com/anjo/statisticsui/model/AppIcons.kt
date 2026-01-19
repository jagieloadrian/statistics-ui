package com.anjo.statisticsui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class ScreenIcons(val label: String, val icon: ImageVector) {
    Main("Main", Icons.Default.Home),
    Charts("Charts", Icons.Default.BarChart),
    Account("Account", Icons.Default.Person)
}