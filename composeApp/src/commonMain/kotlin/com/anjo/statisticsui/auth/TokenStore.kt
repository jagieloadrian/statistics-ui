package com.anjo.statisticsui.auth

expect fun loadToken(): String?

expect fun saveToken(token: String?)
