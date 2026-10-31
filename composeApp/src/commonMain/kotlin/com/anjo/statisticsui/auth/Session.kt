package com.anjo.statisticsui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anjo.statisticsui.model.ADMIN
import com.anjo.statisticsui.model.EDITOR
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.User
import org.koin.core.annotation.Singleton

@Singleton
class Session {
    var token by mutableStateOf(loadSetting(TOKEN))
        private set
    var user by mutableStateOf<User?>(null)

    // Unsaved Account edits, so theme and colour preview app-wide until Save sends all prefs
    var draftPrefs by mutableStateOf<Prefs?>(null)

    val isAdmin get() = user?.role == ADMIN
    val canEdit get() = user?.role in setOf(ADMIN, EDITOR)

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
        draftPrefs = null
    }
}

private const val TOKEN = "token"
