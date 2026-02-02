package com.anjo.statisticsui.service.rest

object ApiUrls {

    const val TEMPERATURE_DEVICES = "/api/v1/temperature/devices"
    const val TEMPERATURE_SERIES = "$TEMPERATURE_DEVICES/{deviceId}"
    const val TEMPERATURE_SUMMARY = "$TEMPERATURE_DEVICES/{deviceId}/summary"
}