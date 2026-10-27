package com.anjo.statisticsui.navigation

import com.anjo.statisticsui.navigation.Destination.ACCOUNT
import com.anjo.statisticsui.navigation.Destination.CHARTS
import com.anjo.statisticsui.navigation.Destination.DASHBOARD
import com.anjo.statisticsui.navigation.Destination.DASHBOARD_VIEW
import com.anjo.statisticsui.navigation.Destination.DATASETS
import com.anjo.statisticsui.navigation.Destination.MAIN
import com.anjo.statisticsui.navigation.Destination.SOURCES
import com.anjo.statisticsui.navigation.Destination.SPLASH

object Destination {
    const val DASHBOARD = "dashboard"
    const val CHARTS = "charts"
    const val ACCOUNT = "account"
    const val SPLASH = "splash"
    const val MAIN = "main"
    const val DASHBOARD_VIEW = "dashboard/{id}"
    const val SOURCES = "sources"
    const val DATASETS = "datasets"
}


sealed class Screen(val route: String) {
    data object Main : Screen(MAIN)
    data object Dashboard : Screen(DASHBOARD)
    data object Charts : Screen(CHARTS)
    data object Account : Screen(ACCOUNT)
    data object Splash: Screen(SPLASH)
    data object DashboardView : Screen(DASHBOARD_VIEW) {
        fun route(id: String) = "dashboard/$id"
    }
    data object Sources : Screen(SOURCES)
    data object Datasets : Screen(DATASETS)
}