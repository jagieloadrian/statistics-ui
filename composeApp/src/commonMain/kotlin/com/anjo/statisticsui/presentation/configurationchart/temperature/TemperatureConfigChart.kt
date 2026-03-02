package com.anjo.statisticsui.presentation.configurationchart.temperature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.anjo.statisticsui.model.dto.TemperatureDevice
import com.anjo.statisticsui.presentation.configurationchart.common.ConfigAnalyticsHeader
import com.anjo.statisticsui.presentation.configurationchart.common.SummaryBox
import com.anjo.statisticsui.presentation.configurationchart.common.toPrettyString
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Bar_chart
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TemperatureDeviceListScreen(navController: NavHostController,
                                viewModel: TemperatureRunsViewModel = koinViewModel<TemperatureRunsViewModel>()) {
    val itemsData by viewModel.devices.collectAsState()
    val init = remember { mutableStateOf(true) }

    if (init.value) {
        viewModel.fetchDevices()
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
            ConfigAnalyticsHeader("Devices", navController)

            Spacer(Modifier.height(32.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryBox("All", itemsData.data.size.toString(), Color.White, Modifier.weight(1f))
                SummaryBox("First seen", itemsData.data.map {it.firstSeen}.minByOrNull { it }.toPrettyString(), Color(0xFF4CAF50), Modifier.weight(1f))
                SummaryBox("Last seen", itemsData.data.map {it.lastSeen}.maxByOrNull { it }.toPrettyString(), Color(0xFFEF5350), Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Device / ID", style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                        modifier = Modifier.weight(2f))
                Text("First seen", style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                        modifier = Modifier.weight(1.5f))
                Text("Last seen", style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                        modifier = Modifier.weight(1.5f))
                Spacer(Modifier.width(48.dp)) // Miejsce na ikonę akcji
            }

            LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
            ) {
                items(itemsData.data) { device ->
                    DeviceStatRow(device) {
                        // Nawigacja do wykresów konkretnego urządzenia
                        // navController.navigate("charts/${device.deviceId}")
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceStatRow(device: TemperatureDevice, onClick: () -> Unit) {
    Card(
            modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
    ) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(2f)) {
                Text(
                        text = device.deviceId,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                )
            }

            // First Seen
            Text(
                    text = device.firstSeen.formatDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    modifier = Modifier.weight(1.5f)
            )

            // Last Seen
            Text(
                    text = device.lastSeen.formatDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    modifier = Modifier.weight(1.5f)
            )

            Icon(
                    imageVector = MaterialSymbols.Rounded.Bar_chart,
                    contentDescription = "Show charts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun LocalDateTime.formatDate(): String {
    return "$day.${month.number}.${this.year} ${
        this.hour.toString().padStart(2, '0')
    }:${this.minute.toString().padStart(2, '0')}"
}