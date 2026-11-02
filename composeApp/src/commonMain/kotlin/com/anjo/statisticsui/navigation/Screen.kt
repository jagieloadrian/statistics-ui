package com.anjo.statisticsui.navigation

sealed class Screen(
    val route: String,
) {
    data object Splash : Screen("splash")

    data object Login : Screen("login")

    data object Main : Screen("main")

    data object DashboardView : Screen("dashboard/{id}") {
        fun route(id: String) = "dashboard/$id"
    }

    data object Sources : Screen("sources")

    data object Datasets : Screen("datasets")

    data object Users : Screen("users")
}
