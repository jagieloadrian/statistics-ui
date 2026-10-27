package com.anjo.statisticsui.auth

import java.util.prefs.Preferences

private val prefs = Preferences.userRoot().node("com/anjo/statisticsui")

actual fun loadToken(): String? = prefs.get("token", null)

actual fun saveToken(token: String?) = if (token == null) prefs.remove("token") else prefs.put("token", token)
