package com.anjo.statisticsui.navigation

import com.anjo.statisticsui.navigation.Destination.ACCOUNT
import com.anjo.statisticsui.navigation.Destination.CHARTS
import com.anjo.statisticsui.navigation.Destination.DASHBOARD
import com.anjo.statisticsui.navigation.Destination.EPIDEMIC_DETAILS
import com.anjo.statisticsui.navigation.Destination.MAIN
import com.anjo.statisticsui.navigation.Destination.SPLASH
import com.anjo.statisticsui.navigation.Destination.TEMP_DETAILS

object Destination {
    const val DASHBOARD = "dashboard"
    const val TEMP_DETAILS = "temp_details"
    const val EPIDEMIC_DETAILS = "epidemic_details"
    const val CHARTS = "charts"
    const val ACCOUNT = "account"
    const val SPLASH = "splash"
    const val MAIN = "main"
}


sealed class Screen(val route: String) {
    data object Main : Screen(MAIN)
    data object Dashboard : Screen(DASHBOARD)
    data object TemperatureDetails : Screen(TEMP_DETAILS)
    data object EpidemicDetails : Screen(EPIDEMIC_DETAILS)
    data object Charts : Screen(CHARTS)
    data object Account : Screen(ACCOUNT)
    data object Splash: Screen(SPLASH)
}