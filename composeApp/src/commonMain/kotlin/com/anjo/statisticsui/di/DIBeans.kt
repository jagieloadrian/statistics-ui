package com.anjo.statisticsui.di

import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Singleton
fun getDefaultDispatcher() = Dispatchers.Default

@KoinApplication(modules = [StatsUiModule::class])
class StatsUIApplication

@Module
@Configuration
@ComponentScan("com.anjo.statisticsui")
class StatsUiModule
