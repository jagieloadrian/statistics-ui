package com.anjo.statisticsui.presentation.configurationchart.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Download
import kotlinx.datetime.LocalDateTime

@Composable
fun ConfigAnalyticsHeader(name:String, navController: NavHostController) {
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
                        text = name,
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
}

@Composable
fun SummaryBox(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
            modifier = modifier,
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Black)
        }
    }
}


fun LocalDateTime?.toPrettyString(): String {
    return this?.let { "${this.year} ${this.month.name.take(3)} ${this.day} ${this.hour.toString().padStart(2, '0')}:" +
            "${this.minute.toString().padStart(2, '0')}:${this.second.toString().padStart(2, '0')}" }
        ?: "null"
}