package com.anjo.statisticsui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Statistics-UI",
    ) {
        App()
    }
}