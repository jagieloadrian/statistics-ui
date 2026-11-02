package com.anjo.statisticsui.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.ui.account.AccountScreen
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

@OptIn(ExperimentalTestApi::class)
class AccountScreenTest :
    FunSpec({

        val ann = User(id = 1, email = "ann@example.com", name = "Ann", role = "editor")

        fun FakeBackend.account() {
            session.signIn("t1", ann)
            reply("dashboards", listOf(DefinitionSummary("energy")))
            reply("auth/config", AuthConfig())
        }

        test("previewsAndSavesPreferences") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.account()
                backend.reply(
                    "me/prefs",
                    ann.copy(prefs = Prefs(theme = "dark", accent = "purple", homeDashboard = "energy")),
                    HttpMethod.Put,
                )
                show(backend) { AccountScreen() }
                awaitText("Ann · ann@example.com · editor")

                // when
                pick("Theme", "dark")
                pick("Units", "imperial")
                pick("Home dashboard", "energy")
                onNodeWithContentDescription("Purple").performClick()
                type("Time zone (optional)", "Europe/Warsaw")

                // then
                backend.session.draftPrefs shouldBe
                    Prefs(timeZone = "Europe/Warsaw", units = "imperial", theme = "dark", accent = "purple", homeDashboard = "energy")
                awaitText("Colour: Purple")

                // when
                click("Save preferences")

                // then
                awaitText("Preferences saved")
                backend.sent(HttpMethod.Put, "me/prefs").single().body shouldContain "\"accent\":\"purple\""
                backend.session.draftPrefs.shouldBeNull()
            }
        }

        test("changesThePassword") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.account()
                backend.ok("me/password", HttpMethod.Put)
                show(backend) { AccountScreen() }

                // when
                click("Change password")

                // then
                awaitText("Required")

                // when
                type("Current password", "old-password")
                type("New password", "new-password")
                type("Repeat new password", "new-password")
                click("Change password")

                // then
                awaitText("Password changed; your other sessions were signed out")
            }
        }

        test("showsErrorsAndSignsOut") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.account()
                backend.fail("me/prefs", HttpStatusCode.BadRequest, "Unknown time zone", HttpMethod.Put)
                backend.ok("auth/logout", HttpMethod.Post)
                show(backend) { AccountScreen() }

                // when
                click("Save preferences")

                // then
                awaitText("Unknown time zone")

                // when
                click("Sign out")
                waitUntil(timeoutMillis = 5_000) { backend.session.token == null }

                // then
                backend.session.user.shouldBeNull()
            }
        }

        test("providerAccountsCannotChangeThePassword") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.account()
                backend.session.signIn("t1", ann.copy(hasPassword = false))

                // when
                show(backend) { AccountScreen() }
                awaitText("Save preferences")

                // then
                onAllNodes(hasText("Change password")).fetchSemanticsNodes().size shouldBe 0
            }
        }
    })
