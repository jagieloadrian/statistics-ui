package com.anjo.statisticsui.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.EDITOR
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.VIEWER
import com.anjo.statisticsui.navigation.NavGraph
import com.anjo.statisticsui.ui.home.MainLayout
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest :
    FunSpec({

        val ann = User(id = 1, email = "ann@example.com", role = "admin", prefs = Prefs(favourites = listOf("b")))
        val dashboards = listOf(DefinitionSummary("a", "Alpha"), DefinitionSummary("b", "Beta"))

        test("adminTogglesFavouritesAndSeesAdministration") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.session.signIn("t1", ann)
                backend.reply("dashboards", dashboards)
                backend.reply("me/prefs", ann.copy(prefs = Prefs(favourites = listOf("b", "a"))), HttpMethod.Put)
                show(backend) { MainLayout(rememberNavController()) }
                awaitText("Alpha")

                // when
                onAllNodesWithContentDescription("Favourite")[1].performClick()

                // then
                waitUntil(timeoutMillis = 5_000) {
                    backend.session.user
                        ?.prefs
                        ?.favourites == listOf("b", "a")
                }
                awaitText("Administration")
                awaitText("Sources")
                awaitText("Users")
            }
        }

        test("viewerWithoutDashboardsIsToldToAsk") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.session.signIn("t1", ann.copy(role = VIEWER))
                backend.reply("dashboards", emptyList<DefinitionSummary>())

                // when
                show(backend) { MainLayout(rememberNavController()) }

                // then
                awaitText("No dashboards yet. Ask an editor to share one with you.")
            }
        }

        test("loadErrorIsShown") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.session.signIn("t1", ann)
                backend.fail("dashboards", HttpStatusCode.InternalServerError, "Database down")

                // when
                show(backend) { MainLayout(rememberNavController()) }

                // then
                awaitText("Database down")
            }
        }

        test("editorCreatesADashboard") {
            uiTest {
                // given
                val backend = FakeBackend()
                val editor = ann.copy(role = EDITOR)
                backend.session.signIn("t1", editor)
                backend.reply("me", editor)
                backend.reply("dashboards", dashboards)
                backend.ok("dashboards/energy", HttpMethod.Put)
                backend.reply("dashboards/energy", Dashboard(id = "energy", title = "Energy use"))
                show(backend) { NavGraph(rememberNavController()) }
                awaitText("Alpha")

                // when
                click("New dashboard")
                type("ID", "a")
                click("Create dashboard")

                // then
                awaitText("Already exists")

                // when
                type("ID", "energy")
                type("Title", "Energy use")
                click("Create dashboard")

                // then
                awaitText("No widgets yet. Use Edit to add one.")
                backend.sent(HttpMethod.Put, "dashboards/energy").single().body shouldContain "\"title\":\"Energy use\""
            }
        }

        test("cancelClosesTheNewDashboardDialog") {
            uiTest {
                // given
                val backend = FakeBackend()
                backend.session.signIn("t1", ann.copy(role = EDITOR))
                backend.reply("dashboards", emptyList<DefinitionSummary>())
                show(backend) { MainLayout(rememberNavController()) }
                awaitText("No dashboards yet. Create the first one with New dashboard.")

                // when
                click("New dashboard")
                awaitText("Create dashboard")
                click("Cancel")

                // then
                waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Create dashboard")).fetchSemanticsNodes().isEmpty() }
            }
        }

        test("compactLayoutUsesTheBottomBar") {
            uiTest(Size(400f, 800f)) {
                // given
                val backend = FakeBackend()
                backend.session.signIn("t1", ann)
                backend.reply("dashboards", dashboards)
                backend.reply("auth/config", AuthConfig())
                show(backend) { MainLayout(rememberNavController()) }
                awaitText("Beta")

                // when
                click("Account")

                // then
                awaitText("Preferences")
            }
        }
    })
