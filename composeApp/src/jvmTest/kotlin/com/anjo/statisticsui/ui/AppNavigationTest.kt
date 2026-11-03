package com.anjo.statisticsui.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.LoginResponse
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.navigation.NavGraph
import com.anjo.statisticsui.ui.splash.SplashScreen
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

@OptIn(ExperimentalTestApi::class)
class AppNavigationTest :
    FunSpec({

        val ann = User(id = 1, email = "ann@example.com", role = "admin")

        fun FakeBackend.home() {
            reply("auth/config", AuthConfig())
            reply("dashboards", listOf(DefinitionSummary("energy", "Energy")))
            reply("dashboards/energy", Dashboard(id = "energy", title = "Energy use"))
            reply("datasets", emptyList<DefinitionSummary>())
            reply("sources", emptyList<DefinitionSummary>())
            reply("users", listOf(ann))
        }

        test("splashFinishes") {
            uiTest {
                // given
                var finished = false

                // when
                show(FakeBackend()) { SplashScreen { finished = true } }

                // then
                awaitText("Your data in one place")
                waitUntil(timeoutMillis = 5_000) { finished }
            }
        }

        test("signedOutUserSignsInAndOpensADashboard") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.session.signOut()
                backend.home()
                backend.reply("auth/login", LoginResponse("t1", ann), HttpMethod.Post)
                show(backend) { NavGraph(rememberNavController()) }

                // when
                awaitText("Email")
                type("Email", "ann@example.com")
                type("Password", "secret")
                click("Sign in")
                awaitText("Energy")
                click("Energy")

                // then
                awaitText("No widgets yet. Use Edit to add one.")
                awaitText("Energy use")
            }
        }

        test("adminOpensTheSourcesScreen") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.home()
                backend.session.signIn("t1", ann)
                backend.reply("me", ann)
                show(backend) { NavGraph(rememberNavController()) }

                // when
                awaitText("Administration")
                click("Sources")

                // then
                awaitText("New source")
            }
        }

        test("rememberedSessionOpensTheHomeDashboard") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.home()
                backend.session.signIn("t1", ann)
                backend.reply("me", ann.copy(prefs = Prefs(homeDashboard = "energy")))
                show(backend) { NavGraph(rememberNavController()) }

                // when
                awaitText("Energy use")

                // then
                backend.session.user
                    ?.prefs
                    ?.homeDashboard shouldBe "energy"

                // when
                backend.session.signOut()

                // then
                awaitText("Email")
            }
        }

        test("expiredSessionGoesToLogin") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.home()
                backend.session.signIn("expired", ann)
                backend.fail("me", HttpStatusCode.Unauthorized, "Session expired")

                // when
                show(backend) { NavGraph(rememberNavController()) }

                // then
                awaitText("Email")
            }
        }
    })
