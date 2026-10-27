package com.anjo.statisticsui.auth

/** Bearer token persisted across restarts (desktop: java.util.prefs, web: localStorage); null removes it. */
expect fun loadToken(): String?

expect fun saveToken(token: String?)
