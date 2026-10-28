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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING

@Composable
fun LoginScreen(
    onSignedIn: (User) -> Unit,
    state: LoginState = rememberLoginState(),
) {
    LaunchedEffect(Unit) { state.load(onSignedIn) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                Modifier.padding(LARGE_MEDIUM_PADDING).width(360.dp),
                verticalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
            ) {
                Text(
                    "StatsFlow",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("Sign in", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (state.showsPasswordForm) {
                    Field("Email", state.email, Modifier.fillMaxWidth()) { state.email = it }
                    Field("Password", state.password, Modifier.fillMaxWidth(), password = true) { state.password = it }
                    Button(
                        onClick = { state.signIn(onSignedIn) },
                        enabled = !state.busy && state.email.isNotBlank() && state.password.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Sign in") }
                }
                state.config?.oidc?.let { provider ->
                    if (state.waitsForProvider) {
                        Text("Waiting for the browser…", color = MaterialTheme.colorScheme.onSurface)
                        OutlinedButton(onClick = state::cancelProviderSignIn, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                    } else {
                        OutlinedButton(
                            onClick = { state.signInWithProvider(onSignedIn) },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Sign in with ${provider.name}") }
                    }
                }
                ErrorText(state.error)
            }
        }
    }
}
