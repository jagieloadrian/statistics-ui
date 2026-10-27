package com.anjo.statisticsui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.savedstate.read
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.account.AccountScreen
import com.anjo.statisticsui.ui.admin.DatasetsScreen
import com.anjo.statisticsui.ui.admin.SourcesScreen
import com.anjo.statisticsui.ui.admin.UsersScreen
import com.anjo.statisticsui.ui.common.catching
import com.anjo.statisticsui.ui.dashboard.DashboardScreen
import com.anjo.statisticsui.ui.home.DashboardContent
import com.anjo.statisticsui.ui.home.MainLayout
import com.anjo.statisticsui.ui.home.PlaceholderScreen
import com.anjo.statisticsui.ui.login.LoginScreen
import com.anjo.statisticsui.ui.splash.SplashScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    modifier: Modifier = Modifier,
    session: Session = koinInject(),
    api: PlatformApi = koinInject(),
) {
    val scope = rememberCoroutineScope()
    fun reset(route: String) = navController.navigate(route) { popUpTo(navController.graph.id) { inclusive = true } }
    fun goHome(user: User) {
        reset(Screen.Main.route)
        user.prefs.homeDashboard?.let { navController.navigate(Screen.DashboardView.route(it)) }
    }

    // Sign-out from anywhere (drawer, a 401 on any call) lands on the login screen.
    LaunchedEffect(session.token) {
        val route = navController.currentDestination?.route
        if (session.token == null && route != null && route != Screen.Splash.route && route != Screen.Login.route) reset(Screen.Login.route)
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn() + slideInHorizontally() },
        exitTransition = { fadeOut() + slideOutHorizontally() }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen {
                scope.launch {
                    // A 401 here clears the stored token; an unreachable backend still opens the app (screens show the error).
                    val user = if (session.token == null) null else catching { api.me() }.getOrNull()
                    when {
                        user != null -> goHome(user)
                        session.token == null -> reset(Screen.Login.route)
                        else -> reset(Screen.Main.route)
                    }
                }
            }
        }
        composable(Screen.Login.route) {
            LoginScreen(onSignedIn = ::goHome)
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
        composable(Screen.Users.route) {
            UsersScreen(navController)
        }
        composable(Screen.Charts.route) {
            PlaceholderScreen("Charts")
        }
        composable(Screen.Account.route) {
            AccountScreen()
        }
    }
}
