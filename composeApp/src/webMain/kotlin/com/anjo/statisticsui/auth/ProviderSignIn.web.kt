package com.anjo.statisticsui.auth

import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.User
import kotlinx.browser.sessionStorage
import kotlinx.browser.window

private const val STATE = "oidc.state"
private const val VERIFIER = "oidc.verifier"
private const val REDIRECT = "oidc.redirect"

actual suspend fun providerSignIn(api: PlatformApi): User? {
    val redirectUri = window.location.origin + "/"
    val start = api.oidcStart(redirectUri)
    sessionStorage.setItem(STATE, start.state)
    sessionStorage.setItem(VERIFIER, start.codeVerifier)
    sessionStorage.setItem(REDIRECT, redirectUri)
    window.location.href = start.authorizationUrl
    return null
}

actual suspend fun resumeProviderSignIn(api: PlatformApi): User? {
    val callback = parseCallback(window.location.search) ?: return null
    window.history.replaceState(null, "", window.location.pathname)
    val state = sessionStorage.getItem(STATE)
    val verifier = sessionStorage.getItem(VERIFIER)
    val redirectUri = sessionStorage.getItem(REDIRECT)
    listOf(STATE, VERIFIER, REDIRECT).forEach(sessionStorage::removeItem)
    callback.error?.let { throw Exception(it) }
    if (callback.code == null || callback.state != state || verifier == null || redirectUri == null) throw Exception("Sign-in expired, try again")
    return api.oidcLogin(callback.code, verifier, redirectUri)
}
