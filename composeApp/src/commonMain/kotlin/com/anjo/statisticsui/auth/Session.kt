package com.anjo.statisticsui.auth

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.api.Prefs
import com.anjo.statisticsui.api.User
import org.koin.core.annotation.Singleton

/** Bearer token persisted across restarts (desktop: java.util.prefs, web: localStorage); null removes it. */
expect fun loadToken(): String?

expect fun saveToken(token: String?)

/** Signed-in state; Compose state so screens react to sign-in, sign-out and changed preferences. */
@Singleton
class Session {
    var token by mutableStateOf(loadToken())
        private set
    var user by mutableStateOf<User?>(null)

    val isAdmin get() = user?.role == "admin"
    val canEdit get() = user?.role == "admin" || user?.role == "editor"

    fun signIn(token: String, user: User) {
        saveToken(token)
        this.token = token
        this.user = user
    }

    fun signOut() {
        saveToken(null)
        token = null
        user = null
    }
}

/** The signed-in user's preferences (zone, units, theme) for every widget renderer. */
val LocalPrefs = compositionLocalOf { Prefs() }
