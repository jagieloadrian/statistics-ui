package com.anjo.statisticsui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.navigation.NavGraph

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF1E1E2C),
            surface = Color(0xFF2D2D44),
            primary = Color(0xFF4DD0E1),
            onSurface = Color.White
        )
    ) {
        val controller = rememberNavController()
        NavGraph(controller)
    }
}