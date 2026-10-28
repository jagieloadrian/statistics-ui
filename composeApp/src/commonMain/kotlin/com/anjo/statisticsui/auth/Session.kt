package com.anjo.statisticsui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.model.User
import org.koin.core.annotation.Singleton

@Singleton
class Session {
    var token by mutableStateOf(loadSetting(TOKEN))
        private set
    var user by mutableStateOf<User?>(null)

    val isAdmin get() = user?.role == "admin"
    val canEdit get() = user?.role == "admin" || user?.role == "editor"

    fun signIn(
        token: String,
        user: User,
    ) {
        saveSetting(TOKEN, token)
        this.token = token
        this.user = user
    }

    fun signOut() {
        saveSetting(TOKEN, null)
        token = null
        user = null
    }
}

private const val TOKEN = "token"
