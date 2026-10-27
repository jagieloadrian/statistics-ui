package com.anjo.statisticsui.presentation.admin

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.anjo.statisticsui.api.NewUser
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.api.User
import com.anjo.statisticsui.api.UserUpdate
import com.anjo.statisticsui.presentation.common.ConfirmButton
import com.anjo.statisticsui.presentation.common.ErrorText
import com.anjo.statisticsui.presentation.common.Field
import com.anjo.statisticsui.presentation.common.Picker
import com.anjo.statisticsui.presentation.common.ScreenHeader
import com.anjo.statisticsui.presentation.common.attempt
import com.anjo.statisticsui.ui.theme.LARGE_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_MEDIUM_PADDING
import com.anjo.statisticsui.ui.theme.SMALL_PADDING
import org.koin.compose.koinInject

private val ROLES = listOf("viewer", "editor", "admin")

/** Admin only: invite with an initial password (no email is sent), change role, delete; the last admin is kept (409). */
@Composable
fun UsersScreen(navController: NavHostController, api: PlatformApi = koinInject()) {
    var users by remember { mutableStateOf(emptyList<User>()) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("viewer") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { attempt({ error = it }) { users = api.users() } }

    Column(
        Modifier.fillMaxSize().padding(LARGE_MEDIUM_PADDING).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SMALL_PADDING)
    ) {
        ScreenHeader("Users", navController)
        Text("Invite", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
            Field("Email", email, Modifier.weight(1f)) { email = it }
            Field("Name", name, Modifier.weight(1f)) { name = it }
            Picker(role, ROLES) { role = it }
            Field("Initial password (≥ 10 characters)", password, Modifier.weight(1f), password = true) { password = it }
            Button(enabled = email.isNotBlank() && password.length >= 10, onClick = {
                scope.attempt({ error = it }) {
                    api.createUser(NewUser(email.trim(), name.trim(), role, password))
                    email = ""; name = ""; password = ""
                    users = api.users()
                }
            }) { Text("Create") }
        }
        ErrorText(error)

        Text("Accounts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = SMALL_MEDIUM_PADDING))
        users.forEach { u ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SMALL_PADDING)) {
                Text(u.email, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Text(u.name, Modifier.weight(1f), color = Color.Gray)
                Picker(u.role, ROLES) { r -> scope.attempt({ error = it }) { api.updateUser(u.id, UserUpdate(role = r)); users = api.users() } }
                ConfirmButton("Delete", "Click again to delete ${u.email}") {
                    scope.attempt({ error = it }) { api.deleteUser(u.id); users = api.users() }
                }
            }
        }
    }
}
