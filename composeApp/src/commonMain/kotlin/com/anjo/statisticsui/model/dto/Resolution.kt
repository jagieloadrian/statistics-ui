package com.anjo.statisticsui.model.dto

enum class Resolution(val param:String) {
    RAW("raw"), ONE_MIN("1m"), FIVE_MINUTES("5m"),
    TEN_MINUTES("10m"), TWENTY_MINUTES("20m"),
    THIRTY_MINUTES("30m");

    companion object {
        fun fromQuery(param: String?): Resolution {
            return entries.firstOrNull { it.param == param } ?: RAW
        }
    }
}