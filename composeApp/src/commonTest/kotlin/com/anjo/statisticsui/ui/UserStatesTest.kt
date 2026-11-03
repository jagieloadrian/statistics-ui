package com.anjo.statisticsui.ui

import com.anjo.statisticsui.FakeBackend
import com.anjo.statisticsui.awaitSnackbar
import com.anjo.statisticsui.model.AuthConfig
import com.anjo.statisticsui.model.DefinitionSummary
import com.anjo.statisticsui.model.EDITOR
import com.anjo.statisticsui.model.LoginResponse
import com.anjo.statisticsui.model.OidcInfo
import com.anjo.statisticsui.model.Prefs
import com.anjo.statisticsui.model.User
import com.anjo.statisticsui.model.VIEWER
import com.anjo.statisticsui.settle
import com.anjo.statisticsui.stateScope
import com.anjo.statisticsui.ui.account.AccountState
import com.anjo.statisticsui.ui.admin.UsersState
import com.anjo.statisticsui.ui.home.HomeState
import com.anjo.statisticsui.ui.login.LoginState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

class UserStatesTest :
    FunSpec({

        val ann = User(id = 1, email = "ann@example.com", name = "Ann", role = "admin", prefs = Prefs(favourites = listOf("b")))
        val bob = User(id = 2, email = "bob@example.com", role = VIEWER)
        val dashboards = listOf(DefinitionSummary("a", "Alpha"), DefinitionSummary("b", "Beta"))

        test("homeListsFavouritesFirst") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", ann)
            backend.reply("dashboards", dashboards)
            val state = HomeState(backend.api, backend.session, stateScope())

            // when
            state.load().join()

            // then
            state.loading shouldBe false
            state.sortedDashboards.map { it.id } shouldBe listOf("b", "a")
            state.canCreate shouldBe true
            state.isAdmin shouldBe true
        }

        test("homeShowsLoadErrors") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", bob)
            backend.fail("dashboards", HttpStatusCode.InternalServerError, "Database down")
            val state = HomeState(backend.api, backend.session, stateScope())

            // when
            state.load().join()

            // then
            state.error shouldBe "Database down"
            state.loading shouldBe false
            state.canCreate shouldBe false
        }

        test("homeTogglesFavourites") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", ann)
            backend.reply("me/prefs", ann.copy(prefs = Prefs(favourites = listOf("b", "a"))), HttpMethod.Put)
            val scope = stateScope()
            val state = HomeState(backend.api, backend.session, scope)

            // when
            state.toggleFavourite("a")
            scope.settle()

            // then
            backend.sent(HttpMethod.Put, "me/prefs").single().body shouldContain "\"favourites\":[\"b\",\"a\"]"
            state.favourites shouldBe listOf("b", "a")
        }

        test("loginValidatesBeforeCallingTheServer") {
            // given
            val backend = FakeBackend()
            val state = LoginState(backend.api, stateScope())

            // when
            state.signIn {}

            // then
            state.errors shouldBe mapOf("email" to "Required", "password" to "Required")
            backend.sent().shouldBeEmpty()
        }

        test("loginSignsInWithTrimmedEmail") {
            // given
            val backend = FakeBackend()
            backend.reply("auth/login", LoginResponse("t1", ann), HttpMethod.Post)
            val scope = stateScope()
            val state = LoginState(backend.api, scope)
            var signedIn: User? = null
            state.email = " ann@example.com "
            state.password = "secret"

            // when
            state.signIn { signedIn = it }
            scope.settle()

            // then
            signedIn shouldBe ann
            state.busy shouldBe false
            backend.sent(HttpMethod.Post, "auth/login").single().body shouldContain "\"email\":\"ann@example.com\""
        }

        test("loginShowsWrongPassword") {
            // given
            val backend = FakeBackend()
            backend.fail("auth/login", HttpStatusCode.Unauthorized, "Wrong email or password", HttpMethod.Post)
            val scope = stateScope()
            val state = LoginState(backend.api, scope)
            state.email = "ann@example.com"
            state.password = "wrong"

            // when
            state.signIn {}
            scope.settle()

            // then
            state.error shouldBe "Wrong email or password"
        }

        test("loginConfigHidesThePasswordFormForProviderOnlySignIn") {
            // given
            val backend = FakeBackend()
            backend.reply("auth/config", AuthConfig(local = false, oidc = OidcInfo("Keycloak")))
            val state = LoginState(backend.api, stateScope())

            // when
            state.load {}

            // then
            state.showsPasswordForm shouldBe false
            state.config?.oidc?.name shouldBe "Keycloak"
        }

        test("providerSignInFailureIsShown") {
            // given
            val backend = FakeBackend()
            backend.fail("auth/oidc/start", HttpStatusCode.ServiceUnavailable, "Provider unavailable", HttpMethod.Post)
            val scope = stateScope()
            val state = LoginState(backend.api, scope)

            // when
            state.signInWithProvider {}
            scope.settle()

            // then
            state.error shouldBe "Provider unavailable"
            state.waitsForProvider shouldBe false
        }

        test("usersCreateValidatesThenInvitesAndReloads") {
            // given
            val backend = FakeBackend()
            backend.reply("users", bob, HttpMethod.Post)
            backend.reply("users", listOf(ann, bob))
            val scope = stateScope()
            val state = UsersState(backend.api, scope)

            // when
            state.create()

            // then
            state.errors shouldBe mapOf("email" to "Required", "password" to "At least 10 characters")
            backend.sent().shouldBeEmpty()

            // when
            state.email = " bob@example.com "
            state.name = "Bob"
            state.password = "long-enough"
            state.create()
            scope.settle()

            // then
            backend.sent(HttpMethod.Post, "users").single().body shouldContain "\"email\":\"bob@example.com\""
            state.email shouldBe ""
            state.password shouldBe ""
            state.users shouldBe listOf(ann, bob)
        }

        test("usersChangeRole") {
            // given
            val backend = FakeBackend()
            backend.ok("users/2", HttpMethod.Put)
            backend.reply("users", listOf(ann, bob.copy(role = EDITOR)))
            val scope = stateScope()
            val state = UsersState(backend.api, scope)

            // when
            state.changeRole(bob, EDITOR).join()

            // then
            backend.sent(HttpMethod.Put, "users/2").single().body shouldContain "\"role\":\"editor\""
            state.users.last().role shouldBe EDITOR
        }

        test("usersDeleteCanBeUndone") {
            // given
            val backend = FakeBackend()
            backend.reply("users", listOf(ann, bob))
            val scope = stateScope()
            val state = UsersState(backend.api, scope)
            state.load().join()

            // when
            state.delete(bob)

            // then
            state.listed shouldBe listOf(ann)

            // when
            state.snackbar.awaitSnackbar().performAction()
            scope.settle()

            // then
            state.listed shouldBe listOf(ann, bob)
            backend.sent(HttpMethod.Delete, "users/2").shouldBeEmpty()
        }

        test("usersDeleteRunsWhenTheSnackbarCloses") {
            // given
            val backend = FakeBackend()
            backend.reply("users", listOf(ann, bob))
            backend.ok("users/2", HttpMethod.Delete)
            val scope = stateScope()
            val state = UsersState(backend.api, scope)
            state.load().join()

            // when
            state.delete(bob)
            backend.reply("users", listOf(ann))
            state.snackbar.awaitSnackbar().dismiss()
            scope.settle()

            // then
            backend.sent(HttpMethod.Delete, "users/2").size shouldBe 1
            state.listed shouldBe listOf(ann)
        }

        test("accountLoadsUserDashboardsAndSignInKind") {
            // given
            val backend = FakeBackend()
            backend.session.signOut()
            backend.reply("me", ann)
            backend.reply("dashboards", dashboards)
            backend.reply("auth/config", AuthConfig(local = false))
            val state = AccountState(backend.api, backend.session, stateScope())

            // when
            state.load()

            // then
            state.user shouldBe ann
            state.dashboards shouldBe listOf("a", "b")
            state.canChangePassword shouldBe false
        }

        test("accountSavesDraftPrefs") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", ann)
            backend.reply("me/prefs", ann.copy(prefs = Prefs(theme = "dark")), HttpMethod.Put)
            val scope = stateScope()
            val state = AccountState(backend.api, backend.session, scope)

            // when
            state.prefs = Prefs(theme = "dark")

            // then
            backend.session.draftPrefs shouldBe Prefs(theme = "dark")

            // when
            state.savePrefs()
            scope.settle()

            // then
            state.prefsInfo shouldBe "Preferences saved"
            backend.session.draftPrefs.shouldBeNull()
            state.prefs shouldBe Prefs(theme = "dark")
        }

        test("accountShowsPrefsErrorsAndDiscardsDrafts") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", ann)
            backend.fail("me/prefs", HttpStatusCode.BadRequest, "Unknown time zone", HttpMethod.Put)
            val scope = stateScope()
            val state = AccountState(backend.api, backend.session, scope)
            state.prefs = Prefs(timeZone = "Mars/Base")

            // when
            state.savePrefs()
            scope.settle()

            // then
            state.prefsError shouldBe "Unknown time zone"

            // when
            state.discardPrefs()

            // then
            state.prefs shouldBe ann.prefs
        }

        test("accountChangesPassword") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", ann)
            backend.ok("me/password", HttpMethod.Put)
            val scope = stateScope()
            val state = AccountState(backend.api, backend.session, scope)
            state.newPassword = "short"

            // when
            state.changePassword()

            // then
            state.passwordErrors shouldBe
                mapOf("current" to "Required", "new" to "At least 10 characters", "repeated" to "Passwords differ")

            // when
            state.currentPassword = "old-password"
            state.newPassword = "new-password"
            state.repeatedPassword = "new-password"
            state.changePassword()
            scope.settle()

            // then
            state.passwordInfo shouldBe "Password changed; your other sessions were signed out"
            state.currentPassword shouldBe ""
            backend.sent(HttpMethod.Put, "me/password").single().body shouldBe """{"current":"old-password","new":"new-password"}"""
        }

        test("accountPasswordErrorIsShown") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", ann)
            backend.fail("me/password", HttpStatusCode.BadRequest, "Current password is wrong", HttpMethod.Put)
            val scope = stateScope()
            val state = AccountState(backend.api, backend.session, scope)
            state.currentPassword = "bad-password"
            state.newPassword = "new-password"
            state.repeatedPassword = "new-password"

            // when
            state.changePassword()
            scope.settle()

            // then
            state.passwordError shouldBe "Current password is wrong"
        }

        test("accountSignsOut") {
            // given
            val backend = FakeBackend()
            backend.session.signIn("t", ann)
            backend.ok("auth/logout", HttpMethod.Post)
            val state = AccountState(backend.api, backend.session, stateScope())

            // when
            state.signOut().join()

            // then
            backend.session.token.shouldBeNull()
            backend.sent(HttpMethod.Post, "auth/logout").size shouldBe 1
        }
    })
