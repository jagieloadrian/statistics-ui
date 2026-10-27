package com.anjo.statisticsui.auth

import com.anjo.statisticsui.api.PlatformApi
import com.anjo.statisticsui.model.User

actual suspend fun providerSignIn(api: PlatformApi): User? = throw Exception("Desktop provider sign-in is not available yet")

actual suspend fun resumeProviderSignIn(api: PlatformApi): User? = null
