package com.anjo.statisticsui.configuration

import com.anjo.statisticsui.BuildKonfig
import org.koin.core.annotation.Singleton

@Singleton
class AppConfig {
    val backendUrl: String = BuildKonfig.backendurl
}
