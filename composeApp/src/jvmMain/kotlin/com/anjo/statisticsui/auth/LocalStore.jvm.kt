package com.anjo.statisticsui.auth

import java.util.prefs.Preferences

private val prefs = Preferences.userRoot().node("com/anjo/statisticsui")

actual fun loadSetting(key: String): String? = prefs.get(key, null)

actual fun saveSetting(
    key: String,
    value: String?,
) = if (value == null) prefs.remove(key) else prefs.put(key, value)
