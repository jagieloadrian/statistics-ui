package com.anjo.statisticsui.model

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

typealias VariableValue = List<String>

data class TimeRange(
    val from: Instant,
    val to: Instant,
) {
    companion object {
        fun last(
            duration: Duration,
            now: Instant = Clock.System.now(),
        ) = TimeRange(now - duration, now)
    }
}
