package com.anjo.statisticsui

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform