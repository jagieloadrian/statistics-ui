package com.anjo.statisticsui.navigation

import com.anjo.statisticsui.navigation.Destination.ACCOUNT
import com.anjo.statisticsui.navigation.Destination.CHARTS
import com.anjo.statisticsui.navigation.Destination.DASHBOARD
import com.anjo.statisticsui.navigation.Destination.MAIN
import com.anjo.statisticsui.navigation.Destination.SPLASH

object Destination {
    const val DASHBOARD = "dashboard"
    const val CHARTS = "charts"
    const val ACCOUNT = "account"
    const val SPLASH = "splash"
    const val MAIN = "main"
}


sealed class Screen(val route: String) {
    data object Main : Screen(MAIN)
    data object Dashboard : Screen(DASHBOARD)
    data object Charts : Screen(CHARTS)
    data object Account : Screen(ACCOUNT)
    data object Splash: Screen(SPLASH)
}