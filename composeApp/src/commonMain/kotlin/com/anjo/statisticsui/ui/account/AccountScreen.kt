package com.anjo.statisticsui.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenTitle
import com.anjo.statisticsui.ui.common.SectionTitle
import com.anjo.statisticsui.ui.common.ShownErrors
import com.anjo.statisticsui.ui.common.screenPadding
import com.anjo.statisticsui.ui.theme.ACCENTS
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.validation.MIN_PASSWORD_LENGTH
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Check
import com.composables.icons.materialsymbols.rounded.Logout

private val UNITS = listOf("metric", "imperial")
private val THEMES = listOf("system", "light", "dark")
private const val NO_HOME = "Home screen"
private val FIELD_WIDTH = 420.dp
private val SWATCH_SIZE = 48.dp

@Composable
fun AccountScreen(state: AccountState = rememberAccountState()) {
    LaunchedEffect(Unit) { state.load() }
    DisposableEffect(Unit) { onDispose(state::discardPrefs) }
    val prefs = state.prefs

    Column(
        Modifier.fillMaxSize().padding(screenPadding()).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING),
    ) {
        ScreenTitle("Account")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SMALL_MEDIUM_PADDING),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            state.user?.let {
                Text(
                    "${it.name.ifBlank { it.email }} · ${it.email} · ${it.role}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = state::signOut) {
                Icon(MaterialSymbols.Rounded.Logout, contentDescription = null)
                Text("Sign out")
            }
        }

        SectionTitle("Preferences")
        Field(
            "Time zone (optional)",
            prefs.timeZone.orEmpty(),
            Modifier.widthIn(max = FIELD_WIDTH).fillMaxWidth(),
            help = "For example Europe/Warsaw. Leave empty to use this device's time zone.",
        ) {
            state.prefs = prefs.copy(timeZone = it.trim().ifBlank { null })
        }
        Picker("Units", prefs.units, UNITS) { state.prefs = prefs.copy(units = it) }
        Picker("Theme", prefs.theme, THEMES) { state.prefs = prefs.copy(theme = it) }
        AccentPicker(prefs.accent) { state.prefs = prefs.copy(accent = it) }
        Picker(
            "Home dashboard",
            prefs.homeDashboard ?: NO_HOME,
            listOf(NO_HOME) + state.dashboards,
            Modifier.widthIn(max = FIELD_WIDTH).fillMaxWidth(),
        ) {
            state.prefs = prefs.copy(homeDashboard = it.takeUnless { home -> home == NO_HOME })
        }
        Button(onClick = state::savePrefs) { Text("Save preferences") }
        state.prefsInfo?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        ErrorText(state.prefsError)

        if (state.canChangePassword) PasswordForm(state)
    }
}

@Composable
private fun AccentPicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text("Colour: ${accentLabel(selected)}", color = MaterialTheme.colorScheme.onSurface)
    FlowRow(
        Modifier.widthIn(max = FIELD_WIDTH).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING),
    ) {
        ACCENTS.forEach { (name, seed) ->
            Box(
                Modifier
                    .size(SWATCH_SIZE)
                    .clip(CircleShape)
                    .background(seed)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .selectable(selected = name == selected, role = Role.RadioButton) { onSelect(name) }
                    .semantics { contentDescription = accentLabel(name) },
                contentAlignment = Alignment.Center,
            ) {
                if (name == selected) {
                    Icon(
                        MaterialSymbols.Rounded.Check,
                        contentDescription = null,
                        tint =
                            if (seed.luminance() >
                                0.4f
                            ) {
                                Color.Black
                            } else {
                                Color.White
                            },
                    )
                }
            }
        }
    }
    Text(
        "Theme and colour preview right away; Save preferences keeps them",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun accentLabel(id: String) = id.replace('-', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun PasswordForm(state: AccountState) {
    val errors = state.passwordErrors
    SectionTitle("Change password")
    PasswordField(
        "Current password",
        state.currentPassword,
        errors["current"],
        state.passwordShown,
        "current",
    ) { state.currentPassword = it }
    PasswordField(
        "New password",
        state.newPassword,
        errors["new"],
        state.passwordShown,
        "new",
        help = "At least $MIN_PASSWORD_LENGTH characters",
    ) {
        state.newPassword = it
    }
    PasswordField("Repeat new password", state.repeatedPassword, errors["repeated"], state.passwordShown, "repeated") {
        state.repeatedPassword = it
    }
    Button(onClick = state::changePassword) { Text("Change password") }
    state.passwordInfo?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ErrorText(state.passwordError)
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    error: String?,
    shown: ShownErrors,
    key: String,
    help: String? = null,
    onChange: (String) -> Unit,
) {
    Field(
        label,
        value,
        Modifier.widthIn(max = FIELD_WIDTH).fillMaxWidth(),
        password = true,
        help = help,
        error = error,
        onBlur = { shown.leave(key) },
        onChange = onChange,
    )
}
