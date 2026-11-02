package com.anjo.statisticsui.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.common.ShownErrors
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.common.reporting
import com.anjo.statisticsui.validation.FieldErrors
import com.anjo.statisticsui.validation.passwordChangeErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class AccountState(
    private val api: PlatformApi,
    private val session: Session,
    private val scope: CoroutineScope,
) {
    var prefs: Prefs
        get() = session.draftPrefs ?: session.user?.prefs ?: Prefs()
        set(value) {
            session.draftPrefs = value
        }
    var dashboards by mutableStateOf(emptyList<String>())
        private set
    var prefsInfo by mutableStateOf<String?>(null)
        private set
    var prefsError by mutableStateOf<String?>(null)
        private set
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var repeatedPassword by mutableStateOf("")
    var passwordInfo by mutableStateOf<String?>(null)
        private set
    var passwordError by mutableStateOf<String?>(null)
        private set
    private var localSignIn by mutableStateOf(true)
    val passwordShown = ShownErrors()

    val passwordErrors: FieldErrors
        get() = passwordShown.of(passwordChangeErrors(currentPassword, newPassword, repeatedPassword))

    val user: User? get() = session.user
    val canChangePassword get() = user?.hasPassword != false && localSignIn

    suspend fun load() =
        reporting({ prefsError = it }) {
            if (session.user == null) api.me()
            dashboards = api.list("dashboards").map { it.id }
            localSignIn = api.authConfig().local
        }

    fun signOut() = scope.launch { api.logout() }

    fun savePrefs() {
        prefsInfo = null
        scope.attempt({ prefsError = it }) {
            api.putPrefs(prefs)
            session.draftPrefs = null
            prefsInfo = "Preferences saved"
        }
    }

    // Leaving Account without Save drops the edits, and with them the theme and colour preview
    fun discardPrefs() {
        session.draftPrefs = null
    }

    fun changePassword() {
        passwordInfo = null
        passwordShown.submit()
        if (passwordErrors.isNotEmpty()) return
        scope.attempt({ passwordError = it }) {
            api.changePassword(currentPassword, newPassword)
            passwordShown.reset()
            currentPassword = ""
            newPassword = ""
            repeatedPassword = ""
            passwordInfo = "Password changed; your other sessions were signed out"
        }
    }
}

@Composable
fun rememberAccountState(
    api: PlatformApi = koinInject(),
    session: Session = koinInject(),
): AccountState {
    val scope = rememberCoroutineScope()
    return remember { AccountState(api, session, scope) }
}
