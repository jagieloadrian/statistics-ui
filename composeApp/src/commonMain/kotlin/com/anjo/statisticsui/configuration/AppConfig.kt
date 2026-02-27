package com.anjo.statisticsui.configuration

import com.anjo.statisticsui.BuildKonfig
import org.koin.core.annotation.Singleton


interface AppConfig {
    val backendUrl: String
}

@Singleton
class BuilAppConfig: AppConfig {
    override val backendUrl: String = BuildKonfig.backendurl
}