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

    // Kept on this device only: server prefs have no field for it
    var accent by mutableStateOf(loadSetting(ACCENT))
        private set

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

    fun pickAccent(accent: String) {
        saveSetting(ACCENT, accent)
        this.accent = accent
    }
}

private const val TOKEN = "token"
private const val ACCENT = "accent"
