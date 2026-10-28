package com.anjo.statisticsui.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenTitle
import com.anjo.statisticsui.ui.common.SectionTitle
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.validation.MIN_PASSWORD_LENGTH

private val UNITS = listOf("metric", "imperial")
private val THEMES = listOf("system", "light", "dark")
private const val NO_HOME = "Home screen"
private val FIELD_WIDTH = 420.dp

@Composable
fun AccountScreen(state: AccountState = rememberAccountState()) {
    LaunchedEffect(Unit) { state.load() }
    val prefs = state.prefs

    Column(
        Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING),
    ) {
        ScreenTitle("Account")
        state.user?.let {
            Text(
                "${it.name.ifBlank { it.email }} · ${it.email} · ${it.role}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle("Preferences")
        Field(
            "Time zone (optional)",
            prefs.timeZone.orEmpty(),
            Modifier.width(FIELD_WIDTH),
            help = "For example Europe/Warsaw. Leave empty to use this device's time zone.",
        ) {
            state.prefs = prefs.copy(timeZone = it.trim().ifBlank { null })
        }
        Picker("Units", prefs.units, UNITS) { state.prefs = prefs.copy(units = it) }
        Picker("Theme", prefs.theme, THEMES) { state.prefs = prefs.copy(theme = it) }
        Picker("Home dashboard", prefs.homeDashboard ?: NO_HOME, listOf(NO_HOME) + state.dashboards, Modifier.width(FIELD_WIDTH)) {
            state.prefs = prefs.copy(homeDashboard = it.takeUnless { home -> home == NO_HOME })
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
    PasswordField("New password", state.newPassword, errors["new"], help = "At least $MIN_PASSWORD_LENGTH characters") {
        state.newPassword = it
    }
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
    help: String? = null,
    onChange: (String) -> Unit,
) {
    Field(label, value, Modifier.width(FIELD_WIDTH), password = true, help = help, error = error, onChange = onChange)
}
