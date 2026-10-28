package com.anjo.statisticsui

import com.anjo.statisticsui.model.Dashboard
import com.anjo.statisticsui.model.Dataset
import com.anjo.statisticsui.model.Dimension
import com.anjo.statisticsui.model.Measure
import com.anjo.statisticsui.model.Query
import com.anjo.statisticsui.model.Source
import com.anjo.statisticsui.model.Variable
import com.anjo.statisticsui.model.Widget
import com.anjo.statisticsui.validation.dashboardErrors
import com.anjo.statisticsui.validation.datasetErrors
import com.anjo.statisticsui.validation.emailError
import com.anjo.statisticsui.validation.idError
import com.anjo.statisticsui.validation.jsonError
import com.anjo.statisticsui.validation.loginErrors
import com.anjo.statisticsui.validation.newDashboardErrors
import com.anjo.statisticsui.validation.newUserErrors
import com.anjo.statisticsui.validation.passwordChangeErrors
import com.anjo.statisticsui.validation.positiveWholeNumber
import com.anjo.statisticsui.validation.sourceErrors
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class ValidationTest :
    FunSpec({

        test("emailNeedsLocalPartAtAndDomain") {
            emailError("admin@local") shouldBe null
            emailError(" user@example.com ") shouldBe null
            emailError("") shouldBe "Required"
            emailError("user") shouldBe "Not an email address"
            emailError("a b@example.com") shouldBe "Not an email address"
            emailError("a@b@c") shouldBe "Not an email address"
        }

        test("loginRequiresEmailAndPassword") {
            loginErrors("", "") shouldBe mapOf("email" to "Required", "password" to "Required")
            loginErrors("admin@local", "x").shouldBeEmpty()
        }

        test("passwordChangeMirrorsTheBackendMinimum") {
            passwordChangeErrors("old", "0123456789", "0123456789").shouldBeEmpty()
            passwordChangeErrors("", "short", "other") shouldBe
                mapOf("current" to "Required", "new" to "At least 10 characters", "repeated" to "Passwords differ")
        }

        test("newUserChecksEmailRoleAndPassword") {
            newUserErrors("new@example.com", "editor", "0123456789").shouldBeEmpty()
            newUserErrors("new", "owner", "123").keys shouldBe setOf("email", "role", "password")
        }

        test("idsFollowTheBackendPattern") {
            idError("temp-sensors_2") shouldBe null
            idError("_x") shouldBe null
            idError("") shouldBe "Required"
            idError("2x") shouldStartWith "Letters"
            idError("a b") shouldStartWith "Letters"
            idError("a".repeat(63)) shouldBe null
            idError("a".repeat(64)) shouldStartWith "Letters"
            idError("home", existingIds = listOf("home")) shouldBe "Already exists"
            newDashboardErrors(" home ", listOf("home")) shouldBe mapOf("id" to "Already exists")
        }

        test("jsonMustParse") {
            jsonError("""{"a": 1}""") shouldBe null
            jsonError(" ") shouldBe "Required"
            jsonError("{nope") shouldStartWith "Not valid JSON"
        }

        test("positiveWholeNumbers") {
            positiveWholeNumber(" 50 ") shouldBe 50
            positiveWholeNumber("0") shouldBe null
            positiveWholeNumber("-1") shouldBe null
            positiveWholeNumber("1.5") shouldBe null
        }

        test("datasetNamesRetentionAndId") {
            val dataset = Dataset(id = "weather", measures = listOf(Measure("temp")), dimensions = listOf(Dimension("city")))
            datasetErrors(dataset, listOf("feels_like"), "", emptyList()).shouldBeEmpty()
            datasetErrors(dataset, emptyList(), "30", emptyList()).shouldBeEmpty()
            datasetErrors(dataset, emptyList(), "0", emptyList()) shouldContainKey "retention"
            datasetErrors(dataset, listOf("temp"), "", emptyList()) shouldBe mapOf("names" to "'temp' is declared more than once")
            datasetErrors(dataset, listOf("ts"), "", emptyList()) shouldBe mapOf("names" to "'ts' is reserved")
            datasetErrors(dataset.copy(measures = listOf(Measure("bad-name"))), emptyList(), "", emptyList()) shouldContainKey "names"
            datasetErrors(dataset, emptyList(), "", listOf("weather")) shouldBe mapOf("id" to "Already exists")
        }

        test("sourceNeedsDatasetRateAndMapping") {
            val source = Source(id = "station", dataset = "weather")
            sourceErrors(source, "50", "[]", emptyList()).shouldBeEmpty()
            sourceErrors(source.copy(dataset = ""), "0", "[{", emptyList()).keys shouldBe setOf("dataset", "rate", "mapping")
        }

        test("dashboardRefreshVariablesAndWidgets") {
            val widget = Widget(id = "w1", type = "line", query = Query(dataset = "weather"))
            Dashboard(id = "home", refreshSeconds = 5, widgets = listOf(widget)).let { dashboardErrors(it).shouldBeEmpty() }
            Dashboard(id = "home", refreshSeconds = null).let { dashboardErrors(it).shouldBeEmpty() }
            val invalid =
                Dashboard(
                    id = "home",
                    refreshSeconds = 4,
                    variables = listOf(Variable("a", "timeRange"), Variable("b", "timeRange")),
                    widgets = listOf(widget, widget),
                )
            dashboardErrors(invalid).keys shouldBe setOf("refresh", "variables", "widgets")
        }
    })
