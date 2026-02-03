package com.anjo.statisticsui.presentation.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.model.DataTypeTile
import com.anjo.statisticsui.model.ScreenIcons
import com.anjo.statisticsui.navigation.Screen
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.LARGE_PADDING
import com.anjo.statisticsui.ui.theme.LARGE_SMALL_PADDING
import com.anjo.statisticsui.ui.theme.MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_SMALL_PADDING
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_forward
import com.composables.icons.materialsymbols.rounded.Coronavirus
import com.composables.icons.materialsymbols.rounded.Thermostat


@Composable
fun MainLayout(navController: NavHostController) {
    var selectedScreenIcons by remember { mutableStateOf(ScreenIcons.Main) }

    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(240.dp)
            ) {
                Spacer(Modifier.height(MEDIUM_PADDING))
                Text(
                    text = "StatsFlow",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = MEDIUM_PADDING, vertical = SMALL_MEDIUM_PADDING)
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = SMALL_MEDIUM_PADDING, vertical = SMALL_PADDING)
                )

                ScreenIcons.entries.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.label) },
                        selected = selectedScreenIcons == screen,
                        onClick = { selectedScreenIcons = screen },
                        icon = { Icon(screen.icon, contentDescription = null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(horizontal = LARGE_SMALL_PADDING, vertical = SMALL_SMALL_PADDING)
                    )
                }
            }
        },
        content = {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (selectedScreenIcons) {
                        ScreenIcons.Main -> DashboardContent(navController = navController)
                        ScreenIcons.Charts -> PlaceholderScreen("Charts section")
                        ScreenIcons.Account -> PlaceholderScreen("Account settings")
                    }
                }
            }
        }
    )
}

@Composable
fun DashboardContent(navController: NavHostController) {
    val tiles = listOf(
        DataTypeTile(
            id = "temp",
            title = "Temperature",
            icon = MaterialSymbols.Rounded.Thermostat,
            primaryColor = Color(0xFFFF7043),
            secondaryColor = Color(0xFFFFAB91),
            route = Screen.TemperatureDetails.route,
        ),
        DataTypeTile(
            id = "epidemic",
            title = "Epidemic",
            icon = MaterialSymbols.Rounded.Coronavirus,
            primaryColor = Color(0xFFEF5350),
            secondaryColor = Color(0xFFE57373),
            route = Screen.EpidemicDetails.route,
        )
    )

    Column(modifier = Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING)) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose category to analyze",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            modifier = Modifier.padding(top = SMALL_PADDING, bottom = LARGE_MEDIUM_PADDING)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
            verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING)
        ) {
            items(tiles) { tile ->
                DataTileCard(tile) {
                    navController.navigate(tile.route)
                }
            }
        }
    }
}

@Composable
fun DataTileCard(tile: DataTypeTile, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(SMALL_MEDIUM_PADDING),
        elevation = CardDefaults.cardElevation(defaultElevation = SMALL_SMALL_PADDING),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .height(180.dp)
            .clickable {
                println("Clicked: ${tile.title}")
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tile.primaryColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MEDIUM_PADDING),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(LARGE_SMALL_PADDING),
                    color = tile.primaryColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(LARGE_PADDING)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = tile.title,
                            tint = tile.primaryColor,
                            modifier = Modifier.size(LARGE_MEDIUM_PADDING)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Icon(
                        imageVector = MaterialSymbols.Rounded.Arrow_forward,
                        contentDescription = "Go",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
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
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}