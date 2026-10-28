package com.anjo.statisticsui.auth

import kotlinx.browser.localStorage

actual fun loadToken(): String? = localStorage.getItem("token")

actual fun saveToken(token: String?) = if (token == null) localStorage.removeItem("token") else localStorage.setItem("token", token)
