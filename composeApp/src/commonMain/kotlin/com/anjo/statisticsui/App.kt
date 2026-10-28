package com.anjo.statisticsui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.di.StatsUIApplication
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.navigation.NavGraph
import com.anjo.statisticsui.ui.common.LocalPrefs
import com.anjo.statisticsui.ui.theme.DarkColors
import com.anjo.statisticsui.ui.theme.LightColors
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

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
                Surface(color = MaterialTheme.colorScheme.background) {
                    val controller = rememberNavController()
                    NavGraph(controller)
                }
            }
        }
    }
}
