package com.anjo.statisticsui.auth

import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.User
import io.ktor.http.parseQueryString

data class Callback(
    val code: String?,
    val state: String?,
    val error: String?,
)

fun parseCallback(query: String): Callback? {
    val p = parseQueryString(query.removePrefix("?"))
    if (p["code"] == null && p["error"] == null) return null
    return Callback(p["code"], p["state"], p["error"]?.let { p["error_description"] ?: it })
}

expect suspend fun providerSignIn(api: PlatformApi): User?

expect suspend fun resumeProviderSignIn(api: PlatformApi): User?
