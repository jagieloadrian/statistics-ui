package com.anjo.statisticsui.auth

// Values kept on this device across restarts: java.util.prefs on desktop, localStorage in the browser
expect fun loadSetting(key: String): String?

expect fun saveSetting(
    key: String,
    value: String?,
)
