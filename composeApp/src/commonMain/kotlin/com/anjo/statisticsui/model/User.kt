package com.anjo.statisticsui.model

import kotlinx.serialization.Serializable

@Serializable
data class Prefs(
    val timeZone: String? = null,
    val units: String = "metric",
    val theme: String = "system",
    val homeDashboard: String? = null,
    val favourites: List<String> = emptyList(),
)

@Serializable
data class User(val id: Long, val email: String, val name: String = "", val role: String, val prefs: Prefs = Prefs())

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: User)

@Serializable
data class PasswordChange(val current: String, val new: String)

@Serializable
data class NewUser(val email: String, val name: String = "", val role: String, val password: String)

@Serializable
data class UserUpdate(val name: String? = null, val role: String? = null)
