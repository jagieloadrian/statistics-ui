package com.anjo.statisticsui.presentation.configurationchart.temperature

import androidx.compose.runtime.Composable

//@Composable
//fun TemperatureConfigChart() {
//}

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.composables.icons.materialicons.MaterialIcons
import com.composables.icons.materialicons.rounded.Arrow_back
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Battery_alert
import com.composables.icons.materialsymbols.rounded.Battery_full
import com.composables.icons.materialsymbols.rounded.Bluetooth_connected
import com.composables.icons.materialsymbols.rounded.Bluetooth_disabled
import com.composables.icons.materialsymbols.rounded.Chevron_right

// --- Modele Danych ---

data class Device(
    val id: String,
    val name: String,
    val location: String,
    val currentTemp: Double,
    val isOnline: Boolean,
    val batteryLevel: Int
)

// --- Mock Danych ---

val mockDevices = listOf(
    Device("1", "Czujnik Salon", "Parter", 22.5, true, 85),
    Device("2", "Czujnik Kuchnia", "Parter", 24.1, true, 42),
    Device("3", "Sypialnia Główna", "Piętro 1", 20.8, true, 92),
    Device("4", "Czujnik Zewnętrzny", "Ogród", -2.4, false, 10),
    Device("5", "Garaż", "Parter", 15.0, true, 64)
)

// --- Ekran Listy Urządzeń ---

@Composable
fun TemperatureDeviceListScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        // Nagłówek
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(onClick = { println("Click back!"); navController.popBackStack() }) {
                    Icon( MaterialIcons.Rounded.Arrow_back, contentDescription = "Wstecz", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Czujniki Temperatury",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Zarządzaj dostępnymi urządzeniami",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // Przycisk dodawania urządzenia
            Button(
                onClick = { /* Akcja dodawania */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Dodaj urządzenie")
            }
        }

        Spacer(Modifier.height(32.dp))

        // Statystyki ogólne (Małe info-boxy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoSummaryCard("Aktywne", "4", Color(0xFF4CAF50), Modifier.weight(1f))
            InfoSummaryCard("Średnia", "15.9°C", Color(0xFF29B6F6), Modifier.weight(1f))
            InfoSummaryCard("Niski prąd", "1", Color(0xFFFFA000), Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        // Lista urządzeń
        Text(
            text = "Urządzenia w pobliżu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mockDevices) { device ->
                DeviceItemRow(device) {
                    // Tutaj można nawigować do jeszcze bardziej szczegółowego wykresu
                    // navController.navigate("device_details/${device.id}")
                }
            }
        }
    }
}

@Composable
fun DeviceItemRow(device: Device, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status ikona
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (device.isOnline) Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else Color.Gray.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (device.isOnline) MaterialSymbols.Rounded.Bluetooth_connected else MaterialSymbols.Rounded.Bluetooth_disabled,
                        contentDescription = null,
                        tint = if (device.isOnline) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Nazwa i lokalizacja
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = device.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Dane urządzenia
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bateria
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = if (device.batteryLevel > 20) MaterialSymbols.Rounded.Battery_full else  MaterialSymbols.Rounded.Battery_alert,
                        contentDescription = null,
                        tint = if (device.batteryLevel > 20) Color.Gray else Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${device.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                // Temperatura
                Text(
                    text = "${device.currentTemp}°C",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (device.currentTemp > 20) Color(0xFFFF7043) else Color(0xFF29B6F6)
                )

                Spacer(Modifier.width(16.dp))

                Icon(MaterialSymbols.Rounded.Chevron_right, contentDescription = null, tint = Color.DarkGray)
            }
        }
    }
}

@Composable
fun InfoSummaryCard(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}