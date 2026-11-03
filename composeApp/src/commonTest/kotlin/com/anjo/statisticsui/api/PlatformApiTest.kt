package com.anjo.statisticsui.api

import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.model.ImportResult
import com.anjo.statisticsui.model.Kind
import com.anjo.statisticsui.model.LoginResponse
import com.anjo.statisticsui.model.NewUser
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class PlatformApiTest :
    FunSpec({

        val ann = User(id = 1, email = "ann@example.com", role = "admin")

        test("loginStoresTheTokenAndSignsLaterRequests") {
            // given
            val backend = FakeBackend()
            backend.reply("auth/login", LoginResponse("t1", ann), HttpMethod.Post)
            backend.reply("users", listOf(ann))

            // when
            val user = backend.api.login("ann@example.com", "secret")
            backend.api.users()

            // then
            user shouldBe ann
            backend.session.token shouldBe "t1"
            backend.session.user shouldBe ann
            backend.sent(HttpMethod.Post, "auth/login").single().body shouldContain "\"password\":\"secret\""
            backend.sent(HttpMethod.Get, "users").single().authorization shouldBe "Bearer t1"
        }

        test("serverErrorMessageAndDetailsAreShown") {
            // given
            val backend = FakeBackend()
            backend.json(
                "users",
                """{"error":"bad","message":"Invalid user","details":["email taken"]}""",
                HttpMethod.Post,
                HttpStatusCode.BadRequest,
            )

            // when
            val error = shouldThrow<ApiException> { backend.api.createUser(NewUser("a@b", role = "viewer", password = "p")) }

            // then
            error.message shouldBe "Invalid user\nemail taken"
            error.status shouldBe 400
        }

        test("errorWithoutBodyShowsTheStatus") {
            // given
            val backend = FakeBackend()
            backend.json("users", "", status = HttpStatusCode.InternalServerError)

            // when
            val error = shouldThrow<ApiException> { backend.api.users() }

            // then
            error.message shouldBe "HTTP 500 Internal Server Error"
        }

        test("unauthorizedSignedRequestSignsOut") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("expired", ann)
            backend.fail("me", HttpStatusCode.Unauthorized, "Session expired")

            // when
            shouldThrow<ApiException> { backend.api.me() }.status shouldBe 401

            // then
            backend.session.token.shouldBeNull()
            backend.session.user.shouldBeNull()
        }

        test("wrongPasswordKeepsTheSession") {
            // given
            val backend = FakeBackend()
            backend.session.signOut()
            backend.fail("auth/login", HttpStatusCode.Unauthorized, "Wrong email or password", HttpMethod.Post)

            // when
            val error = shouldThrow<ApiException> { backend.api.login("ann@example.com", "wrong") }

            // then
            error.message shouldBe "Wrong email or password"
            backend
                .sent(HttpMethod.Post, "auth/login")
                .single()
                .authorization
                .shouldBeNull()
        }

        test("unreachableServerIsExplained") {
            // given
            val backend = FakeBackend()
            backend.offline = true

            // when
            val error = shouldThrow<ApiException> { backend.api.authConfig() }

            // then
            error.message shouldBe "Cannot reach the server. Check your connection and try again."
            error.status shouldBe 0
        }

        test("logoutSignsOutEvenWhenTheServerFails") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t1", ann)
            backend.fail("auth/logout", HttpStatusCode.InternalServerError, "Down", HttpMethod.Post)

            // when
            val result = backend.api.logout()

            // then
            result.exceptionOrNull()?.message shouldBe "Down"
            backend.session.token.shouldBeNull()
        }

        test("meAndPrefsUpdateTheSessionUser") {
            // given
            val backend = FakeBackend()
            val dark = ann.copy(prefs = Prefs(theme = "dark"))
            backend.reply("me", ann)
            backend.reply("me/prefs", dark, HttpMethod.Put)

            // when
            backend.api.me()

            // then
            backend.session.user shouldBe ann

            // when
            backend.api.putPrefs(dark.prefs)

            // then
            backend.session.user shouldBe dark
        }

        test("forcedDeleteAndValueSearchSendParameters") {
            // given
            val backend = FakeBackend()
            backend.ok("datasets/weather", HttpMethod.Delete)
            backend.json("datasets/weather/dimensions/city/values", "[]")

            // when
            backend.api.delete(Kind.DATASETS, "weather", force = true)
            backend.api.dimensionValues("weather", "city", search = "Kra", limit = 5, offset = 10)

            // then
            backend.sent(HttpMethod.Delete, "datasets/weather").single().parameters["force"] shouldBe "true"
            val search = backend.sent(HttpMethod.Get, "datasets/weather/dimensions/city/values").single().parameters
            listOf(search["search"], search["limit"], search["offset"]) shouldBe listOf("Kra", "5", "10")
        }

        test("sharesAndCredentials") {
            // given
            val backend = FakeBackend()
            backend.json("dashboards/d1/shares", """{"userIds":[1,2]}""")
            backend.ok("dashboards/d1/shares", HttpMethod.Put)
            backend.json("sources/s1/credential", """{"token":"secret-token"}""", HttpMethod.Post)

            // when
            val shares = backend.api.shares("d1")
            backend.api.putShares("d1", listOf(3))
            val token = backend.api.rotateCredential("s1")

            // then
            shares shouldBe listOf(1L, 2L)
            backend.sent(HttpMethod.Put, "dashboards/d1/shares").single().body shouldBe """{"userIds":[3]}"""
            token shouldBe "secret-token"
        }

        test("importReturnsRejectedBundleErrors") {
            // given
            val backend = FakeBackend()
            backend.json("import", """{"created":0,"updated":0,"errors":["bad id"]}""", HttpMethod.Post, HttpStatusCode.BadRequest)

            // when
            val result = backend.api.importBundle(buildJsonObject { put("sources", JsonPrimitive(1)) })

            // then
            result shouldBe ImportResult(0, 0, listOf("bad id"))
        }

        test("importFailureWithoutResultThrows") {
            // given
            val backend = FakeBackend()
            backend.fail("import", HttpStatusCode.BadRequest, "Not a bundle", HttpMethod.Post)

            // when and then
            shouldThrow<ApiException> { backend.api.importBundle(JsonPrimitive(1)) }.message shouldBe "Not a bundle"
        }

        test("importServerErrorThrows") {
            // given
            val backend = FakeBackend()
            backend.fail("import", HttpStatusCode.InternalServerError, "Down", HttpMethod.Post)

            // when and then
            shouldThrow<ApiException> { backend.api.importBundle(JsonPrimitive(1)) }.status shouldBe 500
        }

        test("productionClientNegotiatesJson") {
            // when
            val client = createHttpClient()

            // then
            client.pluginOrNull(ContentNegotiation).shouldNotBeNull()
            client.close()
        }
    })
