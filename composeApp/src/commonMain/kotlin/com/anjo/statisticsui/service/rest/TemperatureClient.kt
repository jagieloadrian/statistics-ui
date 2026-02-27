package com.anjo.statisticsui.service.rest

import com.anjo.statisticsui.configuration.BuilAppConfig
import com.anjo.statisticsui.model.dto.Resolution
import com.anjo.statisticsui.model.dto.TemperatureDevice
import com.anjo.statisticsui.model.dto.TemperatureSeries
import com.anjo.statisticsui.model.dto.TemperatureSummary
import com.anjo.statisticsui.service.rest.ApiUrls.TEMPERATURE_DEVICES
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Singleton

interface TemperatureApi {
    suspend fun getTemperatureDevices(): List<TemperatureDevice>
    suspend fun getTemperatureTimeSeries(deviceId: String, from: LocalDateTime, to: LocalDateTime,
                                         resolution: Resolution): List<TemperatureSeries>

    suspend fun getTemperatureSummary(deviceId: String): TemperatureSummary
}

@Singleton
class TemperatureClient(private val client: HttpClient, private val appConfig: BuilAppConfig
) : TemperatureApi {
    private val logger = KotlinLogging.logger { }


    override suspend fun getTemperatureDevices(): List<TemperatureDevice> {
        logger.info { "Calling endpoint $TEMPERATURE_DEVICES into ${appConfig.backendUrl}  ..." }
        return client.get(appConfig.backendUrl) {
            url {
                path(TEMPERATURE_DEVICES)
            }
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
        }.body<List<TemperatureDevice>>()
    }

    override suspend fun getTemperatureTimeSeries(
            deviceId: String,
            from: LocalDateTime,
            to: LocalDateTime,
            resolution: Resolution
    ): List<TemperatureSeries> {
        return client.get(appConfig.backendUrl) {
            url {
                path(ApiUrls.getTemperatureSeriesUrl(deviceId))
                parameters.append("from", from.toString())
                parameters.append("to", to.toString())
                parameters.append("resolution", resolution.param)
            }
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
        }
                .body()
    }

    override suspend fun getTemperatureSummary(deviceId: String): TemperatureSummary {
        return client.get(appConfig.backendUrl) {
            url {
                path(ApiUrls.getTemperatureSummaryUrl(deviceId))
            }
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
        }
                .body()
    }

}