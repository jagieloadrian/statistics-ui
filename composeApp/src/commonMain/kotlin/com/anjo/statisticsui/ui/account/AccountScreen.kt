package com.anjo.statisticsui.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import org.koin.compose.koinInject

private val UNITS = listOf("metric", "imperial")
private val THEMES = listOf("system", "light", "dark")
private const val NO_HOME = "(home screen)"

@Composable
fun AccountScreen(api: PlatformApi = koinInject(), session: Session = koinInject()) {
    val user = session.user
    var prefs by remember(user) { mutableStateOf(user?.prefs ?: Prefs()) }
    var dashboards by remember { mutableStateOf(emptyList<String>()) }
    var prefsInfo by remember { mutableStateOf<String?>(null) }
    var prefsError by remember { mutableStateOf<String?>(null) }
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var passwordInfo by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        attempt({ prefsError = it }) {
            if (session.user == null) api.me()
            dashboards = api.list("dashboards").map { it.id }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)
    ) {
        Text("Account", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        user?.let { Text("${it.name.ifBlank { it.email }} · ${it.email} · ${it.role}", color = Color.Gray) }

        Text("Preferences", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING))
        Field("Time zone, e.g. Europe/Warsaw (empty = this device)", prefs.timeZone.orEmpty(), Modifier.width(420.dp)) {
            prefs = prefs.copy(timeZone = it.trim().ifBlank { null })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Text("Units", color = Color.Gray)
            Picker(prefs.units, UNITS) { prefs = prefs.copy(units = it) }
            Text("Theme", color = Color.Gray)
            Picker(prefs.theme, THEMES) { prefs = prefs.copy(theme = it) }
            Text("Home dashboard", color = Color.Gray)
            Picker(prefs.homeDashboard ?: NO_HOME, listOf(NO_HOME) + dashboards) { prefs = prefs.copy(homeDashboard = it.takeUnless { h -> h == NO_HOME }) }
        }
        Button(onClick = {
            prefsInfo = null
            scope.attempt({ prefsError = it }) { api.putPrefs(prefs); prefsInfo = "Preferences saved" }
        }) { Text("Save preferences") }
        prefsInfo?.let { Text(it, color = Color.Gray) }
        ErrorText(prefsError)

        Text("Change password", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING))
        Field("Current password", current, Modifier.width(420.dp), password = true) { current = it }
        Field("New password (at least 10 characters)", new, Modifier.width(420.dp), password = true) { new = it }
        Field("Repeat new password", repeat, Modifier.width(420.dp), password = true) { repeat = it }
        if (repeat.isNotEmpty() && repeat != new) Text("Passwords differ", color = MaterialTheme.colorScheme.error)
        Button(enabled = current.isNotEmpty() && new.length >= 10 && new == repeat, onClick = {
            passwordInfo = null
            scope.attempt({ passwordError = it }) {
                api.changePassword(current, new)
                current = ""; new = ""; repeat = ""
                passwordInfo = "Password changed; your other sessions were signed out"
            }
        }) { Text("Change password") }
        passwordInfo?.let { Text(it, color = Color.Gray) }
        ErrorText(passwordError)
    }
}
