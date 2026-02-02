package com.anjo.statisticsui.model

data class Result<T>(
    val status: Status = Status.LOADING,
    val data:T
)

enum class Status {
    SUCCESS, ERROR, LOADING
}
