package com.anjo.statisticsui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.model.User
import org.koin.core.annotation.Singleton

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
