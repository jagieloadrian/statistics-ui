package com.anjo.statisticsui.api

import com.anjo.statisticsui.BuildKonfig
import com.anjo.statisticsui.auth.Session
import com.anjo.statisticsui.model.ApiError
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.Credential
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.DimensionValue
import com.anjo.statisticsui.model.ImportResult
import com.anjo.statisticsui.model.LoginRequest
import com.anjo.statisticsui.model.LoginResponse
import com.anjo.statisticsui.model.MappingTestRequest
import com.anjo.statisticsui.model.MappingTestResult
import com.anjo.statisticsui.model.NewUser
import com.anjo.statisticsui.model.OidcLoginRequest
import com.anjo.statisticsui.model.OidcStart
import com.anjo.statisticsui.model.OidcStartRequest
import com.anjo.statisticsui.model.PasswordChange
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.QueryResult
import com.anjo.statisticsui.model.Shares
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.model.SourceStatus
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.UserUpdate
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement
import org.koin.core.annotation.Singleton

class ApiException(
    message: String,
    val status: Int,
) : Exception(message)

@Singleton
class PlatformApi(
    private val client: HttpClient,
    private val session: Session,
) {
    suspend fun login(
        email: String,
        password: String,
    ): User {
        val answer: LoginResponse = request("auth/login") { json(HttpMethod.Post, LoginRequest(email, password)) }
        session.signIn(answer.token, answer.user)
        return answer.user
    }

    suspend fun authConfig(): AuthConfig = request("auth/config")

    suspend fun oidcStart(redirectUri: String): OidcStart =
        request("auth/oidc/start") { json(HttpMethod.Post, OidcStartRequest(redirectUri)) }

    suspend fun oidcLogin(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): User {
        val answer: LoginResponse = request("auth/oidc") { json(HttpMethod.Post, OidcLoginRequest(code, codeVerifier, redirectUri)) }
        session.signIn(answer.token, answer.user)
        return answer.user
    }

    suspend fun logout(): Result<Unit> {
        val serverRevocation = runSuspendCatching { request<Unit>("auth/logout") { method = HttpMethod.Post } }
        session.signOut()
        return serverRevocation
    }

    suspend fun me(): User = request<User>("me").also { session.user = it }

    suspend fun putPrefs(prefs: Prefs): User = request<User>("me/prefs") { json(HttpMethod.Put, prefs) }.also { session.user = it }

    suspend fun changePassword(
        current: String,
        new: String,
    ): Unit = request("me/password") { json(HttpMethod.Put, PasswordChange(current, new)) }

    suspend fun users(): List<User> = request("users")

    suspend fun createUser(user: NewUser): User = request("users") { json(HttpMethod.Post, user) }

    suspend fun updateUser(
        id: Long,
        update: UserUpdate,
    ): Unit = request("users/$id") { json(HttpMethod.Put, update) }

    suspend fun deleteUser(id: Long): Unit = request("users/$id") { method = HttpMethod.Delete }

    suspend fun shares(dashboardId: String): List<Long> = request<Shares>("dashboards/$dashboardId/shares").userIds

    suspend fun putShares(
        dashboardId: String,
        userIds: List<Long>,
    ): Unit = request("dashboards/$dashboardId/shares") { json(HttpMethod.Put, Shares(userIds)) }

    suspend fun list(kind: String): List<DefinitionSummary> = request(kind)

    suspend fun delete(
        kind: String,
        id: String,
        force: Boolean = false,
    ): Unit =
        request("$kind/$id") {
            method = HttpMethod.Delete
            if (force) parameter("force", true)
        }

    suspend fun dataset(id: String): Dataset = request("datasets/$id")

    suspend fun source(id: String): Source = request("sources/$id")

    suspend fun dashboard(id: String): Dashboard = request("dashboards/$id")

    suspend fun putDataset(
        dataset: Dataset,
        force: Boolean = false,
    ): Unit =
        request("datasets/${dataset.id}") {
            json(HttpMethod.Put, dataset)
            if (force) parameter("force", true)
        }

    suspend fun putSource(source: Source): Unit = request("sources/${source.id}") { json(HttpMethod.Put, source) }

    suspend fun putDashboard(dashboard: Dashboard): Unit = request("dashboards/${dashboard.id}") { json(HttpMethod.Put, dashboard) }

    suspend fun rotateCredential(sourceId: String): String =
        request<Credential>("sources/$sourceId/credential") { method = HttpMethod.Post }.token

    suspend fun sourceStatus(sourceId: String): SourceStatus = request("sources/$sourceId/status")

    suspend fun testMapping(
        sourceId: String,
        test: MappingTestRequest,
    ): MappingTestResult = request("sources/$sourceId/mapping-test") { json(HttpMethod.Post, test) }

    suspend fun query(query: Query): QueryResult = request("query") { json(HttpMethod.Post, query) }

    suspend fun dimensionValues(
        dataset: String,
        dimension: String,
        search: String = "",
        limit: Int = 50,
        offset: Int = 0,
    ): List<DimensionValue> =
        request("datasets/$dataset/dimensions/$dimension/values") {
            parameter("search", search)
            parameter("limit", limit)
            parameter("offset", offset)
        }

    suspend fun exportBundle(): JsonElement = request("export")

    suspend fun importBundle(bundle: JsonElement): ImportResult {
        val response = send("import") { json(HttpMethod.Post, bundle) }
        val result =
            if (response.status.isSuccess() || response.status == HttpStatusCode.BadRequest) {
                runSuspendCatching { response.body<ImportResult>() }.getOrNull()
            } else {
                null
            }
        return result ?: throw response.apiException()
    }

    private fun url(path: String) = "${BuildKonfig.backendurl}/api/v2/$path"

    private fun HttpRequestBuilder.authorize() {
        accept(ContentType.Application.Json)
        session.token?.let { bearerAuth(it) }
    }

    private suspend fun HttpResponse.apiException(): ApiException {
        if (status == HttpStatusCode.Unauthorized && call.request.headers[HttpHeaders.Authorization] != null) session.signOut()
        val error = runSuspendCatching { body<ApiError>() }.getOrNull()
        return ApiException(error?.let { (listOf(it.message) + it.details).joinToString("\n") } ?: "HTTP $status", status.value)
    }

    // Failing to get any response means the server is unreachable (down, offline, CORS), whatever the engine calls it
    private suspend fun send(
        path: String,
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse =
        runSuspendCatching {
            client.request(url(path)) {
                authorize()
                block()
            }
        }.getOrElse { throw ApiException("Cannot reach the server. Check your connection and try again.", 0) }

    private suspend inline fun <reified T> request(
        path: String,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = send(path, block)
        if (!response.status.isSuccess()) throw response.apiException()
        return response.body()
    }
}

private inline fun <reified B : Any> HttpRequestBuilder.json(
    verb: HttpMethod,
    body: B,
) {
    method = verb
    contentType(ContentType.Application.Json)
    setBody(body)
}
