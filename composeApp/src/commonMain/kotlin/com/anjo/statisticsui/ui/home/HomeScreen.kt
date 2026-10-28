package com.anjo.statisticsui.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.DataTypeTile
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.ScreenIcons
import com.anjo.statisticsui.navigation.Screen
import com.anjo.statisticsui.ui.account.AccountScreen
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.common.catching
import com.anjo.statisticsui.ui.dashboard.NewDashboardDialog
import com.anjo.statisticsui.ui.theme.Colors
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.LARGE_PADDING
import com.anjo.statisticsui.ui.theme.LARGE_SMALL_PADDING
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_forward
import com.composables.icons.materialsymbols.rounded.Dashboard
import com.composables.icons.materialsymbols.rounded.Database
import com.composables.icons.materialsymbols.rounded.Group
import com.composables.icons.materialsymbols.rounded.Logout
import com.composables.icons.materialsymbols.rounded.Sensors
import com.composables.icons.materialsymbols.rounded.Star
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MainLayout(
    navController: NavHostController,
    api: PlatformApi = koinInject(),
    session: Session = koinInject(),
) {
    var selectedScreenIcons by remember { mutableStateOf(ScreenIcons.Main) }
    val scope = rememberCoroutineScope()

    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(240.dp),
            ) {
                Spacer(Modifier.height(MEDIUM_PADDING))
                Text(
                    text = "StatsFlow",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = MEDIUM_PADDING, vertical = SMALL_MEDIUM_PADDING),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = SMALL_MEDIUM_PADDING, vertical = SMALL_PADDING),
                )

                ScreenIcons.entries.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.label) },
                        selected = selectedScreenIcons == screen,
                        onClick = { selectedScreenIcons = screen },
                        icon = { Icon(screen.icon, contentDescription = null) },
                        colors =
                            NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            ),
                        modifier = Modifier.padding(horizontal = LARGE_SMALL_PADDING, vertical = SMALL_SMALL_PADDING),
                    )
                }
                Spacer(Modifier.weight(1f))
                session.user?.let {
                    Text(
                        it.email,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = MEDIUM_PADDING),
                    )
                }
                NavigationDrawerItem(
                    label = { Text("Sign out") },
                    selected = false,
                    // Session.signOut runs even when the server is unreachable; NavGraph then shows the login screen.
                    onClick = { scope.launch { catching { api.logout() } } },
                    icon = { Icon(MaterialSymbols.Rounded.Logout, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = LARGE_SMALL_PADDING, vertical = SMALL_PADDING),
                )
            }
        },
        content = {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (selectedScreenIcons) {
                        ScreenIcons.Main -> DashboardContent(navController = navController)
                        ScreenIcons.Charts -> PlaceholderScreen("Charts section")
                        ScreenIcons.Account -> AccountScreen()
                    }
                }
            }
        },
    )
}

@Composable
fun DashboardContent(
    navController: NavHostController,
    api: PlatformApi = koinInject(),
    session: Session = koinInject(),
) {
    var dashboards by remember { mutableStateOf(emptyList<DefinitionSummary>()) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { attempt({ error = it }) { dashboards = api.list("dashboards") } }

    val scope = rememberCoroutineScope()
    val prefs = session.user?.prefs ?: Prefs()
    val primary = MaterialTheme.colorScheme.primary
    val tiles =
        dashboards.sortedByDescending { it.id in prefs.favourites }.map {
            DataTypeTile(
                it.id,
                it.label?.ifBlank { null } ?: it.id,
                MaterialSymbols.Rounded.Dashboard,
                primary,
                Screen.DashboardView.route(it.id),
            )
        }
    // The server enforces roles too; viewers just don't see what would answer 403.
    val adminTiles =
        if (!session.isAdmin) {
            emptyList()
        } else {
            listOf(
                DataTypeTile("sources", "Sources", MaterialSymbols.Rounded.Sensors, Colors.LIGHT_ORANGE, Screen.Sources.route),
                DataTypeTile("datasets", "Datasets", MaterialSymbols.Rounded.Database, Colors.LIGHT_RED, Screen.Datasets.route),
                DataTypeTile("users", "Users", MaterialSymbols.Rounded.Group, Colors.LIGHT_RED, Screen.Users.route),
            )
        }

    Column(modifier = Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = SMALL_PADDING, bottom = LARGE_MEDIUM_PADDING),
        ) {
            Text(
                text = "Choose a dashboard",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
            )
            if (session.canEdit) {
                Button(onClick = { creating = true }) {
                    Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
                    Text("New dashboard")
                }
            }
        }
        ErrorText(error)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
            verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
        ) {
            items(tiles) { tile ->
                val favourite = tile.id in prefs.favourites
                DataTileCard(tile, favourite = favourite, onFavourite = {
                    val favourites = if (favourite) prefs.favourites - tile.id else prefs.favourites + tile.id
                    scope.attempt({ error = it }) { api.putPrefs(prefs.copy(favourites = favourites)) }
                }) {
                    navController.navigate(tile.route)
                }
            }
            items(adminTiles) { tile ->
                DataTileCard(tile) { navController.navigate(tile.route) }
            }
        }
    }

    if (creating) {
        NewDashboardDialog(
            existingIds = dashboards.map { it.id },
            onDismiss = { creating = false },
            onCreated = { id ->
                creating = false
                navController.navigate(Screen.DashboardView.route(id))
            },
        )
    }
}

@Composable
fun DataTileCard(
    tile: DataTypeTile,
    favourite: Boolean? = null,
    onFavourite: () -> Unit = {},
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(SMALL_MEDIUM_PADDING),
        elevation = CardDefaults.cardElevation(defaultElevation = SMALL_SMALL_PADDING),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier =
            Modifier
                .height(180.dp)
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    tile.primaryColor.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                        ),
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(MEDIUM_PADDING),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(LARGE_SMALL_PADDING),
                        color = tile.primaryColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(LARGE_PADDING),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = tile.icon,
                                contentDescription = tile.title,
                                tint = tile.primaryColor,
                                modifier = Modifier.size(LARGE_MEDIUM_PADDING),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // ponytail: outlined star tinted when favourite; the filled-symbols artifact isn't a dependency
                    favourite?.let {
                        IconButton(onClick = onFavourite) {
                            Icon(
                                MaterialSymbols.Rounded.Star,
                                contentDescription = if (it) "Unfavourite" else "Favourite",
                                tint = if (it) Colors.LIGHT_ORANGE else Color.Gray.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Icon(
                        imageVector = MaterialSymbols.Rounded.Arrow_forward,
                        contentDescription = "Go",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}
