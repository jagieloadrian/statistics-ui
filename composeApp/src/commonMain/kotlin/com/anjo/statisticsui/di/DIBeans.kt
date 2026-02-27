package com.anjo.statisticsui.di

import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Singleton

@Singleton
fun getDefaultDispatcher() = Dispatchers.Default