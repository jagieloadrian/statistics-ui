package com.anjo.statisticsui.auth

import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.User
import io.ktor.http.parseQueryString

/** The provider's redirect back to the app: [code] + [state] on success, [error] (its description if given) otherwise. */
data class Callback(
    val code: String?,
    val state: String?,
    val error: String?,
)

/** Null when [query] (with or without the leading `?`) carries neither `code` nor `error`. */
fun parseCallback(query: String): Callback? {
    val p = parseQueryString(query.removePrefix("?"))
    if (p["code"] == null && p["error"] == null) return null
    return Callback(p["code"], p["state"], p["error"]?.let { p["error_description"] ?: it })
}

/** Sends the user to the sign-in provider. Desktop: waits for the browser and returns the user; web: leaves the page, returns null. */
expect suspend fun providerSignIn(api: PlatformApi): User?

/** Web: finishes provider sign-in when the page was opened by the provider's redirect. Null otherwise, and always on desktop. */
expect suspend fun resumeProviderSignIn(api: PlatformApi): User?
