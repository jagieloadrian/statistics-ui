package com.anjo.statisticsui.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.auth.providerSignIn
import com.anjo.statisticsui.auth.resumeProviderSignIn
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.common.ShownErrors
import com.anjo.statisticsui.ui.common.attempt
import com.anjo.statisticsui.ui.common.reporting
import com.anjo.statisticsui.validation.FieldErrors
import com.anjo.statisticsui.validation.loginErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.koin.compose.koinInject

class LoginState(
    private val api: PlatformApi,
    private val scope: CoroutineScope,
) {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var config by mutableStateOf<AuthConfig?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var providerJob by mutableStateOf<Job?>(null)
    val shown = ShownErrors()

    val errors: FieldErrors get() = shown.of(loginErrors(email, password))

    val showsPasswordForm get() = config?.local != false
    val waitsForProvider get() = providerJob != null

    suspend fun load(onSignedIn: (User) -> Unit) =
        reporting({ error = it }) {
            config = api.authConfig()
            resumeProviderSignIn(api)?.let(onSignedIn)
        }

    fun signIn(onSignedIn: (User) -> Unit) {
        shown.submit()
        if (errors.isNotEmpty()) return
        scope.attempt({ error = it }) {
            busy = true
            try {
                onSignedIn(api.login(email.trim(), password))
            } finally {
                busy = false
            }
        }
    }

    fun signInWithProvider(onSignedIn: (User) -> Unit) {
        val job = scope.attempt({ error = it }) { providerSignIn(api)?.let(onSignedIn) }
        providerJob = job
        job.invokeOnCompletion { providerJob = null }
    }

    fun cancelProviderSignIn() {
        providerJob?.cancel()
    }
}

@Composable
fun rememberLoginState(api: PlatformApi = koinInject()): LoginState {
    val scope = rememberCoroutineScope()
    return remember { LoginState(api, scope) }
}
