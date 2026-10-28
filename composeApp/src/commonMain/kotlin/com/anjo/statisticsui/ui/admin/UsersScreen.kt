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
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.anjo.statisticsui.ui.common.ConfirmButton
import com.anjo.statisticsui.ui.common.ErrorText
import com.anjo.statisticsui.ui.common.Field
import com.anjo.statisticsui.ui.common.Picker
import com.anjo.statisticsui.ui.common.ScreenHeader
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING

private val ROLES = listOf("viewer", "editor", "admin")

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
        Text("Invite", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING),
        ) {
            Field("Email", state.email, Modifier.weight(1f)) { state.email = it }
            Field("Name", state.name, Modifier.weight(1f)) { state.name = it }
            Picker(state.role, ROLES) { state.role = it }
            Field("Initial password (≥ 10 characters)", state.password, Modifier.weight(1f), password = true) { state.password = it }
            Button(enabled = state.email.isNotBlank() && state.password.length >= 10, onClick = state::create) { Text("Create") }
        }
        ErrorText(state.error)

        Text(
            "Accounts",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING),
        )
        state.users.forEach { user ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING),
            ) {
                Text(user.email, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Text(user.name, Modifier.weight(1f), color = Color.Gray)
                Picker(user.role, ROLES) { state.changeRole(user, it) }
                ConfirmButton("Delete", "Click again to delete ${user.email}") { state.delete(user) }
            }
        }
    }
}
