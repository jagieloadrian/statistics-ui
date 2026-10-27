package com.anjo.statisticsui.api

import com.anjo.statisticsui.configuration.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.koin.core.annotation.Singleton

class ApiException(message: String) : Exception(message)

/** Client for the platform API v2 (contracts/api-v2.md). `kind` = `datasets`, `sources` or `dashboards`. */
@Singleton
class PlatformApi(private val client: HttpClient, private val appConfig: AppConfig) {

    suspend fun list(kind: String): List<DefinitionSummary> = request(kind)

    suspend fun delete(kind: String, id: String): Unit = request("$kind/$id") { method = HttpMethod.Delete }

    suspend fun dataset(id: String): Dataset = request("datasets/$id")

    suspend fun source(id: String): Source = request("sources/$id")

    suspend fun dashboard(id: String): Dashboard = request("dashboards/$id")

    suspend fun putDataset(dataset: Dataset): Unit = request("datasets/${dataset.id}") { json(HttpMethod.Put, dataset) }

    suspend fun putSource(source: Source): Unit = request("sources/${source.id}") { json(HttpMethod.Put, source) }

    suspend fun putDashboard(dashboard: Dashboard): Unit =
        request("dashboards/${dashboard.id}") { json(HttpMethod.Put, dashboard) }

    /** Returns the new source token; it is shown once and the previous one stops working. */
    suspend fun rotateCredential(sourceId: String): String =
        request<Credential>("sources/$sourceId/credential") { method = HttpMethod.Post }.token

    suspend fun sourceStatus(sourceId: String): SourceStatus = request("sources/$sourceId/status")

    suspend fun testMapping(sourceId: String, test: MappingTestRequest): MappingTestResult =
        request("sources/$sourceId/mapping-test") { json(HttpMethod.Post, test) }

    suspend fun query(query: Query): QueryResult = request("query") { json(HttpMethod.Post, query) }

    suspend fun dimensionValues(dataset: String, dimension: String, search: String = "", limit: Int = 50, offset: Int = 0):
            List<DimensionValue> = request("datasets/$dataset/dimensions/$dimension/values") {
        parameter("search", search)
        parameter("limit", limit)
        parameter("offset", offset)
    }

    private suspend inline fun <reified T> request(path: String, block: HttpRequestBuilder.() -> Unit = {}): T {
        val response = client.request("${appConfig.backendUrl}/api/v2/$path") {
            accept(ContentType.Application.Json)
            block()
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ApiError>() }.getOrNull()
            throw ApiException(error?.let { (listOf(it.message) + it.details).joinToString("\n") } ?: "HTTP ${response.status}")
        }
        return response.body()
    }
}

private inline fun <reified B : Any> HttpRequestBuilder.json(verb: HttpMethod, body: B) {
    method = verb
    contentType(ContentType.Application.Json)
    setBody(body)
}
