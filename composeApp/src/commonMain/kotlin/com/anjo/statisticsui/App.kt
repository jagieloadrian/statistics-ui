package com.anjo.statisticsui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.di.StatsUIApplication
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.navigation.NavGraph
import com.anjo.statisticsui.ui.common.LocalPrefs
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

private val DarkColors =
    darkColorScheme(
        background = Color(0xFF1E1E2C),
        surface = Color(0xFF2D2D44),
        primary = Color(0xFF4DD0E1),
        onSurface = Color.White,
    )
private val LightColors = lightColorScheme(primary = Color(0xFF00838F))

@Composable
fun App() {
    KoinApplication(configuration = koinConfiguration<StatsUIApplication>()) {
        val prefs = koinInject<Session>().user?.prefs ?: Prefs()
        val dark =
            when (prefs.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
            CompositionLocalProvider(LocalPrefs provides prefs) {
                // Surface gives every destination the theme background (only MainLayout had one).
                Surface(color = MaterialTheme.colorScheme.background) {
                    val controller = rememberNavController()
                    NavGraph(controller)
                }
            }
        }
    }
}
