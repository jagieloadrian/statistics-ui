package com.anjo.statisticsui.presentation.configurationchart.epidemic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Download
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EpidemicConfigChart(navController: NavHostController,
                        viewModel: EpidemicRunsViewModel = koinViewModel<EpidemicRunsViewModel>()) {

    val itemsData by viewModel.runs.collectAsState()
    val init = remember { mutableStateOf(true) }

    if (init.value) {
        viewModel.fetchRuns()
        init.value = false
    }

    Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(MaterialSymbols.Rounded.Arrow_back, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                                text = "Runs",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                        )
                        Text(
                                text = "Activity time analysis and connection history",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                        )
                    }
                }

                OutlinedButton(
                        onClick = { /* Export danych */ },
                        shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(MaterialSymbols.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export CSV")
                }
            }

            LazyColumn(modifier = Modifier.padding(padding)) {
                items(itemsData.data) {
                    Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}