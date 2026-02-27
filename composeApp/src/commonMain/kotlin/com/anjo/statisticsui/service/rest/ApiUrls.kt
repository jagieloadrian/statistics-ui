package com.anjo.statisticsui.service.rest

object ApiUrls {

    const val TEMPERATURE_DEVICES = "/api/v1/stats/expose/temperature/devices"
    fun getTemperatureSeriesUrl(deviceId:String) = "$TEMPERATURE_DEVICES/$deviceId"
    fun getTemperatureSummaryUrl(deviceId:String) = "$TEMPERATURE_DEVICES/$deviceId/summary"

    const val EPIDEMIC_RUNS = "/api/v1/stats/expose/epidemic/runs"
    fun getEpidemicDetailsUrl(deviceId:String, runId:String) = "$EPIDEMIC_RUNS/$deviceId/run/$runId"
    fun getEpidemicSummaryUrl(deviceId:String, runId:String) = "$EPIDEMIC_RUNS/$deviceId/run/$runId/summary"
}