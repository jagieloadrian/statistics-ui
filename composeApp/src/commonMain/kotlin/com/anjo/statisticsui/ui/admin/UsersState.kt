package com.anjo.statisticsui.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.NewUser
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.UserUpdate
import com.anjo.statisticsui.ui.common.ShownErrors
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.validation.FieldErrors
import com.anjo.statisticsui.validation.newUserErrors
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject

class UsersState(
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var users by mutableStateOf(emptyList<User>())
        private set
    var email by mutableStateOf("")
    var name by mutableStateOf("")
    var role by mutableStateOf("viewer")
    var password by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
        private set
    val shown = ShownErrors()

    val errors: FieldErrors get() = shown.of(newUserErrors(email, role, password))

    fun load() = scope.attempt({ error = it }) { users = api.users() }

    fun create() {
        shown.submit()
        if (errors.isNotEmpty()) return
        scope.attempt({ error = it }) {
            api.createUser(NewUser(email.trim(), name.trim(), role, password))
            shown.reset()
            email = ""
            name = ""
            password = ""
            users = api.users()
        }
    }

    fun changeRole(
        user: User,
        newRole: String,
    ) = scope.attempt({ error = it }) {
        api.updateUser(user.id, UserUpdate(role = newRole))
        users = api.users()
    }

    fun delete(user: User) =
        scope.attempt({ error = it }) {
            api.deleteUser(user.id)
            users = api.users()
        }
}

@Composable
fun rememberUsersState(api: PlatformApi = koinInject()): UsersState {
    val scope = rememberCoroutineScope()
    return remember { UsersState(api, scope) }
}
