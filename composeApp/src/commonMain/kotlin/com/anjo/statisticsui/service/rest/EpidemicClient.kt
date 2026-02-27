package com.anjo.statisticsui.service.rest

import com.anjo.statisticsui.configuration.BuilAppConfig
import com.anjo.statisticsui.model.dto.EpidemicRun
import com.anjo.statisticsui.model.dto.EpidemicShortRun
import com.anjo.statisticsui.model.dto.EpidemicSummary
import com.anjo.statisticsui.service.rest.ApiUrls.EPIDEMIC_RUNS
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.path
import org.koin.core.annotation.Singleton

interface EpidemicApi {

    suspend fun getEpidemicRuns(): List<EpidemicShortRun>
    suspend fun getEpidemicDetails(deviceId: String, runId: String): EpidemicRun
    suspend fun getEpidemicRunSummary(deviceId: String, runId: String): EpidemicSummary

}

@Singleton
class EpidemicClient(private val client: HttpClient, private val appConfig: BuilAppConfig) : EpidemicApi {
    private val logger = KotlinLogging.logger { }

    override suspend fun getEpidemicRuns(): List<EpidemicShortRun> {
        logger.info { "Calling endpoint $EPIDEMIC_RUNS into ${appConfig.backendUrl}  ..." }
        return client.get(appConfig.backendUrl) {
            url {
                path(EPIDEMIC_RUNS)
            }
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
        }.body<List<EpidemicShortRun>>()

    }

    override suspend fun getEpidemicDetails(deviceId: String, runId: String): EpidemicRun {
        return client.get(appConfig.backendUrl) {
            url {
                path(ApiUrls.getEpidemicDetailsUrl(deviceId, runId))
            }
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
        }.body()
    }

    override suspend fun getEpidemicRunSummary(deviceId: String, runId: String): EpidemicSummary {
        return client.get(appConfig.backendUrl) {
            url {
                path(ApiUrls.getEpidemicSummaryUrl(deviceId, runId))
            }
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
        }.body()
    }
}