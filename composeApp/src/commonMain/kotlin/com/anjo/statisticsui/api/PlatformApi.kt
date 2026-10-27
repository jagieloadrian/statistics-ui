package com.anjo.statisticsui.api

import com.anjo.statisticsui.configuration.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement
import org.koin.core.annotation.Singleton

/** Non-2xx answer; [status] lets callers react to e.g. 409 (references, retry with `force`). */
class ApiException(message: String, val status: Int) : Exception(message)

/** Client for the platform API v2 (contracts/api-v2.md). `kind` = `datasets`, `sources` or `dashboards`. */
@Singleton
class PlatformApi(private val client: HttpClient, private val appConfig: AppConfig) {

    suspend fun list(kind: String): List<DefinitionSummary> = request(kind)

    /** [force] overrides the 409 reference guard (dataset still used by widgets, variables or sources). */
    suspend fun delete(kind: String, id: String, force: Boolean = false): Unit =
        request("$kind/$id") { method = HttpMethod.Delete; if (force) parameter("force", true) }

    suspend fun dataset(id: String): Dataset = request("datasets/$id")

    suspend fun source(id: String): Source = request("sources/$id")

    suspend fun dashboard(id: String): Dashboard = request("dashboards/$id")

    suspend fun putDataset(dataset: Dataset, force: Boolean = false): Unit = request("datasets/${dataset.id}") {
        json(HttpMethod.Put, dataset)
        if (force) parameter("force", true)
    }

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

    /** Raw JSON so the copy shown to the user keeps every field. */
    suspend fun exportBundle(): JsonElement = request("export")

    /** 200 = imported; 400 with the same shape = nothing stored. Any other answer throws. */
    suspend fun importBundle(bundle: JsonElement): ImportResult {
        val response = client.request(url("import")) {
            accept(ContentType.Application.Json)
            json(HttpMethod.Post, bundle)
        }
        val result = if (response.status.isSuccess() || response.status == HttpStatusCode.BadRequest) {
            runCatching { response.body<ImportResult>() }.getOrNull()
        } else null
        return result ?: throw response.apiException()
    }

    private fun url(path: String) = "${appConfig.backendUrl}/api/v2/$path"

    private suspend inline fun <reified T> request(path: String, block: HttpRequestBuilder.() -> Unit = {}): T {
        val response = client.request(url(path)) {
            accept(ContentType.Application.Json)
            block()
        }
        if (!response.status.isSuccess()) throw response.apiException()
        return response.body()
    }
}

/** Message plus `details` (e.g. the referencing widgets of a 409) from the [ApiError] body. */
private suspend fun HttpResponse.apiException(): ApiException {
    val error = runCatching { body<ApiError>() }.getOrNull()
    return ApiException(error?.let { (listOf(it.message) + it.details).joinToString("\n") } ?: "HTTP $status", status.value)
}

private inline fun <reified B : Any> HttpRequestBuilder.json(verb: HttpMethod, body: B) {
    method = verb
    contentType(ContentType.Application.Json)
    setBody(body)
}
