package com.anjo.statisticsui.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.api.User
import com.anjo.statisticsui.presentation.common.ErrorText
import com.anjo.statisticsui.presentation.common.Field
import com.anjo.statisticsui.presentation.common.attempt
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import org.koin.compose.koinInject

/** Wrong credentials get the server's one generic message; more than 5 tries a minute answer 429. */
@Composable
fun LoginScreen(onSignedIn: (User) -> Unit, api: PlatformApi = koinInject()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(LARGE_MEDIUM_PADDING).width(360.dp), verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING)) {
                Text("StatsFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("Sign in", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Field("Email", email, Modifier.fillMaxWidth()) { email = it }
                Field("Password", password, Modifier.fillMaxWidth(), password = true) { password = it }
                Button(
                    onClick = {
                        scope.attempt({ error = it }) {
                            busy = true
                            try {
                                onSignedIn(api.login(email.trim(), password))
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sign in") }
                ErrorText(error)
            }
        }
    }
}
