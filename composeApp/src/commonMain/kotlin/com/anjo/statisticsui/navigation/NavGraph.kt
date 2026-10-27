package com.anjo.statisticsui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.savedstate.read
import com.anjo.statisticsui.presentation.admin.DatasetsScreen
import com.anjo.statisticsui.presentation.admin.SourcesScreen
import com.anjo.statisticsui.presentation.dashboard.DashboardScreen
import com.anjo.statisticsui.presentation.home.DashboardContent
import com.anjo.statisticsui.presentation.home.MainLayout
import com.anjo.statisticsui.presentation.home.PlaceholderScreen
import com.anjo.statisticsui.presentation.splash.SplashScreen

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = Screen.Splash.route, modifier: Modifier = Modifier) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn() + slideInHorizontally() },
        exitTransition = { fadeOut() + slideOutHorizontally() }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen { navController.navigate(Screen.Main.route) }
        }
        composable(Screen.Main.route) {
            MainLayout(navController = navController)
        }
        composable(Screen.Dashboard.route) {
            DashboardContent(navController)
        }
        composable(Screen.DashboardView.route) { entry ->
            DashboardScreen(entry.arguments?.read { getStringOrNull("id") }.orEmpty(), navController)
        }
        composable(Screen.Sources.route) {
            SourcesScreen(navController)
        }
        composable(Screen.Datasets.route) {
            DatasetsScreen(navController)
        }
        composable(Screen.Charts.route) {
            PlaceholderScreen("Charts")
        }
        composable(Screen.Account.route) {
            PlaceholderScreen("Account settings")
        }
    }
}

