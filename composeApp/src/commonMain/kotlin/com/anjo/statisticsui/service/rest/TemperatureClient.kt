package com.anjo.statisticsui.service.rest

import com.anjo.statisticsui.BuildKonfig
import com.anjo.statisticsui.model.dto.Resolution
import com.anjo.statisticsui.model.dto.TemperatureDevice
import com.anjo.statisticsui.model.dto.TemperatureSeries
import com.anjo.statisticsui.model.dto.TemperatureSummary
import com.anjo.statisticsui.service.rest.ApiUrls.TEMPERATURE_DEVICES
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.datetime.LocalDateTime

interface TemperatureApi {
    suspend fun getTemperatureDevices() : List<TemperatureDevice>
    suspend fun getTemperatureTimeSeries(deviceId:String, from: LocalDateTime, to: LocalDateTime, resolution: Resolution) : List<TemperatureSeries>
    suspend fun getTemperatureSummary(deviceId:String): TemperatureSummary
}

class TemperatureClient(private val client: HttpClient, private val backendUrl:String = BuildKonfig.backendurl): TemperatureApi {

    override suspend fun getTemperatureDevices(): List<TemperatureDevice> {
        return  client.get {
            url {
                protocol = URLProtocol.HTTP
                host = backendUrl
                path(TEMPERATURE_DEVICES)
            }
            headers {
                append(HttpHeaders.Accept , "application/json")
            }
        }.body<List<TemperatureDevice>>()
    }

    override suspend fun getTemperatureTimeSeries(
        deviceId: String,
        from: LocalDateTime,
        to: LocalDateTime,
        resolution: Resolution
    ): List<TemperatureSeries> {
        TODO("Not yet implemented")
    }

    override suspend fun getTemperatureSummary(deviceId: String): TemperatureSummary {
        TODO("Not yet implemented")
    }

}