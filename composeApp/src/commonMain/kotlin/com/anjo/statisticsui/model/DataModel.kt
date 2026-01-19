package com.anjo.statisticsui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class DataTypeTile(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val route: String
)