package com.anjo.statisticsui.auth

import kotlinx.browser.localStorage

actual fun loadSetting(key: String): String? = localStorage.getItem(key)

actual fun saveSetting(
    key: String,
    value: String?,
) = if (value == null) localStorage.removeItem(key) else localStorage.setItem(key, value)
