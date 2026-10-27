package com.anjo.statisticsui.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@KoinApplication(modules = [StatsUiModule::class])
class StatsUIApplication

@Module
@Configuration
@ComponentScan("com.anjo.statisticsui")
class StatsUiModule
