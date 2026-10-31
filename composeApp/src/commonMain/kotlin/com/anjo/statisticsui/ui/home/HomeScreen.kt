package com.anjo.statisticsui.ui.home

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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.model.DataTypeTile
import com.anjo.statisticsui.model.ScreenIcons
import com.anjo.statisticsui.navigation.Screen
import com.anjo.statisticsui.ui.account.AccountScreen
import com.anjo.statisticsui.ui.common.EXPANDED_MIN_WIDTH
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.ScreenTitle
import com.anjo.statisticsui.ui.common.SectionTitle
import com.anjo.statisticsui.ui.common.screenPadding
import com.anjo.statisticsui.ui.common.windowWidth
import com.anjo.statisticsui.ui.dashboard.NewDashboardDialog
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
import com.composables.icons.materialsymbols.rounded.Bookmark_add
import com.composables.icons.materialsymbols.rounded.Bookmark_added
import com.composables.icons.materialsymbols.rounded.Dashboard
import com.composables.icons.materialsymbols.rounded.Database
import com.composables.icons.materialsymbols.rounded.Group
import com.composables.icons.materialsymbols.rounded.Sensors

private val ADMIN_TILES =
    listOf(
        DataTypeTile("sources", "Sources", MaterialSymbols.Rounded.Sensors, Screen.Sources.route),
        DataTypeTile("datasets", "Datasets", MaterialSymbols.Rounded.Database, Screen.Datasets.route),
        DataTypeTile("users", "Users", MaterialSymbols.Rounded.Group, Screen.Users.route),
    )

private val FULL_LINE: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

@Composable
fun MainLayout(
    navController: NavHostController,
    state: HomeState = rememberHomeState(),
) {
    var selected by remember { mutableStateOf(ScreenIcons.Main) }
    val content: @Composable () -> Unit = {
        when (selected) {
            ScreenIcons.Main -> DashboardContent(navController = navController, state = state)
            ScreenIcons.Account -> AccountScreen()
        }
    }

    if (windowWidth() < EXPANDED_MIN_WIDTH) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    ScreenIcons.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = selected == screen,
                            onClick = { selected = screen },
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                        )
                    }
                }
            },
        ) { padding -> Box(Modifier.padding(padding)) { content() } }
        return
    }

    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                Spacer(Modifier.height(MEDIUM_PADDING))
                Text(
                    text = "StatsFlow",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = MEDIUM_PADDING, vertical = SMALL_MEDIUM_PADDING),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = SMALL_MEDIUM_PADDING, vertical = SMALL_PADDING))
                ScreenIcons.entries.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.label) },
                        selected = selected == screen,
                        onClick = { selected = screen },
                        icon = { Icon(screen.icon, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = LARGE_SMALL_PADDING, vertical = SMALL_SMALL_PADDING),
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
fun DashboardContent(
    navController: NavHostController,
    state: HomeState = rememberHomeState(),
) {
    LaunchedEffect(Unit) { state.load() }
    val tiles =
        state.sortedDashboards.map {
            DataTypeTile(
                it.id,
                it.label?.ifBlank { null } ?: it.id,
                MaterialSymbols.Rounded.Dashboard,
                Screen.DashboardView.route(it.id),
            )
        }

    Column(modifier = Modifier.fillMaxSize().padding(screenPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = MEDIUM_PADDING)) {
            ScreenTitle("Dashboards", Modifier.weight(1f))
            if (state.canCreate) {
                Button(onClick = { state.creating = true }) {
                    Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
                    Text("New dashboard")
                }
            }
        }
        ErrorText(state.error)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
            verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
        ) {
            if (state.loading) {
                item(span = FULL_LINE) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (tiles.isEmpty() && state.error == null) {
                item(span = FULL_LINE) {
                    Text(
                        if (state.canCreate) {
                            "No dashboards yet. Create the first one with New dashboard."
                        } else {
                            "No dashboards yet. Ask an editor to share one with you."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(tiles) { tile ->
                DataTileCard(tile, favourite = tile.id in state.favourites, onFavourite = { state.toggleFavourite(tile.id) }) {
                    navController.navigate(tile.route)
                }
            }
            if (state.isAdmin) {
                item(span = FULL_LINE) { SectionTitle("Administration") }
                items(ADMIN_TILES) { tile ->
                    DataTileCard(tile) { navController.navigate(tile.route) }
                }
            }
        }
    }

    if (state.creating) {
        NewDashboardDialog(
            existingIds = state.dashboards.map { it.id },
            onDismiss = { state.creating = false },
            onCreated = { id ->
                state.creating = false
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
        onClick = onClick,
        shape = RoundedCornerShape(SMALL_MEDIUM_PADDING),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MEDIUM_PADDING),
            verticalArrangement = Arrangement.spacedBy(MEDIUM_PADDING),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(LARGE_SMALL_PADDING),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(LARGE_PADDING),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(LARGE_MEDIUM_PADDING),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                favourite?.let {
                    IconToggleButton(checked = it, onCheckedChange = { onFavourite() }) {
                        Icon(
                            if (it) MaterialSymbols.Rounded.Bookmark_added else MaterialSymbols.Rounded.Bookmark_add,
                            contentDescription = "Favourite",
                            tint = if (it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MaterialSymbols.Rounded.Arrow_forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
