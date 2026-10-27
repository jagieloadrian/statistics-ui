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
data class User(
    val id: Long,
    val email: String,
    val name: String = "",
    val role: String,
    val prefs: Prefs = Prefs(),
    val hasPassword: Boolean = true,
)

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

/** Enabled sign-in methods; [oidc] is null when the backend has no sign-in provider. */
@Serializable
data class AuthConfig(val local: Boolean = true, val oidc: OidcInfo? = null)

@Serializable
data class OidcInfo(val name: String)

@Serializable
data class OidcStartRequest(val redirectUri: String)

@Serializable
data class OidcStart(val authorizationUrl: String, val state: String, val codeVerifier: String)

@Serializable
data class OidcLoginRequest(val code: String, val codeVerifier: String, val redirectUri: String)
