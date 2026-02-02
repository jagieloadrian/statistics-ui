package com.anjo.statisticsui.service.rest

object ApiUrls {

    const val TEMPERATURE_DEVICES = "/api/v1/temperature/devices"
    fun getTemperatureSeriesUrl(deviceId:String) = "$TEMPERATURE_DEVICES/$deviceId"
    fun getTemperatureSummaryUrl(deviceId:String) = "$TEMPERATURE_DEVICES/$deviceId/summary"
}