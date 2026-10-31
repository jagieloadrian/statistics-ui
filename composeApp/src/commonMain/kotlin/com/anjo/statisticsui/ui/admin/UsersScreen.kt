package com.anjo.statisticsui.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anjo.statisticsui.model.ROLES
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.common.SectionTitle
import com.anjo.statisticsui.ui.common.screenPadding
import com.anjo.statisticsui.ui.theme.LARGE_SMALL_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.validation.MIN_PASSWORD_LENGTH

private val FORM_WIDTH = 420.dp

@Composable
fun UsersScreen(
    navController: NavHostController,
    state: UsersState = rememberUsersState(),
) {
    LaunchedEffect(Unit) { state.load() }

    Box(Modifier.fillMaxSize()) {
        UsersContent(state, navController)
        SnackbarHost(state.snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun UsersContent(
    state: UsersState,
    navController: NavHostController,
) {
    Column(
        Modifier.fillMaxSize().padding(screenPadding()).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING),
    ) {
        ScreenHeader("Users", navController)
        SectionTitle("Invite")
        Column(Modifier.widthIn(max = FORM_WIDTH), verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            val errors = state.errors
            val field = Modifier.fillMaxWidth()
            Field(
                "Email",
                state.email,
                field,
                keyboardType = KeyboardType.Email,
                error = errors["email"],
                onBlur = { state.shown.leave("email") },
            ) {
                state.email = it
            }
            Field("Name (optional)", state.name, field) { state.name = it }
            Picker("Role", state.role, ROLES) { state.role = it }
            Field(
                "Initial password",
                state.password,
                field,
                password = true,
                help = "At least $MIN_PASSWORD_LENGTH characters",
                error = errors["password"],
                onBlur = { state.shown.leave("password") },
            ) { state.password = it }
            Button(onClick = state::create) { Text("Create user") }
        }
        ErrorText(state.error)

        SectionTitle("Accounts")
        state.listed.forEach { user ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(LARGE_SMALL_PADDING),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING),
                    verticalArrangement = Arrangement.spacedBy(SMALL_PADDING),
                ) {
                    Column(Modifier.weight(1f).widthIn(min = 200.dp)) {
                        Text(user.email, color = MaterialTheme.colorScheme.onSurface)
                        if (user.name.isNotEmpty()) Text(user.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Picker("Role", user.role, ROLES) { state.changeRole(user, it) }
                    OutlinedButton(onClick = { state.delete(user) }) { Text("Delete") }
                }
            }
        }
    }
}
