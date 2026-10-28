package com.anjo.statisticsui.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import com.anjo.statisticsui.ui.common.ConfirmButton
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.common.SectionTitle
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import com.anjo.statisticsui.validation.MIN_PASSWORD_LENGTH
import com.anjo.statisticsui.validation.ROLES

@Composable
fun UsersScreen(
    navController: NavHostController,
    state: UsersState = rememberUsersState(),
) {
    LaunchedEffect(Unit) { state.load() }

    Column(
        Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING),
    ) {
        ScreenHeader("Users", navController)
        SectionTitle("Invite")
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING),
        ) {
            val errors = state.errors
            Field(
                "Email",
                state.email,
                Modifier.weight(1f),
                keyboardType = KeyboardType.Email,
                error = errors["email"],
            ) { state.email = it }
            Field("Name", state.name, Modifier.weight(1f)) { state.name = it }
            Picker("Role", state.role, ROLES) { state.role = it }
            Field(
                "Initial password",
                state.password,
                Modifier.weight(1f),
                password = true,
                help = "At least $MIN_PASSWORD_LENGTH characters",
                error = errors["password"],
            ) { state.password = it }
            Button(onClick = state::create) { Text("Create user") }
        }
        ErrorText(state.error)

        SectionTitle("Accounts")
        state.users.forEach { user ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING),
            ) {
                Text(user.email, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Text(user.name, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Picker("Role", user.role, ROLES) { state.changeRole(user, it) }
                ConfirmButton("Delete", "Click again to delete ${user.email}") { state.delete(user) }
            }
        }
    }
}
