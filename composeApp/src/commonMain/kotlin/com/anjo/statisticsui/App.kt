package com.anjo.statisticsui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.di.StatsUIApplication
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.navigation.NavGraph
import com.anjo.statisticsui.ui.common.LocalPrefs
import com.anjo.statisticsui.ui.theme.accentScheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(configuration = koinConfiguration<StatsUIApplication>()) {
        val session = koinInject<Session>()
        val prefs = session.user?.prefs ?: Prefs()
        val dark =
            when (prefs.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
        val colors = remember(session.accent, dark) { accentScheme(session.accent, dark) }
        MaterialTheme(colorScheme = colors) {
            CompositionLocalProvider(LocalPrefs provides prefs) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val controller = rememberNavController()
                    NavGraph(controller)
                }
            }
        }
    }
}
