package com.anjo.statisticsui

import com.anjo.statisticsui.model.unitDisplay
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class UnitsTest :
    FunSpec({

        /** metric value [m] in [metric] equals imperial value [i] in [imperial], both ways, and round-trips. */
        fun check(
            metric: String,
            m: Double,
            imperial: String,
            i: Double,
            tolerance: Double = 1e-6,
        ) {
            val toImperial = unitDisplay(metric, "imperial")
            val toMetric = unitDisplay(imperial, "metric")
            toImperial.unit shouldBe imperial
            toMetric.unit shouldBe metric
            toImperial.convert(m) shouldBe (i plusOrMinus tolerance)
            toMetric.convert(i) shouldBe (m plusOrMinus tolerance)
            toMetric.convert(toImperial.convert(m)) shouldBe (m plusOrMinus 1e-9)
        }

        test("everyFamilyConvertsBothWays") {
            check("°C", 100.0, "°F", 212.0)
            check("°C", -40.0, "°F", -40.0)
            check("km", 1.609344, "mi", 1.0)
            check("kg", 0.45359237, "lb", 1.0)
            check("km/h", 1.609344, "mph", 1.0)
            check("l", 3.785411784, "gal", 1.0)
            check("hPa", 33.8638866667, "inHg", 1.0)
        }

        test("unknownOrSameSystemUnitsPassThrough") {
            listOf("%" to "imperial", "ppm" to "metric", "°C" to "metric", "°F" to "imperial").forEach { (unit, system) ->
                val d = unitDisplay(unit, system)
                d.unit shouldBe unit
                d.convert(21.5) shouldBe 21.5
            }
            unitDisplay(null, "imperial").unit shouldBe null
        }
    })
