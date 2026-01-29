package com.anjo.statisticsui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anjo.statisticsui.presentation.details.DataDetailScreen
import com.anjo.statisticsui.presentation.home.DashboardContent
import com.anjo.statisticsui.presentation.home.MainLayout
import com.anjo.statisticsui.presentation.home.PlaceholderScreen
import com.anjo.statisticsui.presentation.splash.SplashScreen
import com.anjo.statisticsui.ui.theme.Colors.LIGHT_ORANGE
import com.anjo.statisticsui.ui.theme.Colors.LIGHT_RED

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
        composable(Screen.TemperatureDetails.route) {
            DataDetailScreen("Temperature statistics", Icons.Default.Thermostat, LIGHT_ORANGE, navController)
        }
        composable(Screen.EpidemicDetails.route) {
            DataDetailScreen("Epidemic statistics", Icons.Default.Coronavirus, LIGHT_RED, navController)
        }
        composable(Screen.Charts.route) {
            PlaceholderScreen("Charts")
        }
        composable(Screen.Account.route) {
            PlaceholderScreen("Account settings")
        }
    }
}

