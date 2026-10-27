package com.anjo.statisticsui.ui.login

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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.providerSignIn
import com.anjo.statisticsui.auth.resumeProviderSignIn
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import org.koin.compose.koinInject

/**
 * Password form and/or provider button, as the backend's sign-in config says (the form also shows while it is unknown).
 * Wrong credentials get the server's one generic message; more than 5 tries a minute answer 429.
 */
@Composable
fun LoginScreen(onSignedIn: (User) -> Unit, api: PlatformApi = koinInject()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var config by remember { mutableStateOf<AuthConfig?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        attempt({ error = it }) {
            config = api.authConfig()
            resumeProviderSignIn(api)?.let(onSignedIn)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(LARGE_MEDIUM_PADDING).width(360.dp), verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING)) {
                Text("StatsFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("Sign in", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (config?.local != false) {
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
                }
                config?.oidc?.let { provider ->
                    OutlinedButton(
                        onClick = {
                            scope.attempt({ error = it }) {
                                busy = true
                                try {
                                    providerSignIn(api)?.let(onSignedIn)
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign in with ${provider.name}") }
                }
                ErrorText(error)
            }
        }
    }
}
