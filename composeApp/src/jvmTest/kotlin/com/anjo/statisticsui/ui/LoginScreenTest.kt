package com.anjo.statisticsui.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.LoginResponse
import com.anjo.statisticsui.model.OidcInfo
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.login.LoginScreen
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest :
    FunSpec({

        val ann = User(id = 1, email = "ann@example.com", role = "admin")

        test("signsInWithEmailAndPassword") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.reply("auth/config", AuthConfig())
                backend.reply("auth/login", LoginResponse("t1", ann), HttpMethod.Post)
                var signedIn: User? = null
                show(backend) { LoginScreen(onSignedIn = { signedIn = it }) }

                // when
                type("Email", "ann@example.com")
                type("Password", "secret")
                click("Sign in")
                waitUntil(timeoutMillis = 5_000) { signedIn != null }

                // then
                signedIn shouldBe ann
            }
        }

        test("showsFieldErrorsAndServerErrors") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.reply("auth/config", AuthConfig())
                backend.fail("auth/login", HttpStatusCode.Unauthorized, "Wrong email or password", HttpMethod.Post)
                show(backend) { LoginScreen(onSignedIn = {}) }

                // when
                click("Sign in")

                // then
                onAllNodes(hasText("Required", substring = true)).assertCountEquals(2)

                // when
                type("Email", "ann")
                type("Password", "wrong")

                // then
                awaitText("Not an email address")

                // when
                type("Email", "ann@example.com")
                click("Sign in")

                // then
                awaitText("Wrong email or password")
            }
        }

        test("providerOnlySignInHidesThePasswordForm") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.reply("auth/config", AuthConfig(local = false, oidc = OidcInfo("Keycloak")))
                backend.fail("auth/oidc/start", HttpStatusCode.ServiceUnavailable, "Provider unavailable", HttpMethod.Post)
                show(backend) { LoginScreen(onSignedIn = {}) }

                // when
                awaitText("Sign in with Keycloak")
                click("Sign in with Keycloak")

                // then
                awaitText("Provider unavailable")
                onAllNodes(hasSetTextAction()).assertCountEquals(0)
            }
        }
    })
