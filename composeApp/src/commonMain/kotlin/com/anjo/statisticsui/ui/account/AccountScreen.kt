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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.validation.MIN_PASSWORD_LENGTH

private val UNITS = listOf("metric", "imperial")
private val THEMES = listOf("system", "light", "dark")
private const val NO_HOME = "(home screen)"
private val FIELD_WIDTH = 420.dp

@Composable
fun AccountScreen(state: AccountState = rememberAccountState()) {
    LaunchedEffect(Unit) { state.load() }
    val prefs = state.prefs

    Column(
        Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING),
    ) {
        Text(
            "Account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        state.user?.let {
            Text(
                "${it.name.ifBlank { it.email }} · ${it.email} · ${it.role}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle("Preferences")
        Field("Time zone, e.g. Europe/Warsaw (empty = this device)", prefs.timeZone.orEmpty(), Modifier.width(FIELD_WIDTH)) {
            state.prefs = prefs.copy(timeZone = it.trim().ifBlank { null })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Text("Units", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Picker(prefs.units, UNITS) { state.prefs = prefs.copy(units = it) }
            Text("Theme", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Picker(prefs.theme, THEMES) { state.prefs = prefs.copy(theme = it) }
            Text("Home dashboard", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Picker(prefs.homeDashboard ?: NO_HOME, listOf(NO_HOME) + state.dashboards) {
                state.prefs = prefs.copy(homeDashboard = it.takeUnless { home -> home == NO_HOME })
            }
        }
        Button(onClick = state::savePrefs) { Text("Save preferences") }
        state.prefsInfo?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        ErrorText(state.prefsError)

        if (state.canChangePassword) PasswordForm(state)
    }
}

@Composable
private fun PasswordForm(state: AccountState) {
    val errors = state.passwordErrors
    SectionTitle("Change password")
    PasswordField("Current password", state.currentPassword, errors["current"]) { state.currentPassword = it }
    PasswordField("New password (at least $MIN_PASSWORD_LENGTH characters)", state.newPassword, errors["new"]) { state.newPassword = it }
    PasswordField("Repeat new password", state.repeatedPassword, errors["repeated"]) { state.repeatedPassword = it }
    Button(onClick = state::changePassword) { Text("Change password") }
    state.passwordInfo?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ErrorText(state.passwordError)
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    error: String?,
    onChange: (String) -> Unit,
) {
    Field(label, value, Modifier.width(FIELD_WIDTH), password = true, error = error, onChange = onChange)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING),
    )
}
