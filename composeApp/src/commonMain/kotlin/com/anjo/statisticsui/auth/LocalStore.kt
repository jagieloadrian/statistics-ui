package com.anjo.statisticsui.auth

expect fun loadSetting(key: String): String?

expect fun saveSetting(
    key: String,
    value: String?,
)
